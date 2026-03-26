# LlmPort 구현 예시

```java
// chatbot/application/port/LlmPort.java
// Flux<String> 반환으로 Reactor 의존이 불가피하여 domain이 아닌 application/port에 위치
package com.thlee.stock.market.stockmarket.chatbot.application.port;

import reactor.core.publisher.Flux;

public interface LlmPort {
    Flux<String> stream(String systemPrompt, String userMessage);
}
```
