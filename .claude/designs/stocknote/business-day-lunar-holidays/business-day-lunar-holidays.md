# [stocknote] BusinessDayCalculator 음력 공휴일 정적 매핑 (옵션 A)

> 분석: [business-day-lunar-holidays](../../../analyzes/stocknote/business-day-lunar-holidays/business-day-lunar-holidays.md). plan task: Phase 10 P1 #5.

## 의도

`BusinessDayCalculator` 에 2026/2027 음력 기반 공휴일 + 대체공휴일을 `Set<LocalDate>` 정적 매핑으로 추가. KRX 휴장일에 captureForMarket 가 KIS 호출 시도 자체를 회피.

옵션 B (KRX 캘린더 외부 연동) 와 옵션 C (KIS 빈 응답 감지) 는 본 task 범위 외 — Task #6 또는 별건 plan 에서 처리.

## 변경 사항

### 단일 파일 변경: `BusinessDayCalculator.java`

```java
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

    /** 음력/대체 공휴일 정적 매핑. 매년 갱신 필요 (현재 2026~2027). */
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
            LocalDate.of(2027, 2, 8),    // 설날 (2/6 토 ~ 2/7 일 ~ 2/8 월)
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
```

## 변경 동작

| 시나리오 | 변경 전 | 변경 후 |
|---|---|---|
| `isBusinessDay(2026-02-17)` (설날) | true (휴장 미인지) | **false** (휴장) |
| `addBusinessDays(2026-02-12, 7)` (목요일) | 2026-02-23 (잘못 — 설 3일 영업일로 셈) | **2026-02-26** (정확) |
| `nextBusinessDay(2026-09-25)` (추석) | 2026-09-25 (잘못) | **2026-09-28 (월)** |

## 운영 갱신 정책

- 매년 12월에 다음 해 음력 공휴일 발표 → `LUNAR_AND_SUBSTITUTE_HOLIDAYS` 에 추가하는 PR
- 정부의 임시공휴일/선거일 발표 시 즉시 PR
- 2025년부터 등록 (2025년 데이터는 본 task 범위 — 필요 시 추가)

> **현재 2026 시점**: 2025 가 끝났고 2026~2027 등록. 2028 은 2027 12월에 발표 예정.

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| 정적 매핑 갱신 누락 | 등록 안 된 음력 공휴일에 KIS 호출 → markFailed | 옵션 C (KIS 빈 응답 감지) 가 별건으로 보호 |
| 임시공휴일 발표 (긴급 휴장) | 갱신 PR 전까지 KIS 호출 시도 | 동일 — 옵션 C 보호 |
| `isBusinessDay` Set lookup 비용 | O(1) HashSet — 무시 가능 | n/a |

## 테스트 가능성

`BusinessDayCalculator` 는 의존성 없는 순수 함수. 단위 테스트 작성 가능 (CLAUDE.md 정책상 명시 요청 시만):
- `isBusinessDay(2026-02-17)` → false
- `isBusinessDay(2026-09-25)` → false
- `addBusinessDays(2026-02-12, 7)` → 2026-02-26
- `nextBusinessDay(2026-09-25)` → 2026-09-28

## 작업 리스트

- [ ] `BusinessDayCalculator.java` 에 `LUNAR_AND_SUBSTITUTE_HOLIDAYS` Set 추가
- [ ] `isBusinessDay` 에 LocalDate 매핑 검증 추가
- [ ] javadoc 업데이트
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #5)

## 승인 대기

태형님 승인 후 구현 진행.
