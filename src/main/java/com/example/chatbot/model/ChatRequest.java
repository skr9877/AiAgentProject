package com.example.chatbot.model;

import java.util.List;

public class ChatRequest {
    private String text;
    private List<String> categories;

    public ChatRequest() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
