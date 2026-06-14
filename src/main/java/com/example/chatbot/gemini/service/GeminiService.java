package com.example.chatbot.gemini.service;

import com.example.chatbot.tools.registry.ToolDefinition;
import com.example.chatbot.tools.registry.ToolRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.endpoint-base:https://generativelanguage.googleapis.com/v1beta}")
    private String endpointBase;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper, ToolRegistry toolRegistry) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
    }

    public String generateResponse(List<Map<String, String>> history, List<String> categories) {
        if (apiKey == null || apiKey.isBlank()) {
            return "죄송합니다. AI 서비스가 설정되지 않았습니다. (GEMINI_API_KEY 환경변수 미설정)";
        }
        try {
            String endpoint = endpointBase + "/models/" + model + ":generateContent";

            List<ToolDefinition> tools = toolRegistry.getToolsForCategories(categories);
            logger.info("[TOOL-PROVIDE] 제공 tool 목록 ({}개): [{}]",
                    tools.size(),
                    tools.stream().map(ToolDefinition::getName).collect(Collectors.joining(", ")));

            String lastContent = history.isEmpty() ? "" : history.get(history.size() - 1).getOrDefault("content", "");
            logger.info("Gemini 요청 ({} 턴): {}", history.size(),
                    lastContent.substring(0, Math.min(100, lastContent.length())));

            ArrayNode contents = buildContents(history);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            // functionCall 루프 (최대 5회)
            for (int iter = 0; iter < 5; iter++) {
                ObjectNode payload = objectMapper.createObjectNode();
                payload.set("contents", contents);
                if (!tools.isEmpty()) {
                    payload.set("tools", buildToolsNode(tools));
                }

                ResponseEntity<String> response = restTemplate.exchange(
                        endpoint, HttpMethod.POST,
                        new HttpEntity<>(objectMapper.writeValueAsString(payload), headers),
                        String.class
                );
                logger.info("HTTP {} (iter={})", response.getStatusCode(), iter);

                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (candidates.isEmpty()) {
                    throw new RuntimeException("Gemini API 응답에서 candidates를 찾을 수 없습니다. body=" + response.getBody());
                }

                JsonNode contentNode = candidates.get(0).path("content");
                JsonNode parts = contentNode.path("parts");

                // functionCall 탐지
                JsonNode functionCallNode = null;
                for (JsonNode part : parts) {
                    if (part.has("functionCall")) {
                        functionCallNode = part.get("functionCall");
                        break;
                    }
                }

                if (functionCallNode != null) {
                    String toolName = functionCallNode.path("name").asText();
                    JsonNode argsNode = functionCallNode.path("args");
                    logger.info("[TOOL-SELECT] 제미나이 선택 tool: '{}', args: {}", toolName, argsNode);

                    Map<String, Object> args = objectMapper.convertValue(argsNode, new TypeReference<>() {});
                    Object toolResult = toolRegistry.dispatch(toolName, args);
                    String toolResultJson = objectMapper.writeValueAsString(toolResult);
                    logger.info("[TOOL-RESULT] '{}' 실행 결과: {}",
                            toolName, toolResultJson.substring(0, Math.min(300, toolResultJson.length())));

                    // model의 functionCall 응답을 contents에 추가
                    contents.add(contentNode.deepCopy());

                    // functionResponse를 contents에 추가
                    ObjectNode funcRespContent = objectMapper.createObjectNode();
                    funcRespContent.put("role", "user");
                    ArrayNode funcRespParts = objectMapper.createArrayNode();
                    ObjectNode funcRespPart = objectMapper.createObjectNode();
                    ObjectNode funcResp = objectMapper.createObjectNode();
                    funcResp.put("name", toolName);
                    funcResp.set("response", objectMapper.valueToTree(toolResult));
                    funcRespPart.set("functionResponse", funcResp);
                    funcRespParts.add(funcRespPart);
                    funcRespContent.set("parts", funcRespParts);
                    contents.add(funcRespContent);

                } else {
                    // 텍스트 응답 - 완료
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : parts) {
                        if (part.has("text")) {
                            sb.append(part.get("text").asText());
                        }
                    }
                    String result = sb.toString().strip();
                    logger.info("답변: {}", result.substring(0, Math.min(200, result.length())));
                    return result;
                }
            }

            throw new RuntimeException("Tool call loop가 최대 반복 횟수(5)를 초과했습니다.");

        } catch (Exception e) {
            logger.error("응답 생성 실패: {}", e.getMessage());
            return "죄송합니다. 현재 서비스를 이용할 수 없습니다. [" + e.getMessage() + "]";
        }
    }

    private ArrayNode buildContents(List<Map<String, String>> messages) {
        ArrayNode contents = objectMapper.createArrayNode();
        for (Map<String, String> msg : messages) {
            String role = msg.getOrDefault("role", "user");
            String content = msg.getOrDefault("content", "");
            String geminiRole = "assistant".equals(role) ? "model" : "user";

            ObjectNode contentNode = objectMapper.createObjectNode();
            contentNode.put("role", geminiRole);
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode part = objectMapper.createObjectNode();
            part.put("text", content);
            parts.add(part);
            contentNode.set("parts", parts);
            contents.add(contentNode);
        }
        return contents;
    }

    private ArrayNode buildToolsNode(List<ToolDefinition> tools) {
        ArrayNode funcDecls = objectMapper.createArrayNode();
        for (ToolDefinition tool : tools) {
            ObjectNode decl = objectMapper.createObjectNode();
            decl.put("name", tool.getName());
            decl.put("description", tool.getDescription());
            decl.set("parameters", objectMapper.valueToTree(tool.getParameters()));
            funcDecls.add(decl);
        }
        ObjectNode toolNode = objectMapper.createObjectNode();
        toolNode.set("functionDeclarations", funcDecls);
        ArrayNode toolsArray = objectMapper.createArrayNode();
        toolsArray.add(toolNode);
        return toolsArray;
    }
}
