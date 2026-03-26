# Infrastructure Config 예시

```java
// chatbot/infrastructure/gemini/config/GeminiProperties.java
package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String model,      // 기본값: gemini-1.5-flash
        String baseUrl     // 기본값: https://generativelanguage.googleapis.com
) {
    public GeminiProperties {
        if (model == null) model = "gemini-1.5-flash";
        if (baseUrl == null) baseUrl = "https://generativelanguage.googleapis.com";
    }
}
```

```java
// chatbot/infrastructure/gemini/config/GeminiConfig.java
package com.thlee.stock.market.stockmarket.chatbot.infrastructure.gemini.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    @Bean
    public WebClient geminiWebClient(GeminiProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
```

```properties
# application-global.properties 추가
gemini.api-key=${GEMINI_API_KEY}
gemini.model=gemini-1.5-flash
gemini.base-url=https://generativelanguage.googleapis.com
```
