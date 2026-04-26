# [stock/kis] KisStockPriceAdapter.getDailyHistory 청크 루프 (옵션 A)

> 분석: [kis-daily-chart-pagination](../../../analyzes/stock/kis-daily-chart-pagination/kis-daily-chart-pagination.md). plan task: Phase 10 P1 #7.

## 의도

`KisStockPriceAdapter.getDailyHistory` 안에서 슬라이딩 윈도우로 KIS API 를 여러 번 호출 → 결과 합병 + 중복 제거. caller / KisStockPriceClient / KisStockPriceMapper 변경 없음.

## 변경 사항

### 단일 파일 변경: `KisStockPriceAdapter.getDailyHistory`

```java
private static final int CHUNK_CALENDAR_DAYS = 140;       // 영업일 100일 ≈ 캘린더 140일
private static final int MAX_CHUNK_ITERATIONS = 10;       // 안전 가드 (365일 ÷ 100 ≈ 3회면 충분)

@Override
public List<DailyPrice> getDailyHistory(String stockCode, MarketType marketType, ExchangeCode exchangeCode,
                                        LocalDate from, LocalDate to) {
    if (!marketType.isDomestic() || from == null || to == null || from.isAfter(to)) {
        return List.of();
    }
    Map<LocalDate, DailyPrice> dedup = new LinkedHashMap<>();   // 중복 제거 (date 기준)
    LocalDate windowEnd = to;
    int iterations = 0;
    try {
        while (!windowEnd.isBefore(from) && iterations++ < MAX_CHUNK_ITERATIONS) {
            LocalDate windowStart = windowEnd.minusDays(CHUNK_CALENDAR_DAYS);
            if (windowStart.isBefore(from)) {
                windowStart = from;
            }
            KisDailyChartResponse response = priceClient.getDomesticDailyChart(stockCode, windowStart, windowEnd);
            List<DailyPrice> chunk = KisStockPriceMapper.fromDailyChart(response.getItems());  // ASC 정렬
            if (chunk.isEmpty()) {
                break;
            }
            for (DailyPrice p : chunk) {
                dedup.putIfAbsent(p.date(), p);
            }
            LocalDate oldestInChunk = chunk.get(0).date();
            if (!oldestInChunk.isAfter(from)) {
                break;   // from 범위 도달
            }
            windowEnd = oldestInChunk.minusDays(1);
        }
    } catch (Exception e) {
        log.warn("KIS 일봉 조회 실패 — degrade to partial/empty: stockCode={}, range={}~{}, collected={}, reason={}",
                stockCode, from, to, dedup.size(), e.getMessage());
        // 부분 결과라도 반환 (이미 모은 dedup) — empty 도 허용
    }
    return dedup.values().stream()
            .sorted(Comparator.comparing(DailyPrice::date))
            .toList();
}
```

### 임포트 추가

```java
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
```

## 변경 동작

| 시나리오 | 변경 전 | 변경 후 |
|---|---|---|
| period=90 (단일 청크) | 1회 호출, ~60 영업일 반환 | 1회 호출, 동일 |
| period=180 (2 청크) | 1회 호출, ~100 영업일 반환 (jangtruncated) | 2회 호출, ~120 영업일 반환 |
| period=365 (3 청크) | 1회 호출, ~100 영업일 반환 (잘림) | 3회 호출, ~250 영업일 반환 (정상) |
| 중간 청크 KIS 실패 | 빈 리스트 | catch 진입 — 이미 모은 chunk 부분 반환 (degrade gracefully) |

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| KIS 호출 회수 증가 (1회 → 3회) | KIS rate limit 부담 | Task #28 일봉 long TTL 캐시로 흡수 (별건) |
| 청크 사이 중복 데이터 | LinkedHashMap putIfAbsent 로 dedup | OK |
| 무한 루프 위험 | MAX_CHUNK_ITERATIONS=10 안전 가드 | OK |
| 부분 실패 (중간 청크 fail) | catch 안에서 이미 모은 dedup 반환 → 차트 부분 표시 | log.warn 으로 운영 가시성 |

## 작업 리스트

- [ ] `KisStockPriceAdapter.java` getDailyHistory 청크 루프로 교체
- [ ] 임포트 추가 (LinkedHashMap, Map, Comparator)
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #7)

## 승인 대기

태형님 승인 후 구현 진행.
