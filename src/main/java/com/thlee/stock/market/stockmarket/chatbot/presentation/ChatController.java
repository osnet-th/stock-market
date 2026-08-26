package com.thlee.stock.market.stockmarket.chatbot.presentation;

import com.thlee.stock.market.stockmarket.chatbot.application.ChatService;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.AnalysisTask;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMessage;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMode;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal Long jwtUserId,
            @RequestParam Long userId,
            @RequestBody ChatMessageRequest request
    ) {
        assertUserMatches(jwtUserId, userId);
        return chatService.chat(new ChatRequest(
                userId,
                request.message(),
                request.chatMode(),
                request.stockCode(),
                request.portfolioItemId(),
                request.indicatorCategory(),
                request.analysisTask(),
                request.messages()
        ));
    }

    private void assertUserMatches(Long jwtUserId, Long requestedUserId) {
        if (jwtUserId != null && !jwtUserId.equals(requestedUserId)) {
            throw new AccessDeniedException("요청한 사용자 정보가 인증된 사용자와 일치하지 않습니다.");
        }
    }

    public record ChatMessageRequest(
            String message,
            ChatMode chatMode,
            String stockCode,
            Long portfolioItemId,
            String indicatorCategory,
            AnalysisTask analysisTask,
            List<ChatMessage> messages
    ) {}
}
