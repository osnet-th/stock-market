package com.thlee.stock.market.stockmarket.chatbot.application.port;

import reactor.core.publisher.Flux;

public interface LlmPort {
    Flux<String> stream(String systemPrompt, String userMessage);
}