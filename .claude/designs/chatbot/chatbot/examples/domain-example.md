# Domain 예시

```java
// chatbot/domain/service/LlmPort.java
package com.thlee.stock.market.stockmarket.chatbot.domain.service;

import reactor.core.publisher.Flux;

public interface LlmPort {
    Flux<String> stream(String systemPrompt, String userMessage);
}
```
