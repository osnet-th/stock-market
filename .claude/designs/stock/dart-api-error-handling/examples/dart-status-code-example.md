# DartStatusCode 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DartStatusCode {

    SUCCESS("000", "정상"),
    UNREGISTERED_KEY("010", "등록되지 않은 인증키"),
    DISABLED_KEY("011", "사용할 수 없는 인증키"),
    NO_DATA("013", "조회된 데이타가 없습니다"),
    RATE_LIMIT_EXCEEDED("020", "요청 제한 초과"),
    INVALID_FIELD("100", "필드의 부적절한 값"),
    SYSTEM_MAINTENANCE("800", "원활한 공시서비스를 위하여 요청을 , , 제한하였습니다"),
    UNKNOWN_ERROR("900", "정의되지 않은 오류가 발생하였습니다");

    private final String code;
    private final String description;

    public static DartStatusCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElse(UNKNOWN_ERROR);
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isNoData() {
        return this == NO_DATA;
    }
}
```