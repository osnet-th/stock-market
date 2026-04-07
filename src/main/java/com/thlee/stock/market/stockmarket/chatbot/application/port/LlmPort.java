package com.thlee.stock.market.stockmarket.chatbot.application.port;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMessage;
import reactor.core.publisher.Flux;

import java.util.List;

public interface LlmPort {
    Flux<String> stream(String systemPrompt, List<ChatMessage> messages);
}