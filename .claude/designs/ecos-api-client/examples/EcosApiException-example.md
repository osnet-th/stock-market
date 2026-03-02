# EcosApiException 코드 예시

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.exception;

public class EcosApiException extends RuntimeException {
    public EcosApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
```