package com.thlee.stock.market.stockmarket.stocknote.infrastructure.scheduler;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * 간이 한국 영업일 계산기.
 *
 * <p>주말(토/일) + 한국 고정 공휴일(양력)을 스킵한다. 음력 기반 공휴일(설/추석/부처님오신날)은
 * 커버 범위 밖이며, 이 경우 스냅샷 배치에서 KIS 조회 실패(FAILED) 로 자연 처리된다
 * (설계 심화 결정 참고).
 */
@Component
public class BusinessDayCalculator {

    /** 한국 양력 고정 공휴일. 음력(설/추석/부처님오신날)은 간이 처리 범위 밖. */
    private static final Set<MonthDay> FIXED_HOLIDAYS = Set.of(
            MonthDay.of(1, 1),    // 신정
            MonthDay.of(3, 1),    // 삼일절
            MonthDay.of(5, 5),    // 어린이날
            MonthDay.of(6, 6),    // 현충일
            MonthDay.of(8, 15),   // 광복절
            MonthDay.of(10, 3),   // 개천절
            MonthDay.of(10, 9),   // 한글날
            MonthDay.of(12, 25)   // 크리스마스
    );

    public boolean isBusinessDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date 는 필수입니다.");
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !FIXED_HOLIDAYS.contains(MonthDay.from(date));
    }

    /** date 가 영업일이면 그대로, 아니면 다음 영업일. */
    public LocalDate nextBusinessDay(LocalDate date) {
        LocalDate d = date;
        while (!isBusinessDay(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    /**
     * base 로부터 영업일 기준으로 n 영업일 뒤 날짜를 반환한다.
     * {@code base} 자체는 계산에 포함되지 않으며 영업일 여부와 무관하다.
     *
     * @param n 1 이상
     */
    public LocalDate addBusinessDays(LocalDate base, int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n 은 1 이상이어야 합니다.");
        }
        LocalDate d = base;
        int remaining = n;
        while (remaining > 0) {
            d = d.plusDays(1);
            if (isBusinessDay(d)) {
                remaining--;
            }
        }
        return d;
    }
}