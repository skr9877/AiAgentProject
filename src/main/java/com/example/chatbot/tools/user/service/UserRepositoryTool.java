package com.example.chatbot.tools.user.service;

import com.example.chatbot.tools.registry.ToolDefinition;
import com.example.chatbot.tools.user.model.User;
import com.example.chatbot.tools.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class UserRepositoryTool {

    private final UserRepository userRepository;

    public UserRepositoryTool(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUser(int userId) {
        return userRepository.findById(userId);
    }

    public List<User> searchUsers(String name) {
        return userRepository.findByName(name);
    }

    public static final List<ToolDefinition> TOOL_SPECS = List.of(
            new ToolDefinition(
                    "get_user",
                    "유저 ID로 사용자 정보를 조회합니다.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "user_id", Map.of("type", "integer", "description", "유저 ID")
                            ),
                            "required", List.of("user_id")
                    )
            ),
            new ToolDefinition(
                    "search_users",
                    "이름으로 사용자를 검색합니다.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "name", Map.of("type", "string", "description", "검색할 이름 (부분 일치)")
                            ),
                            "required", List.of("name")
                    )
            )
    );
}
