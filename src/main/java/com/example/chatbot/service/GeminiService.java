package com.example.chatbot.service;

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

@Service
public class GeminiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiService.class);

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.endpoint-base:https://generativelanguage.googleapis.com/v1}")
    private String endpointBase;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String generateResponse(List<Map<String, String>> history, List<String> categories) {
        if (apiKey == null || apiKey.isBlank()) {
            return "죄송합니다. AI 서비스가 설정되지 않았습니다. (GEMINI_API_KEY 환경변수 미설정)";
        }
        try {
            String endpoint = endpointBase + "/models/" + model + ":generateContent";
            ObjectNode payload = buildPayload(history);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            String lastContent = history.isEmpty() ? "" : history.get(history.size() - 1).getOrDefault("content", "");
            logger.info("Gemini 요청 ({} 턴): {}", history.size(),
                    lastContent.substring(0, Math.min(100, lastContent.length())));

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST,
                    new HttpEntity<>(objectMapper.writeValueAsString(payload), headers),
                    String.class
            );

            logger.info("HTTP {}", response.getStatusCode());
            String result = parseResponse(response.getBody());
            logger.info("답변: {}", result.substring(0, Math.min(200, result.length())));
            return result;
        } catch (Exception e) {
            logger.error("응답 생성 실패: {}", e.getMessage());
            return "죄송합니다. 현재 서비스를 이용할 수 없습니다. [" + e.getMessage() + "]";
        }
    }

    private ObjectNode buildPayload(List<Map<String, String>> messages) {
        ObjectNode payload = objectMapper.createObjectNode();
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

        payload.set("contents", contents);
        return payload;
    }

    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            throw new RuntimeException("Gemini API 응답에서 candidates를 찾을 수 없습니다.");
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                sb.append(part.get("text").asText());
            }
        }
        return sb.toString().strip();
    }
}
