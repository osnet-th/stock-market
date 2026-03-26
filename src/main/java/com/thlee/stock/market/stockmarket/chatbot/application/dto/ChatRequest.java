package com.thlee.stock.market.stockmarket.chatbot.application.dto;

public record ChatRequest(
        Long userId,
        String message,
        ChatMode chatMode,
        String stockCode
) {}