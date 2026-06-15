package com.example.chatbot.config;

import com.google.genai.Client;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AiConfig {

    private final String apiKey;
    private final String model;

    public AiConfig(
            @Value("${spring.ai.google.genai.api-key:}") String apiKey,
            @Value("${spring.ai.google.genai.chat.model:gemini-2.5-flash}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Bean
    @ConditionalOnMissingBean
    public Client googleGenAiClient() {
        return Client.builder().apiKey(apiKey).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public GoogleGenAiChatModel googleGenAiChatModel(Client googleGenAiClient) {
        return GoogleGenAiChatModel.builder()
                .genAiClient(googleGenAiClient)
                .defaultOptions(GoogleGenAiChatOptions.builder().model(model).build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
