package com.thlee.stock.market.stockmarket.chatbot.application;

import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatMessage;
import com.thlee.stock.market.stockmarket.chatbot.application.dto.ChatRequest;
import com.thlee.stock.market.stockmarket.chatbot.application.port.LlmPort;
import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final LlmPort llmPort;
    private final ChatContextBuilder contextBuilder;
    private final DomainEventLogger domainEventLogger;

    public Flux<String> chat(ChatRequest request) {
        String systemPrompt = contextBuilder.build(request);

        List<ChatMessage> allMessages = new ArrayList<>();
        if (request.messages() != null) {
            request.messages().stream()
                    .filter(m -> m.content() != null && !m.content().isBlank())
                    .forEach(allMessages::add);
        }

        String userMessage = resolveUserMessage(request);
        allMessages.add(new ChatMessage("user", userMessage));

        // 로깅 컨텍스트는 caller(WebFlux 요청) 스레드에서 캡처 — Reactor 내부 스레드로의 전환 대비
        Long userId = currentAuthenticatedUserId();
        long startNanos = System.nanoTime();
        AtomicInteger responseLen = new AtomicInteger();
        String llmImpl = llmPort.getClass().getSimpleName();
        String analysisTaskName = request.analysisTask() != null ? request.analysisTask().name() : null;
        int userMessageLen = userMessage.length();

        return llmPort.stream(systemPrompt, allMessages)
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        responseLen.addAndGet(chunk.length());
                    }
                })
                .doOnComplete(() -> publishChatEvent(
                        "CHATBOT_QUERY_COMPLETED", userId, startNanos,
                        userMessageLen, responseLen.get(), llmImpl, analysisTaskName, null))
                .doOnError(e -> publishChatEvent(
                        "CHATBOT_QUERY_FAILED", userId, startNanos,
                        userMessageLen, responseLen.get(), llmImpl, analysisTaskName, e));
    }

    private void publishChatEvent(String eventType, Long userId, long startNanos,
                                  int userMessageLen, int responseLen, String llm,
                                  String analysisTask, Throwable err) {
        try {
            long durationMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("durationMs", durationMs);
            payload.put("userMessageLen", userMessageLen);
            payload.put("responseLen", responseLen);
            payload.put("llm", llm);
            if (analysisTask != null) {
                payload.put("analysisTask", analysisTask);
            }
            if (err != null) {
                payload.put("errorClass", err.getClass().getSimpleName());
            }
            domainEventLogger.logBusiness(eventType, userId, payload);
        } catch (Exception ignored) {
            // 로깅 자체 실패가 챗봇 스트림을 중단시켜서는 안 됨
        }
    }

    private Long currentAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return auth.getPrincipal() instanceof Long id ? id : null;
    }

    private String resolveUserMessage(ChatRequest request) {
        if (request.message() != null && !request.message().isBlank()) {
            return request.message();
        }
        if (request.analysisTask() != null) {
            return request.analysisTask().toUserMessage();
        }
        return "";
    }
}