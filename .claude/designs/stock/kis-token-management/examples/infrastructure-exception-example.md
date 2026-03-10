# KisApiException 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception;

public class KisApiException extends RuntimeException {

    public KisApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public KisApiException(String message) {
        super(message);
    }
}
```