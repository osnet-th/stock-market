package com.thlee.stock.market.stockmarket.chatbot.application.dto;

import java.util.List;

public record ChatRequest(
        Long userId,
        String message,
        ChatMode chatMode,
        String stockCode,
        String indicatorCategory,
        AnalysisTask analysisTask,
        List<ChatMessage> messages
) {}