package com.thlee.stock.market.stockmarket.chatbot.application.dto;

/**
 * 멀티턴 대화의 개별 메시지
 * role: "user" 또는 "model" (Gemini API 규격)
 */
public record ChatMessage(
        String role,
        String content
) {}