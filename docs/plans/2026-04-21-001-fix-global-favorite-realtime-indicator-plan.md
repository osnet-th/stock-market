---
title: 글로벌 관심 경제지표 대시보드 실시간 조회 전환
type: fix
status: active
date: 2026-04-21
issue: "#28"
origin: docs/brainstorms/2026-04-21-global-favorite-realtime-indicator-brainstorm.md
---

# 글로벌 관심 경제지표 대시보드 실시간 조회 전환 (#28)

## Overview

대시보드의 **글로벌 관심 경제지표**가 자주 "데이터 없음"으로 표시되는 문제를 해결한다.
현재 경로는 `GlobalIndicatorLatest` DB 테이블을 조회하지만, 이 테이블은 글로벌 경제지표 탭과 독립적으로 갱신되지 않아 stale 또는 empty 상태가 된다.
해결책은 **글로벌 경제지표 탭과 동일한 스크래핑(캐시 경유) 경로**로 관심 지표 조회를 전환하는 것이다.

### 리비전 이력
- **Rev.1 (2026-04-21 초안)**: Phase 1~3 + Phase 4 (수동 검증). 병렬화/캐시 구조 변경은 Deferred.
- **Rev.2 (2026-04-21 리뷰 반영)**: `docs/analyzes/favorite/2026-04-21-global-favorite-review.md` 의 P1 4건 편입. Phase 5 (리뷰 반영) 추가. `GlobalIndicatorCacheService` 변경 금지 제한 해제 (AsyncLoadingCache 전환 허용). 병렬 카테고리 조회 Deferred → 범위 내.
- **Rev.3 (2026-04-23 재리뷰 반영)**: 같은 리뷰 문서의 **Rev.2 재리뷰 섹션** Rev2-P1 3건(loader 브루트포스 / 벽시계 timeout / graceful shutdown) 편입. Phase 6 추가.

## Problem Statement / Motivation

### 현상
- `GET /api/favorites/enriched` 응답에서 사용자의 글로벌 관심 지표들이 `hasData=false` 로 반환됨
- 동일 지표를 글로벌 탭(`GET /api/economics/global-indicators/categories/{category}`)에서 조회하면 값이 보임

### 원인 (리서치 결과)
- `FavoriteIndicatorService.findEnrichedByUserId`(`.../favorite/application/FavoriteIndicatorService.java:55`)의 Global 분기가 `GlobalIndicatorQueryService.findAllLatest()`(`.../economics/application/GlobalIndicatorQueryService.java:67`)를 호출
- 이는 `GlobalIndicatorLatestRepository.findAll()` — 순수 DB 조회. `GlobalIndicatorLatest` 테이블은 `GlobalIndicatorSaveService`가 별도로 채워야 하는데 현재 충분히 채워지지 않음
- 반면 글로벌 탭은 `GlobalIndicatorCacheService.getIndicator(type)`(`.../economics/application/GlobalIndicatorCacheService.java:25`)를 통해 Trading Economics 스크래핑을 Caffeine 캐시 경유로 실시간 조회

### 동기
- 사용자가 관심 지표를 등록해도 유효한 값이 안 나오는 기능 장애
- 데이터 공급 파이프라인(Writer 스케줄러) 보강 없이 **Read 경로만 교체**하여 빠르게 해결

## Proposed Solution

**관심 지표가 속한 카테고리 집합을 도출해 `GlobalIndicatorQueryService.getIndicatorsByCategory(category)`(`.../GlobalIndicatorQueryService.java:37`)를 카테고리 단위로 호출**하여 결과를 in-memory 평탄화한 뒤 사용자의 관심 지표만 필터링해서 반환한다.

### 데이터 흐름 (새 경로)

```
/api/favorites/enriched (GET)
  └─ FavoriteIndicatorService.findEnrichedByUserId(userId)
      └─ [GLOBAL 분기 교체]
          1. userFavoriteRepo → 사용자의 GLOBAL 관심 지표 N건
          2. indicatorCode 파싱 → (countryName, GlobalEconomicIndicatorType) 쌍 집합
             ├─ valueOf 실패는 per-card try/catch 로 격리 (해당 카드 failed=true)
          3. 관심 지표들이 속한 IndicatorCategory 집합 추출
          4. 각 카테고리마다 getIndicatorsByCategory(cat) 호출
             ├─ 내부는 type별 GlobalIndicatorCacheService.getIndicator(type) → Trading Economics 스크래핑 + Caffeine 캐시 히트
             └─ 카테고리 단위 try/catch: 실패 시 해당 카테고리 관심 지표 모두 failed=true + failureReason
          5. CountryIndicatorSnapshot 리스트 → Map<compareKey, snapshot> 평탄화
          6. 사용자의 각 관심 지표를 Map에서 조회 → GlobalItem 변환 (성공/실패)
```

### 재조회 경로 (신규)

```
/api/favorites/global/refresh/{indicatorType} (POST)
  └─ FavoriteIndicatorService.refreshGlobalIndicator(userId, indicatorType)
      1. 권한 체크: 해당 indicatorType을 해당 userId 가 즐겨찾기했는지 확인
      2. 레이트리밋 체크 (user+indicatorType 기준, 예: 60s/1회)
      3. SingleFlight 락 획득 (indicatorType 단위)
      4. 캐시 강제 갱신 (putIfFresh 패턴: 스크래핑 성공 시만 put, 실패는 기존 캐시 유지 + 실패 응답)
      5. 해당 type의 관심 지표만 GlobalItem으로 변환해 반환
```

## Technical Considerations

### 아키텍처 (ARCHITECTURE.md 준수)
- **Presentation**: `FavoriteIndicatorController` 에 `refreshGlobal` 엔드포인트 추가
- **Application**: `FavoriteIndicatorService` 의 Global 분기 로직 교체 + `refreshGlobalIndicator(...)` 추가
- **Domain/Infra**: `GlobalIndicatorPort` / `TradingEconomicsIndicatorAdapter` **변경 없음**
- **Application(economics)**: `GlobalIndicatorCacheService` 에 재조회 API(`forceRefresh`) 추가 및 **Phase 5 에서 cold-single-flight 전환(`AsyncLoadingCache`)** 허용 (Rev.2 재승인 사항)
- `GlobalIndicatorLatestRepository` / `GlobalIndicatorLatest` / Writer 경로 **건드리지 않음** (브레인스토밍 결정)

### DTO
- `EnrichedFavoriteResponse.GlobalItem`(`.../favorite/presentation/dto/EnrichedFavoriteResponse.java:60`)에 필드 신규
  - `boolean failed` — 해당 카드의 실시간 조회 실패 여부
  - `String failureReason` — `FETCH | PARSE | INVALID_CODE` 중 하나 (nullable) — **Rev.2: `UNKNOWN` 제거** (catch-all 방어 블록이 dead branch 였음)
  - `boolean refreshable` — `PARSE`/`INVALID_CODE` 실패는 재조회 무의미 → false
- `hasData` 와 `failed` 의미 명확화: `hasData=false && failed=false` = 정상이지만 값 없음(예: 해당 국가 행 없음), `hasData=false && failed=true` = 실시간 조회 자체 실패
- **Rev.2: 재조회 응답 이원화 제거** — Service 는 `List<EnrichedGlobalFavorite>` 반환. presentation 의 `GlobalRefreshResponse` 가 직접 조립. 기존 `RefreshedGlobalFavorites` application record 는 삭제

### 동시성
- **SingleFlight (재조회)**: `indicatorType` 단위 `ConcurrentHashMap<GlobalEconomicIndicatorType, ReentrantLock>` 로 재조회 동시 실행 억제
- **forced fetch → putIfFresh**: `evict` 직후 타 사용자 요청이 cold cache로 진입하는 구간 최소화. `Cache#put` 으로 성공 결과만 원자적 덮어쓰기
- **Rev.2: SingleFlight (cold-cache 전역)** — `GlobalIndicatorCacheService.getIndicator(type)` 을 Caffeine **동기 `LoadingCache`** 로 전환하여 동일 key 에 대한 loader 중복 실행을 내장 single-flight 로 억제. 대시보드 동시 접속 시 Trading Economics 스크래핑이 N → 1 로 축소. 기존 동기 시그니처(`List<CountryIndicatorSnapshot>` 반환)를 유지해 호출부 변경 없음. TTL/size 는 기존 정책 유지 (12h / 200)
  - (비동기 `AsyncLoadingCache` 는 반환 타입이 `CompletableFuture` 로 바뀌어 호출부 대량 수정 필요 + Spring `@Cacheable(sync=true)` 는 `unless` 와 병용 불가 → Caffeine `LoadingCache` 직접 사용이 single-flight + empty 미캐시 두 요구를 모두 충족하는 유일해)

### 권한 / 보안
- `@PreAuthorize` + JWT 사용자 스코프 내 확인 (기존 관심지표 API 패턴 동일)
- 재조회 API는 해당 사용자가 `indicatorType` 을 **즐겨찾기 중일 때만** 허용 → 403
- 레이트리밋: `user+indicatorType` 기준 최근 60초 내 1회 (Caffeine 기반 경량 토큰 버킷)

### 성능
- cold cache 시 관심 지표가 여러 카테고리에 걸치면 카테고리 수 × 카테고리 내 지표 수 만큼 순차 스크래핑 가능성
- 카테고리 루프에 try/catch 두고 실패 시 다음 카테고리 진행 (부분 실패 격리)
- **Rev.2: 카테고리 병렬 조회 범위 내 반영** — bounded executor (`Executors.newFixedThreadPool(4)`, favorite 모듈 소유 전용 bean) + `CompletableFuture.allOf` 로 카테고리 단위 병렬화. cold p95 목표 < 5s. 이전의 Deferred 선언(병렬화 별도 이슈)은 철회
- 카테고리 내부 type 단위 조회는 `AsyncLoadingCache` 경유이므로 중복 스크래핑 없음 (위 "동시성" 항목 참조)

### 로깅 / 관측성
- 실패 시 `log.error` 에 `{userId, indicatorType, failureReason, exceptionClass}` 구조화 로깅
- Micrometer 카운터는 본 이슈 범위 외 (후속 개선)

## System-Wide Impact

- **Interaction graph**:
  - `FavoriteIndicatorController.getEnrichedFavorites` → `FavoriteIndicatorService.findEnrichedByUserId` → (ECOS 분기는 그대로) + (GLOBAL 분기 신규: `GlobalIndicatorQueryService.getIndicatorsByCategory` → `GlobalIndicatorCacheService.getIndicator` → `GlobalIndicatorPort.fetchByIndicator` → `TradingEconomicsIndicatorAdapter`)
  - 신규: `FavoriteIndicatorController.refreshGlobal` → `FavoriteIndicatorService.refreshGlobalIndicator` →
    `FavoriteIndicatorRepository.findByUserIdAndSourceType(userId, GLOBAL)` + `indicatorCode.endsWith("::" + indicatorType.name())` 필터로 권한 체크 + 응답 대상 카드 확보 →
    `RefreshRateLimiter.tryAcquire(userId, indicatorType)` →
    `SingleFlightCoordinator.run(indicatorType, ...)` →
    `GlobalIndicatorCacheService.forceRefresh(indicatorType)` →
    `CacheManager.getCache("globalIndicators").put(type.name(), snapshots)` (성공 시만)
- **Error propagation**:
  - 카테고리 단위 try/catch 에서 `TradingEconomicsFetchException` → `FETCH`, `TradingEconomicsParseException` → `PARSE` 만 catch. (**Rev.2: catch-all `RuntimeException` 블록 제거** — 실제로 발생 가능한 예외 2종뿐이며 catch-all 은 dead branch + 예외 은닉 리스크)
  - 전체 요청 실패 회피: 카테고리 모두 실패해도 200 OK + 전 카드 `failed=true`
- **State lifecycle risks**:
  - `GlobalIndicatorLatest` 테이블 Writer 경로 유지 → 레거시 데이터 팽창 계속. 이 이슈에선 허용 (별도 이슈로 정리)
  - 캐시 `put` 시 `unless` 조건(`#result == null || #result.isEmpty()`) 직접 재현 필요
- **API surface parity**:
  - 글로벌 탭 엔드포인트는 **변경 없음** (`GET /api/economics/global-indicators/categories/{category}`). 캐시를 공유하므로 관심 지표 재조회가 탭 조회에도 즉시 반영됨 (긍정적 부수 효과)
- **Integration test scenarios** (구현 후 수동 검증 필수):
  1. 여러 카테고리에 걸친 관심 지표 8개 + cold cache 상태에서 `/enriched` 응답 확인
  2. Trading Economics 응답을 일시 차단한 상태에서 카드별 failed 표시 + 다른 카드 정상 표시
  3. 재조회 연타 시 서버 로그에서 실제 스크래핑 호출이 1회만 발생하는지 확인
  4. stale 관심지표(`OLD_TYPE`) 잔존 상태에서 전체 요청 실패 없이 해당 카드만 failed 반환
  5. 다른 사용자의 indicatorType 재조회 시도 → 403

## Acceptance Criteria

### 기능 요구사항
- [ ] `/api/favorites/enriched` GLOBAL 분기가 DB `GlobalIndicatorLatest` 대신 `GlobalIndicatorCacheService` 경유 실시간 조회를 사용
- [ ] 사용자의 관심 지표가 속한 카테고리만 호출 (불필요 카테고리 조회 없음)
- [ ] 글로벌 탭이 반환하는 값과 동일한 값이 대시보드 카드에 표시됨
- [ ] 특정 카테고리 실패 시 **해당 카테고리 관심 지표만 `failed=true`**, 나머지 카테고리 지표는 정상 표시
- [ ] 응답 DTO에 `failed`, `failureReason`, `refreshable` 필드가 포함됨
- [ ] `POST /api/favorites/global/refresh/{indicatorType}` 엔드포인트가 추가되고 단일 `indicatorType` 재조회 결과를 반환
- [ ] 재조회는 JWT 인증 필수 + 본인의 관심 지표일 때만 허용 (아니면 403)
- [ ] 재조회는 user+indicatorType 기준 60초에 1회로 제한 (429 Too Many Requests)
- [ ] stale enum 값(현재 enum에 없는 type) 보유 시 해당 카드만 `failed` 처리, 전체 응답 정상 반환
- [ ] 대시보드 카드에 `failed` 상태 UI (실패 메시지 + 재조회 아이콘) 표시
- [ ] 재조회 중 아이콘 회전 / 버튼 비활성화, 성공/실패에 따라 카드 상태 갱신
- [ ] ECOS 관심 지표 조회 경로는 **변경 없음**

### 품질 요구사항
- [ ] 카테고리 단위 try/catch 로 부분 실패 격리 (한 카테고리 실패가 전체 응답 실패로 번지지 않음)
- [ ] 재조회 동시성: `indicatorType` 단위 single-flight (중복 스크래핑 방지)
- [ ] 재조회 성공 시 `Cache#put` 으로 원자적 갱신 (`evict` 구간 최소화)
- [ ] 모든 실패 경로에 구조화 로깅 (`userId, indicatorType, failureReason`)
- [ ] 기존 글로벌 탭 엔드포인트 동작 변경 없음 (회귀 금지)
- [ ] **Rev.2**: cold 상태 동시 다중 사용자 접속 시 type 당 스크래핑 1회 (AsyncLoadingCache single-flight)
- [ ] **Rev.2**: 관심 지표가 여러 카테고리에 분산된 사용자의 `/enriched` cold p95 < 5s (병렬화)
- [ ] **Rev.2**: `failureReason` 은 `FETCH | PARSE | INVALID_CODE` 3종만 사용 (UNKNOWN 제거)
- [ ] **Rev.2**: `refreshGlobalIndicator` 반환은 application record 없이 `List<EnrichedGlobalFavorite>` 단일 계층

## Implementation Phases

### Phase 1: 백엔드 — DTO 확장 및 Global 분기 교체
- [x] `EnrichedFavoriteResponse.GlobalItem` 에 `failed: boolean`, `failureReason: String?`, `refreshable: boolean` 추가 (`.../favorite/presentation/dto/EnrichedFavoriteResponse.java`)
- [x] `GlobalEconomicIndicatorType` 에 카테고리 접근자가 이미 있다면 그대로 사용, 없다면 `getCategory()` 추가 (확인 필요)
- [x] `CountryIndicatorSnapshot` → `GlobalItem` 변환 매퍼 추가 (Service 내부 private 메서드 혹은 DTO factory)
- [x] `FavoriteIndicatorService.findEnrichedByUserId` Global 분기 교체
  - 사용자의 GLOBAL 관심 지표 수집 → indicatorType set / category set 도출
  - 카테고리별 try/catch 루프로 `getIndicatorsByCategory(cat)` 호출
  - 스냅샷 Map 평탄화 → 관심 지표 기준 `GlobalItem` 생성
  - stale `valueOf` 예외는 per-favorite try/catch 로 격리
- [x] 기존 `GlobalIndicatorQueryService.findAllLatest()` 호출 제거 (해당 메서드 자체는 보존 — 다른 컨슈머 없음 확인됐지만 안전)

### Phase 2: 백엔드 — 재조회 엔드포인트
- [x] `FavoriteIndicatorService.refreshGlobalIndicator(userId, indicatorTypeName): RefreshedGlobalFavorites`
  - 입력 파싱 → `GlobalEconomicIndicatorType.valueOf` 실패 시 `IllegalArgumentException` → 400
  - 권한 체크: `FavoriteIndicatorRepository.findByUserIdAndSourceType(userId, GLOBAL)` 조회 후
    `indicatorCode.endsWith("::" + indicatorType.name())` 필터로 해당 타입 카드 집합 추출 →
    비어있으면 `FavoriteRefreshForbiddenException` → 403
    (설계 초안의 `existsBy(userId, GLOBAL, indicatorType)` 는 `indicatorCode` 포맷이 `countryName::indicatorType` 이라 부정확했고,
    재조회 응답에 카드 목록이 어차피 필요하므로 한 번의 조회로 권한 체크 + 응답 재료를 동시에 얻음)
  - 레이트리밋 체크 (`RefreshRateLimiter` — Caffeine 기반, 60s TTL key=`userId:indicatorType`) → 초과 시 `RefreshRateLimitExceededException` → 429
  - `SingleFlightCoordinator` 로 type 단위 락 (`ConcurrentHashMap<Type, ReentrantLock>`)
  - `GlobalIndicatorCacheService.forceRefresh(type): List<CountryIndicatorSnapshot>` 메서드 추가
    - 내부: `port.fetchByIndicator(type)` 직접 호출 → 성공 시 `cache.put(type.name(), result)`, 실패 시 예외 전파 (캐시는 유지)
  - fresh 스냅샷을 `countryName::indicatorType.name()` 키로 평탄화 → 권한 체크 단계에서 확보한 카드들을 매핑하여
    `EnrichedGlobalFavorite` 리스트로 반환 (`success`/`noData`/`failed(INVALID_CODE)`)
- [x] `FavoriteIndicatorController` 에 `POST /api/favorites/global/refresh/{indicatorType}` 추가
  - `SecurityContextHolder` 에서 userId 조회 (기존 컨트롤러 패턴 일관)
  - 예외 → HTTP 상태 매핑은 `GlobalExceptionHandler` 에 추가 (403/429), 400/502 는 기존 핸들러 재사용

### Phase 3: 프론트엔드 — failed 상태 UI + 재조회 아이콘
- [x] `src/main/resources/static/index.html:331-365` global 반복 블록 수정
  - `card.failed` 분기 추가 (실패 메시지 + 재조회 버튼)
  - 기존 `!card.hasData` 는 "정상이지만 값 없음" 의미로 유지
  - 재조회 아이콘: `&#10227;` (↻) HTML 엔티티 사용 (신규 아이콘 라이브러리 도입 금지)
  - 재조회 중 상태(`card.refreshing`) 표시 — 아이콘 회전 CSS (`animate-spin` Tailwind)
- [x] `src/main/resources/static/js/components/favorite.js` 에 `refreshGlobal(card)` 핸들러 추가
  - `card.refreshing = true` → `POST /api/favorites/global/refresh/{indicatorType}` (API 유틸: `API.refreshGlobalIndicator(type)`)
  - 성공 응답 items 를 `indicatorCode` 기준으로 `homeSummary.enrichedFavorites.global` 배열의 동일 카드 객체 교체 (country 여러 개 동시 갱신)
  - 실패 시 `card.refreshing = false` + `alert('재조회에 실패했어요. 잠시 후 다시 시도해주세요')` (프로젝트 토스트 관행: `alert`)
  - 동일 카드 연타 방지 (`refreshing` 플래그 + `:disabled` 바인딩)
- [x] `card.refreshable === false` (PARSE/INVALID_CODE 실패)이면 재조회 버튼 비활성화 + `title` 툴팁 안내
- [x] `failureReason` → 사람 친화 메시지 변환 헬퍼 `globalFailureMessage(card)` (FETCH / PARSE / INVALID_CODE / 기본)
- [ ] 빈 관심 지표 상태 — 기존 UX 유지 (본 이슈 범위 외)

### Phase 5: 리뷰 반영 (Rev.2) — P1 보강

리뷰 리포트: `docs/analyzes/favorite/2026-04-21-global-favorite-review.md`

#### Phase 5.1: Simplicity — DTO 이원화 제거 (P1-4)
- [x] `FavoriteIndicatorService.RefreshedGlobalFavorites` application record 삭제
- [x] `FavoriteIndicatorService.refreshGlobalIndicator(...)` 의 반환 타입을 `List<EnrichedGlobalFavorite>` 로 변경
- [x] `GlobalRefreshResponse.from(RefreshedGlobalFavorites)` 팩토리 → `GlobalRefreshResponse.of(GlobalEconomicIndicatorType, List<EnrichedGlobalFavorite>)` 로 변경
- [x] Controller 에서 조립 (`GlobalEconomicIndicatorType.valueOf` 를 Controller 진입부에서 수행하고, `IllegalArgumentException` 은 `GlobalExceptionHandler` 의 400 매핑이 처리)

#### Phase 5.2: Simplicity — FailureReason.UNKNOWN 제거 (P1-3)
- [x] `FavoriteIndicatorService.FailureReason.UNKNOWN` 상수 삭제
- [x] `enrichGlobalFavorites` 의 `catch (RuntimeException)` 블록 삭제 → 비-예상 예외는 상위(트랜잭션/컨트롤러) 로 전파. 로그 포맷도 한국어 + `e` 로 표준화 (P2-6 부수 반영)
- [x] 프론트 `globalFailureMessage(card)` 는 기존에 이미 `FETCH | PARSE | INVALID_CODE | default` 명시 분기로 작성되어 있어 추가 변경 없음 (default 는 서버가 미지 값 반환 시 안전장치로 유지)

#### Phase 5.3: Performance — Cold-cache 전역 SingleFlight (P1-2)
- [x] `GlobalIndicatorCacheService` 를 Caffeine **동기 `LoadingCache<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>>`** 기반으로 재구성
  - 기존 `@Cacheable("globalIndicators", key=type.name(), unless=isEmpty)` + `@CacheEvict` 제거
  - `getIndicator(type)` → `cache.get(type)` (Caffeine 내장 single-flight)
  - loader 는 `port.fetchByIndicator(type)` 를 호출하고 결과가 비어있으면 `cache.invalidate(type)` 로 "빈 결과 미캐시" 효과 재현
  - `forceRefresh(type)` → `port.fetchByIndicator(type)` 직접 호출 후 비어있지 않으면 `cache.put(type, fresh)` (실패 시 예외 전파, 캐시 유지)
  - `evict(type)` / `evictAll()` 는 `cache.invalidate(type)` / `cache.invalidateAll()` 로 단순 교체 (시그니처 유지)
- [x] `GlobalIndicatorCacheConfig` 는 TTL/size 상수만 보유하는 구조로 단순화. `CacheManager` bean 제거. Service 내부에서 Caffeine LoadingCache 직접 빌드
- [x] loader 는 빈 결과 시 `null` 반환 → Caffeine 계약에 따라 캐시되지 않음 (기존 `unless=isEmpty` 의미 유지). `getIndicator` 는 `null` → `List.of()` 로 방어
- [x] `getIndicator` 호출부(`GlobalIndicatorQueryService`, `FavoriteIndicatorService`) 시그니처 변경 없음 — 컴파일 회귀 확인 완료

#### Phase 5.4: Performance — 카테고리 병렬 조회 (P1-1)
- [x] favorite 모듈 전용 `ExecutorService` bean 구성 — `Executors.newFixedThreadPool(4)` + `NamedThreadFactory("global-fav-fetch-N")`. Spring `@Bean(destroyMethod = "shutdown")` 으로 종료. 위치: `favorite/infrastructure/config/GlobalFavoriteExecutorConfig.java`
- [x] `FavoriteIndicatorService.enrichGlobalFavorites` 의 카테고리 루프를 `CompletableFuture.runAsync(...).allOf(...).join()` 으로 병렬화
- [x] 기존 try/catch 분기(FETCH/PARSE)는 각 future 내부(`fetchCategoryInto` 헬퍼)에서 수행 → 공유 맵은 `ConcurrentHashMap` 으로 교체
- [ ] 스레드 풀 메트릭/로깅은 후속 이슈(관측성 개선) 로 분리 — 본 이슈에서는 기본 `log.error` 만
- [ ] 응답 p95 < 5s 목표 (Acceptance Criteria 품질 요구사항) — Phase 4 수동 검증 단계에서 실측 필요

### Phase 6: 재리뷰 반영 (Rev.3) — Rev2-P1 보강

리뷰 리포트: `docs/analyzes/favorite/2026-04-21-global-favorite-review.md` (Rev.2 재리뷰 섹션)

#### Phase 6.1: Performance — loader 빈 결과 negative cache (Rev2-P1-1)
- [x] `GlobalIndicatorCacheService` 의 로더를 `loadFromPort` 로 재명명하고 빈 결과 시 `List.of()` 반환 (캐시에 저장)
- [x] Caffeine `Expiry` 구현체 `EmptyAwareExpiry` 로 빈 리스트는 60s, 비어있지 않은 리스트는 12h 차등 관리. `expireAfterRead` 는 기존 잔여시간 유지
- [x] `GlobalIndicatorCacheConfig` 에 `EMPTY_TTL = Duration.ofSeconds(60)` 상수 추가

#### Phase 6.2: Performance — 카테고리 병렬 조회 wall-clock timeout (Rev2-P1-2)
- [x] `GlobalFavoriteExecutorConfig.WALL_CLOCK_TIMEOUT_SECONDS = 8L` 상수 추가 — cold p95 목표 5s 대비 여유
- [x] `FavoriteIndicatorService.enrichGlobalFavorites` 에서 `allOf(...).get(WALL_CLOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)` 로 벽시계 상한 적용
- [x] `TimeoutException` / `ExecutionException` / `InterruptedException` 3종 catch — 미완료 카테고리 `future.cancel(true)` + `categoryFailure.putIfAbsent(category, FailureReason.FETCH)` 강등
- [x] `Map<IndicatorCategory, CompletableFuture<Void>>` (`EnumMap`) 구조로 변경해 타임아웃 시 카테고리 식별

#### Phase 6.3: Performance — Graceful shutdown (Rev2-P1-3)
- [x] `GlobalFavoriteExecutorConfig.NamedThreadFactory` 를 `setDaemon(false)` 로 변경 — JVM exit 시 즉시 잘림 방지
- [x] `@Bean(destroyMethod = "shutdown")` 제거. Configuration 클래스에 `@PreDestroy shutdownGracefully()` 메서드 추가 — `shutdown()` → `awaitTermination(5s)` → 미완료 시 `shutdownNow()`
- [x] Executor 인스턴스를 Configuration 필드로 보관해 `@PreDestroy` 에서 참조 (wrapper 클래스 회피 — 단순성 유지)
- [ ] 여러 카테고리에 걸친 관심 지표 8개로 `/enriched` 응답 확인 (cold → warm)
- [ ] Trading Economics 일시 차단(네트워크 차단)으로 부분 실패 UX 확인
- [ ] 재조회 연타 시 로그에서 실제 스크래핑 1회만 발생 확인
- [ ] stale enum 값 DB에 주입하여 해당 카드만 failed 반환 확인
- [ ] 타 사용자 indicatorType 재조회 시도 → 403
- [ ] 레이트리밋 동작 확인 (60초 내 2회 호출 → 429)
- [ ] **Rev.2**: cold 상태에서 동시 N명 접속 시 Trading Economics 스크래핑이 type별 1회만 발생하는지 로그 확인 (AsyncLoadingCache single-flight 검증)
- [ ] **Rev.2**: 관심 지표가 8개 카테고리 전체에 분산된 사용자의 `/enriched` 응답 p95 < 5s 목표 실측

## Success Metrics

- 대시보드 관심 글로벌 지표의 `failed=false && hasData=true` 비율 (기대: cold 이후 95%+)
- `/api/favorites/enriched` p95 응답 시간 (cold 시 주의, 목표 < 5s)
- 재조회 성공률 / 실패 이유별 분포 (로그 기반)

## Dependencies & Risks

### Dependencies
- `GlobalIndicatorCacheService`, `GlobalIndicatorQueryService.getIndicatorsByCategory`, `IndicatorCategory` enum — 모두 기존 존재 (신규 의존성 없음)
- Caffeine `CacheManager` — 이미 구성됨 (`GlobalIndicatorCacheConfig`)

### Risks
1. **cold cache 응답 지연** — 관심 지표가 많은 카테고리에 분산돼 있으면 여러 카테고리 스크래핑 누적 → Nginx 타임아웃 위험
   - 완화(초안): 카테고리 단위 실패 격리 → 응답은 진행하되 실패 카테고리 `failed` 표시
   - **완화(Rev.2)**: (a) 카테고리 병렬 조회 (Phase 5.4) + (b) `AsyncLoadingCache` 기반 전역 single-flight (Phase 5.3) 로 N배 스크래핑 N→1 축소
   - 잔여 리스크: 스레드 풀 포화 (fixed 4 초과 동시 cold 요청 시 대기). 메트릭으로 관찰하고 필요 시 풀 크기 조정
2. **Trading Economics HTML 구조 변경** (PARSE 실패) — 재조회해도 무의미
   - 완화: `refreshable=false` 로 재조회 버튼 비활성화 + 관리자 알림은 본 이슈 범위 외
3. **캐시 공용성** — 한 사용자의 재조회가 전체 사용자 캐시에 영향
   - 완화: `evict` 대신 `put` (성공 시만) + 레이트리밋
4. **stale enum 값** — 과거 enum 이름이 `UserFavoriteIndicator` 에 남아있을 수 있음 (2026-04-16 brainstorm 에서 파악)
   - 완화: per-favorite `valueOf` try/catch 로 격리
5. **권한 체크 누락** — 재조회는 본인 관심지표만 허용
   - 완화: Repository 존재 체크 + 인증 필수

## Sources & References

### Origin
- **Brainstorm**: [docs/brainstorms/2026-04-21-global-favorite-realtime-indicator-brainstorm.md](../brainstorms/2026-04-21-global-favorite-realtime-indicator-brainstorm.md)
  - 캐리오버 결정: (1) `GlobalIndicatorCacheService` 재사용, (2) 카테고리 단위 일괄 조회, (3) 글로벌만 스코프, (4) 지표별 failed + 재조회 아이콘, (5) DB Latest 유지

### Internal References
- `.../favorite/presentation/FavoriteIndicatorController.java:68` — `/api/favorites/enriched` 엔드포인트
- `.../favorite/application/FavoriteIndicatorService.java:55,80-92` — Global 분기 교체 대상
- `.../favorite/presentation/dto/EnrichedFavoriteResponse.java:60-92` — `GlobalItem` DTO 확장 위치
- `.../economics/application/GlobalIndicatorCacheService.java:25-35` — 재사용 서비스 + `evict` 존재
- `.../economics/application/GlobalIndicatorQueryService.java:37,67` — 카테고리 일괄 조회 메서드 재사용
- `.../economics/domain/model/GlobalEconomicIndicatorType.java:10-66` — 카테고리 매핑 (enum 첫 인자)
- `.../economics/domain/model/GlobalIndicatorLatest.java:56` — `toCompareKey` 포맷 `countryName::indicatorType.name()`
- `.../economics/infrastructure/global/tradingeconomics/TradingEconomicsIndicatorAdapter.java:23` — 스크래핑 어댑터
- `.../economics/config/GlobalIndicatorCacheConfig.java:19-21` — Caffeine TTL 12h, size 200
- `src/main/resources/static/index.html:331-365` — 대시보드 global 카드 템플릿
- `src/main/resources/static/js/components/favorite.js:50-91` — 프론트 관심 지표 컴포넌트

### Related Work
- 선행 brainstorm: [docs/brainstorms/2026-04-16-global-favorite-enriched-500-brainstorm.md](../brainstorms/2026-04-16-global-favorite-enriched-500-brainstorm.md) — 500 에러 패턴, stale enum 리스크
- 선행 brainstorm: [docs/brainstorms/2026-04-15-favorite-indicator-dashboard-brainstorm.md](../brainstorms/2026-04-15-favorite-indicator-dashboard-brainstorm.md) — 대시보드 구조 결정
- Solution: [docs/solutions/architecture-patterns/global-indicator-history-mirroring.md](../solutions/architecture-patterns/global-indicator-history-mirroring.md) — 글로벌 지표 3테이블 구조

### Conventions
- [CLAUDE.md](../../CLAUDE.md) — 설계 선행, 테스트는 명시 요청 시만, YAGNI, Lombok 필수
- [ARCHITECTURE.md](../../ARCHITECTURE.md) — 레이어 경계, DTO/Entity 분리

## Open Questions

- 없음. (모두 브레인스토밍 + SpecFlow 단계에서 해결)

## Deferred / Out-of-Scope

- ~~병렬 카테고리 조회 (`CompletableFuture`) — 실측 후 별도 이슈~~ **Rev.2: 본 이슈 범위 내 편입 (Phase 5.4)**
- `GlobalIndicatorLatest` 테이블 / Writer 제거 — 별도 리팩토링 이슈
- Micrometer 메트릭 추가 — 별도 관측성 개선 이슈
- 전체 "재시도 Retry-All" 버튼 — UX 관찰 후 결정
- ECOS 관심 지표의 실시간 전환 여부 — 사용자 확인상 현재 정상 동작, 별도 이슈로 분리 시 검토