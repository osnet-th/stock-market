# KisProperties 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kis.api")
public class KisProperties {
    private String baseUrl;
    private String appKey;
    private String appSecret;
}
```