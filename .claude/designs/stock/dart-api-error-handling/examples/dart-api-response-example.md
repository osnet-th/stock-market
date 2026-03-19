# DartApiResponse 변경 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor
public class DartApiResponse<T> {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("list")
    private List<T> list;

    public boolean isSuccess() {
        return DartStatusCode.fromCode(status).isSuccess();
    }

    public boolean isNoData() {
        return DartStatusCode.fromCode(status).isNoData();
    }

    public DartStatusCode getStatusCode() {
        return DartStatusCode.fromCode(status);
    }

    /**
     * 데이터 없음(013) 응답을 빈 리스트 응답으로 변환
     */
    public static <T> DartApiResponse<T> empty() {
        DartApiResponse<T> response = new DartApiResponse<>();
        response.status = DartStatusCode.SUCCESS.getCode();
        response.message = "정상";
        response.list = Collections.emptyList();
        return response;
    }
}
```

## 변경 포인트

- `isNoData()` 추가: status `"013"` 판별
- `getStatusCode()` 추가: enum 변환
- `isSuccess()`: 하드코딩 `"000"` 제거 → enum 활용
- `empty()` 정적 팩토리: 013 응답을 빈 리스트 응답으로 대체할 때 사용
- `status`, `list` 필드에 직접 접근하기 위해 package-private 또는 setter 없이 static 팩토리에서 설정