package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import com.thlee.stock.market.stockmarket.chatbot.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmPort llmPort;
    private final ChatContextBuilder contextBuilder;

    public Flux<String> chat(ChatRequest request) {
        String systemPrompt = contextBuilder.build(request);
        return llmPort.stream(systemPrompt, request.message());
    }
}