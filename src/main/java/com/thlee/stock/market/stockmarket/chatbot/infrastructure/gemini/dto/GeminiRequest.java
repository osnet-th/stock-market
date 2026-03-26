package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    public static GeminiRequest of(String systemPrompt, String userMessage) {
        return new GeminiRequest(
                new SystemInstruction(List.of(new Part(systemPrompt))),
                List.of(new Content("user", List.of(new Part(userMessage)))),
                new GenerationConfig(0.7)
        );
    }
}