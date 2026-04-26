package com.thlee.stock.market.stockmarket.stocknote.infrastructure.scheduler;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * 간이 한국 영업일 계산기.
 *
 * <p>주말(토/일) + 한국 양력 고정 공휴일 + 음력 기반/대체 공휴일(연도별 정적 매핑)을 스킵한다.
 * 음력/대체 공휴일은 매년 발표 후 갱신 필요. 임시공휴일/선거일 등 미등록 휴장일은 KIS 응답
 * 감지(별건) 로 보호.
 */
@Component
public class BusinessDayCalculator {

    /** 양력 고정 공휴일. */
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

    /** 음력/대체 공휴일 정적 매핑. 매년 12월에 다음 해 데이터 PR 로 갱신 필요. */
    private static final Set<LocalDate> LUNAR_AND_SUBSTITUTE_HOLIDAYS = Set.of(
            // 2026
            LocalDate.of(2026, 2, 16),   // 설날 연휴
            LocalDate.of(2026, 2, 17),   // 설날
            LocalDate.of(2026, 2, 18),   // 설날 연휴
            LocalDate.of(2026, 5, 25),   // 부처님오신날 대체 (5/24 일요일)
            LocalDate.of(2026, 8, 17),   // 광복절 대체 (8/15 토요일)
            LocalDate.of(2026, 9, 24),   // 추석 연휴
            LocalDate.of(2026, 9, 25),   // 추석
            // 2027
            LocalDate.of(2027, 2, 8),    // 설날 (2/6 토 ~ 2/7 일 이후 월요일)
            LocalDate.of(2027, 5, 13),   // 부처님오신날
            LocalDate.of(2027, 9, 14),   // 추석 연휴
            LocalDate.of(2027, 9, 15),   // 추석
            LocalDate.of(2027, 9, 16)    // 추석 연휴
    );

    public boolean isBusinessDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date 는 필수입니다.");
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        if (FIXED_HOLIDAYS.contains(MonthDay.from(date))) {
            return false;
        }
        return !LUNAR_AND_SUBSTITUTE_HOLIDAYS.contains(date);
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