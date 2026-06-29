package com.example.chatbot.tools.user.service;

import com.example.chatbot.tools.user.entity.User;
import com.example.chatbot.tools.user.mapper.UserMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserRepositoryTool {

    private final UserMapper userMapper;

    public UserRepositoryTool(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Tool(description = "유저 ID로 사용자 정보를 조회합니다.")
    public User getUser(@ToolParam(description = "유저 ID") int userId) {
        return userMapper.findById(userId);
    }

    @Tool(description = "이름으로 사용자를 검색합니다.")
    public List<User> searchUsers(@ToolParam(description = "검색할 이름 (부분 일치)") String name) {
        return userMapper.findByName(name);
    }
}
