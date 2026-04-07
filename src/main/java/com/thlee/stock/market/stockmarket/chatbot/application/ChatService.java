package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMessage;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import com.thlee.stock.market.stockmarket.chatbot.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmPort llmPort;
    private final ChatContextBuilder contextBuilder;

    public Flux<String> chat(ChatRequest request) {
        String systemPrompt = contextBuilder.build(request);

        List<ChatMessage> allMessages = new ArrayList<>();
        if (request.messages() != null) {
            request.messages().stream()
                    .filter(m -> m.content() != null && !m.content().isBlank())
                    .forEach(allMessages::add);
        }
        allMessages.add(new ChatMessage("user", request.message()));

        return llmPort.stream(systemPrompt, allMessages);
    }
}