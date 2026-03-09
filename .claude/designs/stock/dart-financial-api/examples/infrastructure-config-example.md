# DartProperties / DartApiException 구현 예시

## DartProperties

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "dart.api")
public class DartProperties {

    private String baseUrl;
    private String crtfcKey;
}
```

## DartApiException

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.exception;

public class DartApiException extends RuntimeException {

    public DartApiException(String message) {
        super(message);
    }

    public DartApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
```
