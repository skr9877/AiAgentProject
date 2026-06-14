package com.example.chatbot.chatting.handler;

import com.example.chatbot.chatting.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final ChatService chatService;

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session);
        String userId = extractUserId(session);
        chatService.connect(session, sessionId, userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        String userId = extractUserId(session);
        String payload = message.getPayload();

        String userMessage;
        List<String> categories = new ArrayList<>();

        try {
            JsonNode json = objectMapper.readTree(payload);
            userMessage = json.path("text").asText("").strip();
            JsonNode cats = json.path("categories");
            if (cats.isArray()) {
                for (JsonNode cat : cats) {
                    categories.add(cat.asText());
                }
            }
        } catch (Exception e) {
            userMessage = payload.strip();
        }

        if (userMessage.isBlank()) return;

        logger.info("[{}] 사용자 입력: {}", userId, userMessage);

        chatService.sendMessage(sessionId, "고객: " + userMessage);
        String aiReply = chatService.getAiResponse(sessionId, userMessage, categories);
        chatService.sendMessage(sessionId, "AI: " + aiReply);

        logger.info("[{}] AI 최종 답변: {}", userId, aiReply);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        chatService.disconnect(sessionId);
    }

    private String extractSessionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String extractUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("userId=")) {
                    return param.substring("userId=".length());
                }
            }
        }
        return "unknown";
    }
}
