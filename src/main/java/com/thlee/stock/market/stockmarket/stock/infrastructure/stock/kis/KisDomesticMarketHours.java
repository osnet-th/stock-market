package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 국내 주식 시간외 단일가 시간대 판단.
 * KIS API 호출 분기를 위한 인프라 유틸리티.
 */
@Component
public class KisDomesticMarketHours {

    private static final LocalTime OVERTIME_START = LocalTime.of(16, 0);
    private static final LocalTime OVERTIME_END = LocalTime.of(18, 0);

    private final Clock clock;

    public KisDomesticMarketHours() {
        this(Clock.system(ZoneId.of("Asia/Seoul")));
    }

    /**
     * 테스트용 생성자. Clock을 주입하여 시간을 제어할 수 있다.
     */
    KisDomesticMarketHours(Clock clock) {
        this.clock = clock;
    }

    /**
     * 현재 시각이 시간외 단일가 시간대(16:00~18:00)인지 판단한다.
     */
    public boolean isOvertimeHours() {
        LocalTime now = ZonedDateTime.now(clock).toLocalTime();
        return !now.isBefore(OVERTIME_START) && !now.isAfter(OVERTIME_END);
    }
}