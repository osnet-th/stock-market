# 설계: 환율 조회 실패 시 ECOS 경제지표 fallback

## 배경

분석 문서: `.claude/analyzes/stock/exchange-rate-fallback/exchange-rate-fallback.md`

## 핵심 결정

수출입은행 API 실패 시 `DEFAULT_RATE = 1`을 반환하는 대신, ECOS DB에 저장된 `원/달러` 환율 지표를 fallback으로 사용한다.

## 구현 방식

`KoreaEximExchangeRateAdapter`에서 ECOS fallback을 직접 처리한다.

### 변경 대상

| 파일 | 변경 내용 |
|------|-----------|
| `EcosIndicatorLatestRepository.java` | `findByClassNameAndKeystatName()` 메서드 추가 |
| `EcosIndicatorLatestRepositoryImpl.java` | 위 메서드 구현 |
| `EcosIndicatorLatestJpaRepository.java` | JPA 쿼리 메서드 추가 |
| `KoreaEximExchangeRateAdapter.java` | ECOS fallback 로직 추가, 실패 로그 추가 |

### 작업 리스트

- [x] `EcosIndicatorLatestRepository`에 `findByClassNameAndKeystatName(String, String)` 추가
- [x] `EcosIndicatorLatestRepositoryImpl`에 구현 추가
- [x] `EcosIndicatorLatestJpaRepository`에 JPA 쿼리 메서드 추가 (findById 활용으로 불필요)
- [x] `KoreaEximExchangeRateAdapter`에 ECOS fallback 로직 추가
  - 생성자에 `EcosIndicatorLatestRepository` 의존성 주입
  - `getRate()`: 수출입은행 캐시 miss 시 ECOS `원/달러` 지표 값 조회
  - API 실패 시 로그 추가 (`log.warn`)
  - ECOS도 없을 때만 `DEFAULT_RATE` 반환 + 경고 로그

### 로직 흐름

```
getRate("USD") 호출
  ├── rateCache에 "USD" 있음 → 반환
  ├── rateCache 비어있음 → loadRatesIfEmpty() 시도
  │     ├── 수출입은행 API 성공 → 캐시 저장 → 반환
  │     └── 수출입은행 API 실패 → ECOS fallback
  │           ├── ECOS "환율"::"원/달러" 조회 성공 → 해당 값 반환
  │           └── ECOS도 없음 → DEFAULT_RATE(1) + 경고 로그
  └── rateCache에 "USD" 없음 (다른 통화만 있음) → ECOS fallback 동일
```

### 주의사항

- ECOS 환율 값은 `String` 타입 (`dataValue` 필드) → `BigDecimal` 변환 필요
- ECOS는 USD만 제공 (`원/달러`). 다른 통화(JPY, EUR 등)는 수출입은행 전용
- 순환 의존 없음: `stock` 모듈이 `economics` 모듈의 repository를 참조 (단방향)