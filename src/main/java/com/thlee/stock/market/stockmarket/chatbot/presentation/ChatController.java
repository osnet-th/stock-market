package com.thlee.stock.market.stockmarket.chatbot.presentation;

import com.thlee.stock.market.stockmarket.chatbot.application.ChatService;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMessage;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMode;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam Long userId,
            @RequestBody ChatMessageRequest request
    ) {
        return chatService.chat(new ChatRequest(
                userId,
                request.message(),
                request.chatMode(),
                request.stockCode(),
                request.indicatorCategory(),
                request.messages()
        ));
    }

    public record ChatMessageRequest(
            String message,
            ChatMode chatMode,
            String stockCode,
            String indicatorCategory,
            List<ChatMessage> messages
    ) {}
}