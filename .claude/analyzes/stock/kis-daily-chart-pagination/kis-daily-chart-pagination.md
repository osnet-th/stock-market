# [stock/kis] 일봉 100영업일 한계 — silent truncation

> ce-review 2026-04-25 P1 #7 (performance + adversarial 합의). plan task: Phase 10 P1 일봉 분할 호출.

## 현재 상태

`KisStockPriceAdapter.getDailyHistory` (L108-122) 가 `priceClient.getDomesticDailyChart(stockCode, from, to)` 단일 호출.

KIS `inquire-daily-itemchartprice` (TR `FHKST03010100`) 응답은 **output2 배열 최대 100건**. from~to 범위가 100영업일 초과여도 가장 최근 100건만 반환되고 오래된 데이터는 잘린다.

`StockNoteChartService.MAX_PERIOD_DAYS = 365` 까지 허용 → 캘린더 365일 ≈ 영업일 ~250일 → **단일 호출 시 ~5개월치만 받고 나머지 누락**.

## 영향 범위

### 시나리오: 사용자가 1년 차트 선택

1. 프론트가 `period=365` 로 `/api/stock-notes/by-stock/{stockCode}/chart` 호출
2. ChartService 가 `from = today.minusDays(365)` 로 `stockPriceService.getDailyHistory(...)` 호출
3. KisStockPriceAdapter 가 단일 호출 → 최근 ~100영업일치만 (약 5개월)
4. ChartDataResponse.prices 가 ~5개월치만 채워짐
5. 프론트 차트가 5개월치 line 만 그림 — 1년 전 ~ 5개월 전 구간은 비어있음
6. **resolveNoteIndex** 가 잘린 labels 에 없는 noteDate 를 0번 fallback 매핑 → 1년 전 기록이 차트의 가장 왼쪽 끝(5개월 전 위치)에 잘못 표시
7. 사용자에게 안내 없음 (silent)

### 부수 영향

- 차트 scatter 마커 위치 부정확 (Task #39 P3 와 결합되어 0번 fallback 노출 빈도 증가)
- 사용자가 "왜 1년 차트가 짧게 나오지?" 의문

## 해결 옵션

### 옵션 A — KisStockPriceAdapter 안에서 청크 루프 (권장)

`getDailyHistory` 가 100영업일 단위로 슬라이딩 윈도우 호출 후 결과 합병.

| 장점 | 단점 |
|---|---|
| KisStockPriceClient 인터페이스 변경 없음 | 어댑터 복잡도 증가 |
| 단일 책임 — 페이지네이션은 어댑터의 관심사 | safety guard 필요 (무한 루프 방지) |
| caller (ChartService) 코드 변경 없음 | |

### 옵션 B — KIS tr_cont 연속조회 사용

`KisApiClient.getWithContinuation` 으로 tr_cont 헤더 기반 페이지네이션.

| 장점 | 단점 |
|---|---|
| KIS 표준 패턴 | KIS inquire-daily-itemchartprice 가 tr_cont 지원하는지 확인 필요 (보통 일별 시세는 미지원) |
| | 검증 비용 |

### 옵션 C — 호출 측 (ChartService) 에서 분할

ChartService 가 365일을 100영업일 단위로 잘라 어댑터를 여러 번 호출.

| 장점 | 단점 |
|---|---|
| 어댑터 단순 유지 | application 레이어가 KIS 한계 인지 필요 (포트 추상화 위반) |
| | 다른 caller 도 동일 처리 필요 |

## 추천: 옵션 A

근거:
- 페이지네이션은 인프라 어댑터의 관심사 (포트-어댑터 분리 정합)
- ChartService / 다른 caller 가 KIS 의 100건 한계를 몰라도 됨
- 청크 루프는 단순 (safety guard 만 있으면 OK)

## 슬라이딩 윈도우 전략

KIS 응답이 가장 최근 N건을 반환하므로 **windowEnd 를 후방 슬라이딩**:

```
result = []
windowEnd = to
safetyGuard = 0
while (windowEnd >= from && safetyGuard++ < 10):
    windowStart = max(from, windowEnd - CHUNK_CALENDAR_DAYS)  // CHUNK_CALENDAR_DAYS ≈ 140 (영업일 100일)
    chunk = client.getDomesticDailyChart(stockCode, windowStart, windowEnd)
    items = mapper.fromDailyChart(chunk.items)  // ASC 정렬
    if items.empty: break
    result.addAll(items)
    LocalDate oldestInChunk = items.get(0).date()
    if !oldestInChunk.isAfter(from): break  // from 범위 도달
    windowEnd = oldestInChunk.minusDays(1)

// 중복 제거 + ASC 정렬
return dedupAndSort(result);
```

**CHUNK_CALENDAR_DAYS = 140**: 영업일 100일 = 캘린더 약 140일 (주말 + 공휴일 포함). 안전 마진으로 140 사용. 더 짧게 잡아 안전하게는 120도 가능.

**safety guard = 10 회**: 365일 / 100영업일 ≈ 2.5 청크 → 10회는 충분. 무한 루프 방지.

## 코드 위치

| 파일 | 변경 |
|---|---|
| `KisStockPriceAdapter.getDailyHistory` | 단일 호출 → 청크 루프 + dedup |

`KisStockPriceClient.getDomesticDailyChart` 변경 없음. `KisStockPriceMapper.fromDailyChart` 변경 없음 (ASC 정렬 보장).

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #28 P2 일봉 long TTL 캐시 | 청크 루프 호출 결과를 (stockCode, from, to) 키로 캐싱 가능 |
| #39 P3 frontend resolveNoteIndex 0 fallback | 본 task 가 365일 누락을 해결하면 fallback 빈도 감소 (별건) |

## 설계 문서

[kis-daily-chart-pagination](../../../designs/stock/kis-daily-chart-pagination/kis-daily-chart-pagination.md)