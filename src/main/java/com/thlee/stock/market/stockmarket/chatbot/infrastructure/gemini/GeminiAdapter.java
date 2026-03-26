package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.chatbot.application.port.LlmPort;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.config.GeminiProperties;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto.GeminiRequest;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto.GeminiStreamChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAdapter implements LlmPort {

    private static final String SSE_DATA_PREFIX = "data: ";

    private final WebClient geminiWebClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<String> stream(String systemPrompt, String userMessage) {
        String path = "/v1beta/models/{model}:streamGenerateContent?key={apiKey}&alt=sse";

        log.info("[Gemini] 요청 시작 - model: {}, userMessage: {}", properties.getModel(), userMessage);

        return geminiWebClient.post()
                .uri(path, properties.getModel(), properties.getApiKey())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(GeminiRequest.of(systemPrompt, userMessage))
                .retrieve()
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
                .doOnSubscribe(s -> log.info("[Gemini] Flux 구독 시작"))
                .map(dataBuffer -> {
                    String content = dataBuffer.toString(StandardCharsets.UTF_8);
                    org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                    log.debug("[Gemini] raw chunk 수신: {}", content.substring(0, Math.min(content.length(), 200)));
                    return content;
                })
                .doOnNext(chunk -> log.info("[Gemini] DataBuffer 수신 - length: {}", chunk.length()))
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n")))
                .filter(line -> line.startsWith(SSE_DATA_PREFIX))
                .map(line -> line.substring(SSE_DATA_PREFIX.length()))
                .filter(json -> !json.isBlank())
                .doOnNext(json -> log.info("[Gemini] SSE data 파싱됨 - length: {}", json.length()))
                .mapNotNull(json -> {
                    try {
                        GeminiStreamChunk chunk = objectMapper.readValue(json, GeminiStreamChunk.class);
                        log.info("[Gemini] GeminiStreamChunk 역직렬화 성공");
                        return chunk;
                    } catch (Exception e) {
                        log.error("[Gemini] JSON 파싱 실패: {}", e.getMessage());
                        return null;
                    }
                })
                .map(GeminiStreamChunk::extractText)
                .doOnNext(text -> log.info("[Gemini] 추출된 텍스트: {}", text.substring(0, Math.min(text.length(), 100))))
                .filter(text -> !text.isBlank())
                .doOnComplete(() -> log.info("[Gemini] 스트리밍 완료"))
                .doOnError(e -> log.error("[Gemini] 스트리밍 에러: {}", e.getMessage(), e))
                .onErrorResume(e ->
                        Flux.just("오류가 발생했습니다: " + e.getMessage())
                );
    }
}