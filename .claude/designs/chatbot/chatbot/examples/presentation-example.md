# Presentation 예시

```java
// chatbot/presentation/ChatController.java
package com.thlee.stock.market.stockmarket.chatbot.presentation;

import com.thlee.stock.market.stockmarket.chatbot.application.ChatService;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/chat
     * Body: { "message": "내 포트폴리오 분석해줘" }
     * Response: SSE 스트리밍 텍스트
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(
            @AuthenticationPrincipal Long userId,
            @RequestBody ChatMessageRequest request
    ) {
        return chatService.chat(new ChatRequest(userId, request.message()));
    }

    public record ChatMessageRequest(String message) {}
}
```

```
# Security Config 추가 (DevSecurityConfig / ProdSecurityConfig)
.requestMatchers("/api/chat/**").authenticated()
```
