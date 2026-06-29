package com.example.chatbot.chatting.service;

import com.example.chatbot.chatting.service.ResponseFilterService;
import com.example.chatbot.tools.order.service.OrderApiTool;
import com.example.chatbot.tools.order.service.OrderRepositoryTool;
import com.example.chatbot.tools.restaurant.service.RestaurantRepositoryTool;
import com.example.chatbot.tools.user.service.UserRepositoryTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    private final ChatClient chatClient;
    private final OrderRepositoryTool orderRepositoryTool;
    private final OrderApiTool orderApiTool;
    private final UserRepositoryTool userRepositoryTool;
    private final RestaurantRepositoryTool restaurantRepositoryTool;
    private final ResponseFilterService responseFilterService;

    public ChatService(ChatClient chatClient,
                       OrderRepositoryTool orderRepositoryTool,
                       OrderApiTool orderApiTool,
                       UserRepositoryTool userRepositoryTool,
                       RestaurantRepositoryTool restaurantRepositoryTool,
                       ResponseFilterService responseFilterService) {
        this.chatClient = chatClient;
        this.orderRepositoryTool = orderRepositoryTool;
        this.orderApiTool = orderApiTool;
        this.userRepositoryTool = userRepositoryTool;
        this.restaurantRepositoryTool = restaurantRepositoryTool;
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
        List<Message> messages = history.subList(fromIndex, history.size()).stream()
                                        .<Message>map(turn -> "assistant".equals(turn.get("role"))
                                                ? new AssistantMessage(turn.get("content"))
                                                : new UserMessage(turn.get("content")))
                                        .collect(Collectors.toList());

        try {
            String raw = chatClient.prompt(new Prompt(messages))
                    .tools(selectTools(categories))
                    .call()
                    .content();

            String filtered = responseFilterService.sanitizeImageTags(raw);

            Map<String, String> assistantTurn = new HashMap<>();
            assistantTurn.put("role", "assistant");
            assistantTurn.put("content", filtered);
            history.add(assistantTurn);

            logger.info("[{}] AI 응답 완료 (히스토리 {}턴)", userId, history.size() / 2);
            return filtered;
        } catch (Exception e) {
            logger.error("AI 응답 실패: {}", e.getMessage());
            return "죄송합니다. 현재 서비스를 이용할 수 없습니다. [" + e.getMessage() + "]";
        }
    }

    private Object[] selectTools(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return new Object[0];
        }
        Set<Object> tools = new LinkedHashSet<>();
        for (String cat : categories) {
            switch (cat) {
                case "주문조회" -> { tools.add(orderRepositoryTool); tools.add(orderApiTool); }
                case "유저조회" -> tools.add(userRepositoryTool);
                case "식당조회" -> tools.add(restaurantRepositoryTool);
            }
        }
        return tools.toArray();
    }
}
