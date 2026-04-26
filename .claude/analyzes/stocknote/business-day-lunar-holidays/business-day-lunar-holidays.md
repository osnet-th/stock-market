# [stocknote] BusinessDayCalculator 음력/임시 공휴일 누락 — D+N 캡처 정전 위험

> ce-review 2026-04-25 P1 #5 (reliability + adversarial 합의). plan task: Phase 10 P1 BusinessDayCalculator 음력 공휴일 보강.

## 현재 상태

`BusinessDayCalculator.java:21-30` — 양력 8개 고정 공휴일만 처리.

```java
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
```

음력 기반 공휴일 누락:
- 설날 연휴 (음력 12/30 ~ 1/2) — 2026: 2/16~19
- 부처님오신날 (음력 4/8) — 2026: 5/24
- 추석 연휴 (음력 8/14~16) — 2026: 9/24~26
- 대체공휴일 / 임시공휴일 / 선거일 / 한시적 휴장일

주석에서도 "간이 처리 범위 밖이며, 이 경우 스냅샷 배치에서 KIS 조회 실패(FAILED)로 자연 처리" 라고 인지.

## 영향 범위

### 시나리오 A: 휴장일 캡처 시도 → empty 응답 → markFailed

`StockNoteSnapshotService.captureForMarket` 가 `addBusinessDays(noteDate, 7)` 결과가 today 와 일치하면 captureTarget 호출. 하지만 today 가 음력 공휴일(KRX 휴장)이면:
- KIS `inquire-price` 응답이 비어있음 → `IllegalStateException("empty current price")` → `markFailed` → retryCount++
- retryPending 10분 배치가 PENDING 만 처리하므로 (Task #24) 자동 복구 안 됨
- 사용자 manualRetry 호출해야 복구 가능

### 시나리오 B: KIS 가 stale 가격 반환

KIS 일부 종목은 휴장일에 전일 종가를 그대로 반환 — markSuccess 로 전이되지만 `closePrice` 가 의미상 잘못된 값(전일 종가). 사용자에게는 "캡처 성공" 으로 보이나 데이터 정확성 손상.

### 시나리오 C: 영업일 도달일 잘못 판정

음력 공휴일을 영업일로 셈하면 `addBusinessDays(noteDate, 7)` 가 KRX 실제 영업일 기준 D+8 을 반환하는 등 D+N 정의 자체가 틀어짐.

## 해결 옵션

### 옵션 A — 음력 공휴일 정적 매핑 추가 (간이)

연도별 공휴일 set 을 코드에 정적으로 등록. 향후 1~2년치 등록 후 매년 갱신.

| 장점 | 단점 |
|---|---|
| 외부 의존 없음, 즉시 반영 | 매년 수동 갱신 필요 |
| 단순 | 임시 공휴일/선거일은 발표 후 반영해야 |
| | 부처님오신날 같은 종교 공휴일도 정부 공식 발표 의존 |

### 옵션 B — KRX 휴장 캘린더 외부 연동

KIS holiday API 또는 KRX 캘린더 다운로드 → 캐싱.

| 장점 | 단점 |
|---|---|
| 정확. 임시 공휴일/선거일 자동 반영 | 외부 API 의존성 추가 |
| 운영 인입 비용 0 | 외부 장애 시 fallback 필요 |
| | 본 PR 범위 초과 (별도 plan) |

### 옵션 C — KIS 응답 휴장 감지 시 retry 차감 회피

KIS 가 빈 응답 / 전일 종가를 반환하는 패턴을 휴장 시그니처로 식별 → markFailed 대신 PENDING 유지 (retryCount 미증가).

| 장점 | 단점 |
|---|---|
| 완벽한 정합 (실제 시장 운영에 맞게 자동 적응) | KIS 응답 패턴 식별 신뢰성 (휴장 vs 일시 장애 구분 어려움) |
| | 영구 PENDING 잔존 위험 (휴장이 길게 끌면) |

### 옵션 D — 옵션 A + 옵션 C 결합 (권장)

음력 공휴일 정적 매핑(2026~2027 등록) + KIS 빈 응답 감지 시 PENDING 유지 (retry 차감 회피). 향후 옵션 B 도입 가능.

| 장점 | 단점 |
|---|---|
| 음력 공휴일 사전 차단 (옵션 A) | 매년 정적 매핑 갱신 (옵션 A 부담) |
| 임시 공휴일 / 누락 case 안전 보호 (옵션 C) | KIS 응답 패턴 식별 로직 추가 |
| 외부 API 의존 없음 | |

## 추천: 옵션 A (이번 task) + 옵션 C (Task #6 또는 별건 plan)

근거:
- 옵션 A 만으로 알려진 음력 공휴일 99% 차단
- 옵션 C 는 markFailed 동작 자체 변경이 필요해 별도 task 와 정합 (Task #6 의 D+N changePercent null 처리와 함께 다루는 게 효율)
- 옵션 B 는 외부 캘린더 도입이라 별도 plan 권장 (본 task 범위 초과)

본 task 에서는 **옵션 A** 만 진행 — 2026/2027 음력 공휴일 정적 매핑.

## 음력 공휴일 (2026, 2027)

### 2026
| 날짜 | 공휴일 |
|---|---|
| 2/16 (월) | 설날 연휴 |
| 2/17 (화) | 설날 |
| 2/18 (수) | 설날 연휴 |
| 5/24 (일) | 부처님오신날 — 일요일이라 휴장 무관 |
| 5/25 (월) | 부처님오신날 대체 |
| 9/24 (목) | 추석 연휴 |
| 9/25 (금) | 추석 |
| 9/26 (토) | 추석 연휴 — 토요일 |

### 2027
| 날짜 | 공휴일 |
|---|---|
| 2/6 (토) ~ 2/8 (월) | 설날 연휴 |
| 5/13 (목) | 부처님오신날 |
| 9/14 (화) ~ 9/16 (목) | 추석 연휴 |

대체공휴일 추가:
- 2026 어린이날 5/5 (화) — 평일
- 2027 어린이날 5/5 (수) — 평일
- 2026 광복절 8/15 (토) — 8/17 (월) 대체

## 코드 위치

| 파일 | 변경 |
|---|---|
| `BusinessDayCalculator.java` | FIXED_HOLIDAYS 외에 LUNAR_HOLIDAYS Set<LocalDate> 추가 |

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #6 P1 AT_NOTE FAILED 시 D+N changePercent null 영구화 방지 | KIS 휴장 빈 응답 감지 + retry 차감 회피 (옵션 C) 와 함께 처리 가능 |
| Task 신규 (별건 plan): KRX 휴장 캘린더 외부 연동 (옵션 B) | 본 task 의 옵션 A 완료 후 검토 |

## 설계 문서

[business-day-lunar-holidays](../../../designs/stocknote/business-day-lunar-holidays/business-day-lunar-holidays.md)
