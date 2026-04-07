package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMessage;

import java.util.List;

public record GeminiRequest(
        @JsonProperty("system_instruction") SystemInstruction systemInstruction,
        List<Content> contents,
        @JsonProperty("generationConfig") GenerationConfig generationConfig
) {
    public record SystemInstruction(List<Part> parts) {}

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(double temperature) {}

    public static GeminiRequest of(String systemPrompt, List<ChatMessage> messages) {
        List<Content> contents = messages.stream()
                .filter(m -> m.content() != null && !m.content().isBlank())
                .map(m -> new Content(m.role(), List.of(new Part(m.content()))))
                .toList();

        return new GeminiRequest(
                new SystemInstruction(List.of(new Part(systemPrompt))),
                contents,
                new GenerationConfig(0.7)
        );
    }
}