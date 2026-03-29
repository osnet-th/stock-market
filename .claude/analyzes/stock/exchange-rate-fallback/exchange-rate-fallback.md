# 분석: 해외 주식 환율 적용 실패 — DEFAULT_RATE = 1 문제

## 현재 상태

해외 주식(FIG, GOOGL)의 평가 금액이 비정상적으로 낮게 표시됨:
- FIG 80주 × 20.19 USD = **1,600원** (정상: ~2,200,000원)
- GOOGL 6주 × 274.34 USD = **1,644원** (정상: ~2,300,000원)

## 문제점

`KoreaEximExchangeRateAdapter.getRate("USD")` 호출 시:

1. 한국수출입은행 API 호출 실패 또는 캐시 비어있음
2. `rateCache.get("USD")` → `null` 반환
3. **`DEFAULT_RATE = BigDecimal.ONE` 반환** (line 25, 47)
4. `currentPrice * 1 = 20.19` → 원화가 아닌 USD 가격 그대로 사용
5. `20 * 80주 = 1,600원`

## 근본 원인

- `KoreaEximExchangeRateAdapter` (line 72-74): API 실패 시 **로그 없이 조용히 무시**
- `DEFAULT_RATE = BigDecimal.ONE`: fallback 없이 **환율 1**을 반환
- 수출입은행 API: 비영업일/영업일 11시 이전 데이터 없음 가능

## 영향 범위

- **해외 주식 전체**: 평가 금액, 수익률, 포트폴리오 자산 배분 비율 모두 오류
- **국내 주식**: 영향 없음 (KRW → `BigDecimal.ONE` 정상)

## 코드 위치

| 파일 | 라인 | 내용 |
|------|------|------|
| `KoreaEximExchangeRateAdapter.java` | 25 | `DEFAULT_RATE = BigDecimal.ONE` |
| `KoreaEximExchangeRateAdapter.java` | 47 | `return rate != null ? rate : DEFAULT_RATE` |
| `KoreaEximExchangeRateAdapter.java` | 72-74 | API 실패 시 catch 로그 없음 |
| `StockPriceResponse.java` | 64 | `price.multiply(exchangeRate)` — 환율 1이면 원화 환산 안 됨 |

## 해결 방안

ECOS 경제지표에 `className: "환율"`, `keystatName: "원/달러"` 데이터가 DB에 저장되어 있음 (`EcosIndicatorLatest` 테이블). 이를 fallback으로 활용:

1. 수출입은행 API 성공 → 기존 로직 유지
2. 수출입은행 API 실패 → ECOS `원/달러` 지표 값을 USD 환율로 사용
3. ECOS도 없으면 → 예외 또는 로그 경고 (환율 1 반환 금지)