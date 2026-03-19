# DartApiException 변경 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.exception;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartStatusCode;
import lombok.Getter;

@Getter
public class DartApiException extends RuntimeException {

    private final DartStatusCode statusCode;

    // 기존 생성자 유지 (하위 호환 — DartCorpCodeCache 등에서 사용)
    public DartApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    public DartApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    // 새 생성자: 상태 코드 포함
    public DartApiException(DartStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
```

## 변경 포인트

- `statusCode` 필드 추가 (nullable — 기존 생성자에서는 null)
- 기존 생성자 2개 그대로 유지 → `DartCorpCodeCache`, `parseCorpCodeZip()` 등에서 수정 없이 사용 가능
- 새 생성자 `(DartStatusCode, String)` 추가 → `callApi()`에서 사용