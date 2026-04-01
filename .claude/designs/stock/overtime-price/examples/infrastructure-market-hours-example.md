# KisDomesticMarketHours 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * 국내 주식 시간외 단일가 시간대 판단.
 * KIS API 호출 분기를 위한 인프라 유틸리티.
 */
@Component
@RequiredArgsConstructor
public class KisDomesticMarketHours {

    private static final LocalTime OVERTIME_START = LocalTime.of(16, 0);
    private static final LocalTime OVERTIME_END = LocalTime.of(18, 0);

    private final Clock clock;

    /**
     * 현재 시각이 시간외 단일가 시간대(16:00~18:00)인지 판단한다.
     */
    public boolean isOvertimeHours() {
        LocalTime now = ZonedDateTime.now(clock).toLocalTime();
        return !now.isBefore(OVERTIME_START) && !now.isAfter(OVERTIME_END);
    }
}
```

## Clock 빈 등록 (KIS 설정 클래스에 추가)

```java
// KisConfig 또는 기존 KIS 관련 설정 클래스에 추가
@Bean
public Clock koreaMarketClock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
}
```