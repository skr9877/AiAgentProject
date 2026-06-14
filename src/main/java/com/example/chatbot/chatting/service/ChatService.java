package com.example.chatbot.chatting.service;

import com.example.chatbot.gemini.service.GeminiService;
import com.example.chatbot.gemini.service.ResponseFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Value("${chat.max-connections:100}")
    private int maxConnections;

    @Value("${chat.max-history-turns:10}")
    private int maxHistoryTurns;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<Map<String, String>>> histories = new ConcurrentHashMap<>();
    private final Map<String, String> userIds = new ConcurrentHashMap<>();

    private final GeminiService geminiService;
    private final ResponseFilterService responseFilterService;

    public ChatService(GeminiService geminiService, ResponseFilterService responseFilterService) {
        this.geminiService = geminiService;
        this.responseFilterService = responseFilterService;
    }

    public boolean connect(WebSocketSession session, String sessionId, String userId) throws Exception {
        if (sessions.size() >= maxConnections) {
            session.sendMessage(new TextMessage("SYSTEM:서버가 혼잡합니다. 잠시 후 다시 시도해주세요."));
            session.close();
            return false;
        }
        sessions.put(sessionId, session);
        histories.put(sessionId, new ArrayList<>());
        userIds.put(sessionId, userId);
        logger.info("[{}] 세션 연결 (현재 {}개)", userId, sessions.size());
        return true;
    }

    public void disconnect(String sessionId) {
        String userId = userIds.getOrDefault(sessionId, "unknown");
        sessions.remove(sessionId);
        histories.remove(sessionId);
        userIds.remove(sessionId);
        logger.info("[{}] 세션 해제 (현재 {}개)", userId, sessions.size());
    }

    public void sendMessage(String sessionId, String message) throws Exception {
        WebSocketSession ws = sessions.get(sessionId);
        if (ws != null && ws.isOpen()) {
            ws.sendMessage(new TextMessage(message));
        }
    }

    public String getAiResponse(String sessionId, String userMessage, List<String> categories) {
        String userId = userIds.getOrDefault(sessionId, "unknown");
        List<Map<String, String>> history = histories.computeIfAbsent(sessionId, k -> new ArrayList<>());

        Map<String, String> userTurn = new HashMap<>();
        userTurn.put("role", "user");
        userTurn.put("content", userMessage);
        history.add(userTurn);

        int fromIndex = Math.max(0, history.size() - maxHistoryTurns);
        List<Map<String, String>> recentHistory = new ArrayList<>(history.subList(fromIndex, history.size()));

        String raw = geminiService.generateResponse(recentHistory, categories);
        String filtered = responseFilterService.sanitizeImageTags(raw);

        Map<String, String> assistantTurn = new HashMap<>();
        assistantTurn.put("role", "assistant");
        assistantTurn.put("content", filtered);
        history.add(assistantTurn);

        logger.info("[{}] AI 응답 완료 (히스토리 {}턴)", userId, history.size() / 2);
        return filtered;
    }
}
