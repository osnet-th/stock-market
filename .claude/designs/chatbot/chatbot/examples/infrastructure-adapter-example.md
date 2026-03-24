# Infrastructure Adapter 예시

```java
// chatbot/infrastructure/gemini/GeminiAdapter.java
package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini;

import com.thlee.stock.market.stockmarket.chatbot.domain.service.LlmPort;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.config.GeminiProperties;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto.GeminiRequest;
import com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto.GeminiStreamChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class GeminiAdapter implements LlmPort {

    private final WebClient geminiWebClient;
    private final GeminiProperties properties;

    @Override
    public Flux<String> stream(String systemPrompt, String userMessage) {
        String path = "/v1beta/models/{model}:streamGenerateContent?key={apiKey}&alt=sse";

        return geminiWebClient.post()
                .uri(path, properties.model(), properties.apiKey())
                .bodyValue(GeminiRequest.of(systemPrompt, userMessage))
                .retrieve()
                .bodyToFlux(GeminiStreamChunk.class)
                .map(GeminiStreamChunk::extractText)
                .filter(text -> !text.isBlank());
    }
}
```
