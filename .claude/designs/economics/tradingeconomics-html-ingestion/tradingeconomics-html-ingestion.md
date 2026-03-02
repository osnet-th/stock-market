# TradingEconomics HTML 수집 전체 흐름 설계

- 작성일: 2026-02-26

## 작업 리스트

- [x] `build.gradle`에 Jsoup 의존성 추가
- [x] `GlobalEconomicIndicatorType` enum 정의 (`domain/model`)
- [x] `CountryIndicatorSnapshot`, `IndicatorValue` 도메인 모델 정의 (`domain/model`)
- [x] `GlobalIndicatorPort` 포트 인터페이스 정의 (`domain/service`)
- [x] `TradingEconomicsProperties` 설정 클래스 구현 (`infrastructure/global/tradingeconomics/config`)
- [x] `TradingEconomicsIndicatorRegistry` URL 매핑 구현 (`infrastructure/global/tradingeconomics`)
- [x] `TradingEconomicsHtmlClient` HTML 수집 클래스 구현 (`infrastructure/global/tradingeconomics`)
- [x] `TradingEconomicsTableParser` 테이블 파싱 클래스 구현 (`infrastructure/global/tradingeconomics`)
- [x] `TradingEconomicsValueNormalizer` 값 정규화 클래스 구현 (`infrastructure/global/tradingeconomics`)
- [x] `TradingEconomicsIndicatorAdapter` 어댑터 조립 구현 (`infrastructure/global/tradingeconomics`)
- [x] `TradingEconomicsFetchException`, `TradingEconomicsParseException` 예외 클래스 구현
- [x] `GlobalIndicatorQueryService` 애플리케이션 서비스 구현 (`application`)
- [x] `GlobalIndicatorCacheConfig` 캐시 설정 구현 (`infrastructure/global/tradingeconomics/config`)
- [x] `GlobalIndicatorCacheService` 캐시 전담 서비스 분리 (`application`) — `@Cacheable`, `@CacheEvict` 책임
- [x] `GlobalIndicatorQueryService` 리팩토링 — 캐시 어노테이션 제거, `GlobalIndicatorCacheService` 위임
- [x] `GlobalIndicatorResponse`, `CountryIndicatorRowResponse` 응답 DTO 구현 (`presentation/dto`)
- [x] `GlobalCategoryResponse`, `GlobalIndicatorSummaryResponse` 카테고리 응답 DTO 구현 (`presentation/dto`)
- [x] `GlobalIndicatorController` 컨트롤러 구현 (`presentation`)
- [x] `GlobalIndicatorController#getCategories` 응답 구조 변경 (카테고리 key/displayName + 지표 key/displayName)
- [x] `GlobalIndicatorQueryService#getIndicatorsByCategory` 카테고리별 전체 지표 조회 메서드 추가 (`application`)
- [x] `GlobalCategoryIndicatorResponse` 카테고리별 지표 데이터 응답 DTO 구현 (`presentation/dto`)
- [x] `GlobalIndicatorController#getIndicatorsByCategory` 카테고리별 지표 데이터 조회 API 추가 (`presentation`)
- [x] `application.yml`에 TradingEconomics 설정 추가

## 배경

기존 ECOS(JSON API) 패턴(`ApiClient → Adapter → Port`)을 따르되, TradingEconomics는 API가 없어 HTML 파싱으로 수집한다. 모든 지표 페이지가 동일한 테이블 구조를 사용하므로 **공통 파서 1개 + URL 매핑**으로 처리 가능하다.

## 전체 흐름

```
Controller → GlobalIndicatorQueryService → GlobalIndicatorPort
                                                  ↓
                                    TradingEconomicsIndicatorAdapter
                                                  ↓
                          ┌───────────────────────────────────┐
                          │  1. Registry.getUrl(indicatorType) │
                          │  2. HtmlClient.fetch(url)          │
                          │  3. TableParser.parse(document)    │
                          │  4. Normalizer.normalize(rows)     │
                          └───────────────────────────────────┘
                                                  ↓
                                   List<CountryIndicatorSnapshot>
```

### 단계별 책임

| 단계 | 클래스 | 입력 | 출력 | 책임 |
|------|--------|------|------|------|
| 1 | `Registry` | `GlobalEconomicIndicatorType` | `String (URL)` | enum → URL 매핑 |
| 2 | `HtmlClient` | `String (URL)` | `Document (Jsoup)` | HTTP 요청, HTML 수집 |
| 3 | `TableParser` | `Document` | `List<RawTableRow>` | thead 기반 동적 컬럼 매핑, raw 텍스트 추출 |
| 4 | `Normalizer` | `List<RawTableRow>` | `List<CountryIndicatorSnapshot>` | 숫자/날짜/국가명 정규화, 도메인 모델 변환 |

## 핵심 결정

- **Jsoup 사용**: HTTP 요청 + DOM 파싱 모두 지원, 기존 프로젝트에 Jsoup 의존성 추가 필요
- **헤더 기반 동적 매핑**: 셀 인덱스 고정 금지, `thead th` 텍스트로 컬럼 위치 결정
- **헤더 별칭 매핑**: 한글(`국가`, `마지막`) / 영문(`Country`, `Last`) 모두 대응
- **기존 Port/Adapter 패턴 유지**: `EcosIndicatorPort` 패턴과 동일하게 `GlobalIndicatorPort` 정의
- **파서 공통화**: 모든 TradingEconomics 지표 페이지가 동일 테이블 구조 → 파서 1개로 처리

## HTML 테이블 구조 (파싱 대상)

실제 HTML 기준 (`핵심 소비자 물가 - 국가 목록 - G20.html`):

```
thead: 국가 | 마지막 | 이전 | 참고 | 단위
tbody tr:
  <td><a href="...">아르헨티나</a></td>   ← .text()로 a 내부 텍스트 추출
  <td data-heatmap-value="0">10666</td>   ← .text()로 값 추출
  <td>10392</td>
  <td><span>2026-01</span></td>           ← .text()로 span 내부 추출
  <td>포인트</td>
```

- `Element.text()`가 중첩 태그 텍스트를 자동 추출하므로 별도 selector 불필요
- 헤더 텍스트에 공백 포함 가능 (`마지막 `) → `.trim()` 필수

## 패키지 구조

```
economics/
├── application/
│   ├── EcosIndicatorService.java                   // 기존 유지
│   ├── GlobalIndicatorCacheService.java            // 신규: 캐시 전담 (Cacheable/CacheEvict)
│   ├── GlobalIndicatorQueryService.java            // 신규: 조회 로직 (CacheService 위임)
│   └── dto/
│       └── GlobalIndicatorResult.java              // 신규
├── domain/
│   ├── model/
│   │   ├── Ecos*.java                              // 기존 유지
│   │   ├── GlobalEconomicIndicatorType.java        // 신규: enum
│   │   ├── CountryIndicatorSnapshot.java           // 신규: 국가별 스냅샷
│   │   └── IndicatorValue.java                     // 신규: 값/단위 래퍼
│   └── service/
│       ├── EcosIndicatorPort.java                  // 기존 유지
│       └── GlobalIndicatorPort.java                // 신규: 포트
├── infrastructure/
│   ├── korea/ecos/                                 // 기존 유지
│   └── global/
│       └── tradingeconomics/
│           ├── TradingEconomicsIndicatorAdapter.java
│           ├── TradingEconomicsHtmlClient.java
│           ├── TradingEconomicsTableParser.java
│           ├── TradingEconomicsValueNormalizer.java
│           ├── TradingEconomicsIndicatorRegistry.java
│           ├── config/
│           │   └── TradingEconomicsProperties.java
│           ├── dto/
│           │   ├── RawTableRow.java
│           │   └── ParsedTable.java
│           └── exception/
│               ├── TradingEconomicsFetchException.java
│               └── TradingEconomicsParseException.java
└── presentation/
    ├── GlobalIndicatorController.java              // 신규: API 진입점
    └── dto/
        ├── GlobalIndicatorResponse.java            // 신규: 지표 조회 응답 래퍼
        ├── CountryIndicatorRowResponse.java        // 신규: 국가별 row 응답
        ├── GlobalCategoryResponse.java              // 신규: 카테고리 목록 응답
        ├── GlobalIndicatorSummaryResponse.java     // 신규: 카테고리 내 지표 요약
        └── GlobalCategoryIndicatorResponse.java    // 신규: 카테고리별 지표 데이터 응답
```

## 설정 (`application.yml`)

```yaml
economics:
  api:
    global:
      tradingeconomics:
        base-url: https://ko.tradingeconomics.com
        connect-timeout: 3000
        read-timeout: 5000
        user-agent: "Mozilla/5.0 ..."
```

## 캐시 전략

- 캐시명: `globalIndicators`
- 키: `indicatorType.name()`
- TTL: `12시간`
- 최대 엔트리: `200` (지표 enum 확장 여유 포함)
- 적용 지점: `GlobalIndicatorQueryService#getIndicator`
- 예외/실패 응답 캐싱 금지: `@Cacheable(unless = "#result == null || #result.isEmpty()")` + 예외 발생 시 미캐싱

### Cache Evict 유스케이스

운영 상황에서 TTL 만료를 기다리지 않고 강제 갱신이 가능해야 하므로 아래 유스케이스를 제공한다.

- 단건 evict: `evict(GlobalEconomicIndicatorType indicatorType)`
  - `@CacheEvict(cacheNames = "globalIndicators", key = "#indicatorType.name()")`
  - 특정 지표만 즉시 무효화 후 다음 조회에서 재수집
- 전체 evict: `evictAll()`
  - `@CacheEvict(cacheNames = "globalIndicators", allEntries = true)`
  - 구조 변경/대량 보정 시 전체 캐시 초기화

### 운영 로그 권장 필드

- `indicatorType`
- `cacheHit` / `cacheMiss`
- `evictType(single|all)`
- `elapsedMs`

## 주의사항

- TradingEconomics HTML 구조 변경 가능성 높음 → 셀 인덱스 고정 파싱 절대 금지
- `thead th` 텍스트가 없거나 필수 헤더(`country`, `last`) 누락 시 `TradingEconomicsParseException` 발생
- 개별 row 정규화 실패 시 해당 row만 스킵 (전체 실패 방지)
- 원문 HTML 전체 로그 저장 금지
- 과도한 호출 금지 (캐시 TTL 12시간 적용 + 필요 시 evict 유스케이스로 수동 무효화)

## 구현 예시 링크

- [Domain Model 예시](./examples/domain-model-example.md)
- [Infrastructure Parsing 예시](./examples/infrastructure-parsing-example.md)
- [Infrastructure Adapter 예시](./examples/infrastructure-adapter-example.md)
- [Application Service 예시](./examples/application-service-example.md)
- [Presentation 예시](./examples/presentation-example.md)
