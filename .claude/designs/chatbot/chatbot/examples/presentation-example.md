# Presentation 예시

```java
// chatbot/presentation/ChatController.java
package com.thlee.stock.market.stockmarket.chatbot.presentation;

import com.thlee.stock.market.stockmarket.chatbot.application.ChatService;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMode;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/chat?userId=1
     * Body (포트폴리오 분석): { "message": "내 포트폴리오 분석해줘", "chatMode": "PORTFOLIO" }
     * Body (재무 분석):      { "message": "이 종목 주가 대비 실적 어때?", "chatMode": "FINANCIAL", "stockCode": "005930" }
     * Response: SSE 스트리밍 텍스트
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @RequestParam Long userId,
            @RequestBody ChatMessageRequest request
    ) {
        return chatService.chat(new ChatRequest(userId, request.message(), request.chatMode(), request.stockCode()));
    }

    public record ChatMessageRequest(String message, ChatMode chatMode, String stockCode) {}
}
```