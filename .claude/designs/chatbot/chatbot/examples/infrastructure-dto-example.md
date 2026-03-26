# Infrastructure DTO 예시

```java
// chatbot/infrastructure/gemini/dto/GeminiRequest.java
package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GeminiRequest(
        @JsonProperty("system_instruction") SystemInstruction systemInstruction,
        List<Content> contents,
        @JsonProperty("generationConfig") GenerationConfig generationConfig
) {
    public record SystemInstruction(List<Part> parts) {}

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(double temperature) {}

    public static GeminiRequest of(String systemPrompt, String userMessage) {
        return new GeminiRequest(
                new SystemInstruction(List.of(new Part(systemPrompt))),
                List.of(new Content("user", List.of(new Part(userMessage)))),
                new GenerationConfig(0.7)
        );
    }
}
```

```java
// chatbot/infrastructure/gemini/dto/GeminiStreamChunk.java
package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.dto;

import java.util.List;

public record GeminiStreamChunk(List<Candidate> candidates) {

    public record Candidate(Content content) {}

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    /**
     * 스트리밍 청크에서 텍스트 추출
     */
    public String extractText() {
        if (candidates == null || candidates.isEmpty()) return "";
        Content content = candidates.get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) return "";
        String text = content.parts().get(0).text();
        return text != null ? text : "";
    }
}
```
