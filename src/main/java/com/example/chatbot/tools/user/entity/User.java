package com.example.chatbot.tools.user.entity;

import lombok.Data;

@Data
public class User {
    private int userId;
    private String name;
    private String email;
    private String phone;
    private String address;
}
