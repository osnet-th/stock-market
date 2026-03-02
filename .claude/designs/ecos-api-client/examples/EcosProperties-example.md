# EcosProperties 코드 예시

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "economics.api.korea.ecos")
public class EcosProperties {
    private String baseUrl;
    private String apiKey;
    private int startCount = 1;
    private int endCount = 200;
}
```