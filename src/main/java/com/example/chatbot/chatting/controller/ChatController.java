package com.example.chatbot.chatting.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class ChatController {

    @GetMapping("/chat")
    public String chatPageGet(Model model) {
        model.addAttribute("sessionId", UUID.randomUUID().toString());
        model.addAttribute("userId", "user");
        return "chat";
    }

    @PostMapping("/chat")
    public String chatPagePost(@RequestParam String userId, Model model) {
        model.addAttribute("sessionId", UUID.randomUUID().toString());
        model.addAttribute("userId", userId);
        return "chat";
    }
}
