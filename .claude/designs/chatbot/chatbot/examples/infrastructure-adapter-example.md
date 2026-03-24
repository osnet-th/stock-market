# Infrastructure Adapter 예시

```java
// chatbot/infrastructure/gemini/GeminiAdapter.java
package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.chatbot.application.port.LlmPort;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.config.GeminiProperties;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto.GeminiRequest;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto.GeminiStreamChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class GeminiAdapter implements LlmPort {

    private final WebClient geminiWebClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public Flux<String> stream(String systemPrompt, String userMessage) {
        String path = "/v1beta/models/{model}:streamGenerateContent?key={apiKey}&alt=sse";

        return geminiWebClient.post()
                .uri(path, properties.model(), properties.apiKey())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(GeminiRequest.of(systemPrompt, userMessage))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .mapNotNull(ServerSentEvent::data)
                .filter(data -> !data.isBlank())
                .mapNotNull(json -> {
                    try {
                        return objectMapper.readValue(json, GeminiStreamChunk.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .map(GeminiStreamChunk::extractText)
                .filter(text -> !text.isBlank());
    }
}
```
