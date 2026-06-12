package com.example.chatbot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.UUID;

@Controller
public class ChatController {

    @GetMapping("/chat")
    public String chatPage(Model model) {
        model.addAttribute("sessionId", UUID.randomUUID().toString());
        return "chat";
    }
}
