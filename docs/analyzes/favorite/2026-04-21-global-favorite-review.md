---
title: 글로벌 관심 경제지표 실시간 조회 전환 — 리뷰 합성
type: review
status: draft
date: 2026-04-21
issue: "#28"
plan: docs/plans/2026-04-21-001-fix-global-favorite-realtime-indicator-plan.md
agents:
  - code-simplicity-reviewer
  - security-sentinel
  - performance-oracle
  - architecture-strategist
  - pattern-recognition-specialist
---

# 리뷰 합성 — 글로벌 관심 경제지표 실시간 조회 전환 (#28)

## 요약

태형님, 5개 리뷰어를 병렬 실행해 **총 26건** 발견했습니다.

- 계층 규칙/캐시 관리 축은 설계서대로 준수되고 있고, XSS 같은 즉시 차단 사안은 없습니다.
- 다만 설계 의도("스크래핑 폭주 방지") 자체를 훼손할 수 있는 **권한 우회 벡터(P1)** 와 **cold cache 직렬 HTTP 폭발(P1)** 은 머지 전 반영 권장.
- 그 외 단순성(UNKNOWN, DTO 이원화)과 아키텍처(application→application 횡단 의존) 개선 여지.
- dev 프로파일 `permitAll` + `(Long)` 캐스팅 문제는 이번 이슈 범위 밖이라 별도 이슈 권장.

## 우선순위별 집계

| 심각도 | 건수 | 머지 차단 여부 |
|---|---|---|
| 🔴 P1 | 4 | 권장 (차단은 아님) |
| 🟡 P2 | 13 | 가능하면 이번 이슈 내 수정 |
| 🔵 P3 | 9 | 후속 이슈로 분리 가능 |

---

## 🔴 P1 — 이번 이슈 내 수정 강력 권고

### P1-1. [Performance] Cold cache 직렬 HTTP 폭발
- 출처: `performance-oracle`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:184-200`, `src/main/java/com/thlee/stock/market/stockmarket/economics/application/GlobalIndicatorQueryService.java:37-45`
- 현상: `enrichGlobalFavorites` 가 카테고리 순차 루프 + `getIndicatorsByCategory` 내부 stream 이 type 별 `getIndicator` 를 직렬 호출. 최악(관심 지표가 다수 카테고리에 분산) 시 **40회 스크래핑 직렬 → 8~80초** 가능. Tomcat 워커 점유로 동시 사용자 방어 붕괴.
- 권고:
  1) 단기: 카테고리 단위 병렬화(bounded executor, fixed 4~6) + `CompletableFuture.allOf`. 40→8초 수준.
  2) 중기: `GlobalIndicatorPort` 에 `fetchByCategory(category)` 가능 여부 확인 후 HTTP 1회로 통합.
  3) `RestClient` 레벨 타임아웃 상한(10s 등) 강제.

### P1-2. [Performance] enrich cold 경로 SingleFlight 사각지대
- 출처: `performance-oracle`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:167-222`, `src/main/java/com/thlee/stock/market/stockmarket/economics/application/GlobalIndicatorCacheService.java:29-37`
- 현상: `SingleFlightCoordinator` 는 **재조회 경로에만** 적용. 대시보드 최초 진입 시점에 여러 사용자가 같은 cold type 을 동시 요청하면 Trading Economics 에 **N배 중복 스크래핑**. 레이트리밋도 `refreshGlobalIndicator` 에만 걸려 있어 사각지대.
- 권고: `GlobalIndicatorCacheService.getIndicator` 자체를 Caffeine `AsyncLoadingCache` 로 전환 (내장 single-flight) → 미스 시 동일 key 에 대해 loader 하나만 실행. 또는 `getIndicator` 내부에 per-type SingleFlight 적용.

### P1-3. [Simplicity] `FailureReason.UNKNOWN` 미사용 · 계획 범위 초과
- 출처: `code-simplicity-reviewer`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:245-251, 196-199`
- 현상: 계획서는 `FETCH|PARSE|UNKNOWN` 만 명시. 구현엔 `INVALID_CODE` 까지 4종, 그 중 `UNKNOWN` 은 방어 catch-all. 실제로 `getIndicator` 가 던지는 건 `TradingEconomicsFetch/Parse` 뿐이라 사실상 dead branch.
- 권고: `catch (RuntimeException)` 블록 제거 → 상위 전파 또는 `FETCH` 로 병합. `UNKNOWN` 상수 및 프론트 `globalFailureMessage` default 분기 정리.

### P1-4. [Simplicity] `RefreshedGlobalFavorites` ↔ `GlobalRefreshResponse` 이원화 (중복 계층)
- 출처: `code-simplicity-reviewer`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:277`, `src/main/java/com/thlee/stock/market/stockmarket/favorite/presentation/dto/GlobalRefreshResponse.java:12-21`
- 현상: 양쪽 모두 `(indicatorType, items)` 구조. application record 는 enum 보유, presentation 은 String 변환만. 실질 이득 없는 중간 계층.
- 권고: application record 제거하고 Service 가 `List<EnrichedGlobalFavorite>` 반환 → Controller 에서 바로 `GlobalRefreshResponse` 조립. (혹은 그 반대로 하나 제거)

---

## 🟡 P2 — 가능하면 이번 이슈 내 수정

### P2-1. [Security] `endsWith` 권한 체크 우회 벡터 ⚠️
- 출처: `security-sentinel`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:105-108`
- 현상: `indicatorCode` 는 `toggle` 경로에서 포맷 검증 없이 저장됨(P2-2 참조). 사용자가 자기 자신의 관심 지표로 `"X::FOO::CPI"` 등 조작 코드를 등록한 뒤 `POST /api/favorites/global/refresh/CPI` 호출 시 `endsWith("::CPI")` 통과 → **본인이 등록하지 않은 타입의 Trading Economics 스크래핑 강제 트리거**. 설계 목적(스크래핑 폭주 방지) 훼손.
- 권고: `ParsedGlobalFavorite.of(fav).indicatorType() == indicatorType` 로 정확 일치 비교. endsWith 폐기.

### P2-2. [Security] `toggle()` indicatorCode 포맷 검증 부재
- 출처: `security-sentinel`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:50-62`
- 현상: GLOBAL 저장 시 `split("::",2)` 및 `GlobalEconomicIndicatorType.valueOf` 검증이 없어 임의 문자열 저장 가능. P2-1 공격면의 근본 원인.
- 권고: `toggle` 내부에서 sourceType 별 포맷 검증 (GLOBAL: 2파트 + enum, ECOS: 2파트 non-blank). 실패 시 `IllegalArgumentException`.

### P2-3. [Architecture] application → application 횡단 의존 중복
- 출처: `architecture-strategist`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:40-41`
- 현상: `FavoriteIndicatorService` 가 `GlobalIndicatorQueryService`(읽기) + `GlobalIndicatorCacheService`(재조회) 둘 다 주입. QueryService 는 이미 CacheService 를 랩핑(`GlobalIndicatorQueryService.java:30,43`) 하는데 favorite 이 두 층을 동시에 알게 됨 → economics 내부 구조 변화에 강결합.
- 권고: `GlobalIndicatorQueryService` 에 `forceRefresh(type)` 파사드 1줄 추가 → favorite 은 QueryService 단일 의존으로 통일.

### P2-4. [Architecture] `SingleFlightCoordinator` 도메인 소속 부적합
- 출처: `architecture-strategist`, `performance-oracle`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/SingleFlightCoordinator.java:3,17,19`
- 현상: favorite 패키지에 위치하나 락 키가 `GlobalEconomicIndicatorType`. 본질은 "글로벌 지표 스크래핑 억제". 향후 economics 탭에서 동일 기능이 필요하면 favorite 으로의 역의존 발생.
- 권고: 현 이슈 범위에서 재배치는 과함. 클래스명을 `GlobalIndicatorRefreshCoordinator` 로 변경하거나, 주석에 "임시 소유자 favorite, 장기 이관 대상" 명시. P1-2 적용 시 자연스럽게 economics 로 이관됨.

### P2-5. [Pattern] 예외 패키지 배치 기존 관행 이탈
- 출처: `pattern-recognition-specialist`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/exception/*.java`
- 현상: 기존 관행은 `{module}/domain/exception/` (예: `user/domain/exception/DuplicateNicknameException.java`). application 하위 exception 디렉토리 사례 없음. 네이밍 `RefreshRateLimitExceededException` 은 모듈 소속 불명확.
- 권고: `favorite/domain/exception/` 로 이동. 네이밍 `FavoriteRefresh*` 접두어 통일 → `FavoriteRefreshRateLimitExceededException`.

### P2-6. [Pattern] 로그 메시지 포맷 불일치
- 출처: `pattern-recognition-specialist`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:191,194,197`
- 현상: `"global category fetch failed: category={}, cause={}"` + `e.toString()` 형식. 기존 관행(`GlobalIndicatorSaveService.java:68`, `OverseasNewsService.java:46`)은 한국어 메시지 + `e` 를 마지막 인자로 넘겨 stack 출력.
- 권고: `log.error("글로벌 카테고리 조회 실패: category={}", category, e)` 로 통일.

### P2-7. [Pattern] 프론트 Alpine 반응성 — 배열 index 대입
- 출처: `pattern-recognition-specialist`
- 파일: `src/main/resources/static/js/components/favorite.js:124`
- 현상: `globals[i] = { ...fresh, refreshing: false }` index 대입. Alpine 은 array index 변경을 개별 감지하지 않는 경우가 있음. 기존 `home.js:108,111` 는 `.filter(...)` 로 배열 참조 재할당.
- 권고: `.map()` 재할당 또는 `.splice(i, 1, obj)` 로 변경.

### P2-8. [Simplicity] 자명한 WHAT 주석 다수
- 출처: `code-simplicity-reviewer`
- 파일: `FavoriteIndicatorService.java:46-48, 69-71`, `FavoriteIndicatorController.java:31-33, 41-43, 52-54, 65-67`, `GlobalIndicatorCacheService.java:41-42, 45-47`
- 현상: CLAUDE.md "주석은 WHY 만" 원칙 위반.
- 권고: 메서드명/어노테이션으로 자명한 주석 삭제. 프로토콜 설명(4xx 매핑/SingleFlight 결합)은 유지.

### P2-9. [Security] Dev `permitAll` + `getCurrentUserId` `(Long)` 캐스팅 버그 (이슈 범위 외)
- 출처: `security-sentinel`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/infrastructure/security/config/DevSecurityConfig.java:42`, `src/main/java/com/thlee/stock/market/stockmarket/favorite/presentation/FavoriteIndicatorController.java:87-89`
- 현상: dev 에서 익명 요청 시 `AnonymousAuthenticationToken` → `(Long)` 캐스팅 실패 → 500 + 스택 로깅. 로그 스토리지 팽창 벡터.
- 권고: **별도 이슈**로 분리 후 `GlobalExceptionHandler.currentUserId()` 와 동일한 `instanceof Long id ? id : null` 패턴으로 교체하고 null 시 403 던짐. 본 이슈에서는 최소 가드만 추가할지 선택.

### P2-10. [Performance] `findByUserIdAndSourceType` 복합 인덱스 확인
- 출처: `performance-oracle`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/infrastructure/persistence/UserFavoriteIndicatorJpaRepository.java`
- 현상: `(user_id, source_type)` 복합 인덱스 유무 미확인. 없으면 DAU 증가 시 seq scan.
- 권고: Entity/스키마 확인 후 없으면 마이그레이션 추가 검토. (별도 이슈 가능)

### P2-11. [Pattern] Repository 파생 메서드 관행
- 출처: `pattern-recognition-specialist`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:102-108`
- 현상: 기존 관행은 Spring Data 파생 메서드로 DB 필터링. 이번은 in-memory `endsWith`.
- 권고: `findByUserIdAndSourceTypeAndIndicatorCodeEndingWith(...)` 추가. P2-1 수정 방향(정확 매칭 기반)과 함께 검토.

### P2-12. [Simplicity] enrich/refresh 예외 처리 비대칭
- 출처: `code-simplicity-reviewer`
- 현상: enrich 는 3단계(FETCH/PARSE/UNKNOWN) catch, refresh 는 무처리(컨트롤러에서 매핑). P1-3 반영 시 2단계로 축소되어 비대칭감 완화.

### P2-13. [Performance] `getIndicatorsByCategory` 반환 Map 불필요 중간 할당
- 출처: `performance-oracle`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:186-189`
- 권고: 카테고리 병렬화(P1-1) 진행 시 `List<CountryIndicatorSnapshot>` 직접 반환 오버로드로 할당 절반 감소.

---

## 🔵 P3 — 후속 개선

### P3-1. [Security] 에러 메시지에 enum 정보 노출
- 파일: `FavoriteIndicatorService.java:112,117`, `GlobalExceptionHandler.java:122,131`
- 권고: 응답 본문은 일반화 문구(`"잘못된 요청입니다"`, `"요청이 너무 잦습니다"`) + 구체 정보는 로그만.

### P3-2. [Security] 레이트리미터 키 구분자 / maximumSize 경계
- 파일: `RefreshRateLimiter.java:34-40`
- 권고: 숫자 ID 에 나올 수 없는 구분자, size 상향 또는 제거, acquire 성공/실패 메트릭 노출.

### P3-3. [Security] 스크래핑 원문 정화
- 파일: `EnrichedFavoriteResponse.java:104-107`
- 권고: `rawText/unit/countryName/referenceText` 에 제어문자 제거 + 길이 상한(64자) 정화 — 심층 방어.

### P3-4. [Security] `forceRefresh` 무결성 sanity check
- 파일: `GlobalIndicatorCacheService.java:54-63`
- 권고: 건수 최소치 또는 기대 국가 목록 대비 급감 시 거부 로직.

### P3-5. [Performance] 네거티브 캐시 (빈 결과)
- 파일: `GlobalIndicatorCacheService.java:33`
- 현상: `unless=isEmpty` 로 빈 결과 미캐시 → 다음 요청마다 재스크래핑.
- 권고: 60~120s 짧은 TTL 네거티브 캐시 별도 레이어.

### P3-6. [Architecture] Unless 조건 중복 / 캐시 put 조건 일원화
- 파일: `GlobalIndicatorCacheService.java:29-37, 54-63`
- 권고: `private boolean isCacheable(List<?>)` 헬퍼로 추출.

### P3-7. [Architecture] `refreshGlobalIndicator` 책임 과다
- 파일: `FavoriteIndicatorService.java:98-148`
- 현 이슈 범위에선 분리 불요. 후속 단위테스트 도입 시 `GlobalFavoriteAuthorizer` 등 분리 검토.

### P3-8. [Simplicity] `SingleFlightCoordinator` 제네릭 Supplier
- 파일: `SingleFlightCoordinator.java:19`
- 권고: 유지. 테스트 가능성/책임 분리 이득이 비용 대비 큼.

### P3-9. [Simplicity] 프론트 가드 중복
- 파일: `favorite.js:110`, `index.html:339-345`
- 현상: `if (card.failed && !card.refreshable) return;` 가 템플릿 `:disabled="!card.refreshable || card.refreshing"` 와 중복.
- 권고: 하나만 남겨도 무방.

---

## 권고 반영 순서 (제안)

이번 이슈 내 반영 권장 (체크박스 형식):

- [ ] P2-1 + P2-2 권한 체크 재구성 (endsWith → enum 정확 일치 + toggle 검증)
- [ ] P1-3 UNKNOWN 제거 + 프론트 default 분기 정리
- [ ] P1-4 `RefreshedGlobalFavorites` 제거 (또는 역방향 일원화)
- [ ] P2-3 `GlobalIndicatorQueryService.forceRefresh(type)` 파사드 + favorite 단일 의존화
- [ ] P2-6 로그 메시지 한국어 포맷 통일
- [ ] P2-7 프론트 `.map()` 기반 배열 재할당 (반응성 보장)
- [ ] P2-8 자명한 WHAT 주석 제거

별도 이슈 분리 권장:
- [ ] P1-1 카테고리 병렬화 / `fetchByCategory` HTTP 통합
- [ ] P1-2 `AsyncLoadingCache` 전환 (전역 cold 경로 single-flight)
- [ ] P2-5 예외 패키지 이동/리네이밍 (일관성 리팩터)
- [ ] P2-9 dev `permitAll` + `getCurrentUserId` 방어
- [ ] P3 전체 (관측성/무결성/네거티브 캐시)

## 관련 파일 (절대경로)

- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/application/SingleFlightCoordinator.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/application/RefreshRateLimiter.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/application/exception/FavoriteRefreshForbiddenException.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/application/exception/RefreshRateLimitExceededException.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/presentation/FavoriteIndicatorController.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/presentation/dto/EnrichedFavoriteResponse.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/presentation/dto/GlobalRefreshResponse.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/economics/application/GlobalIndicatorCacheService.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/economics/application/GlobalIndicatorQueryService.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/infrastructure/security/config/DevSecurityConfig.java`
- `/Users/thlee/Documents/personal/stock-market/src/main/resources/static/index.html`
- `/Users/thlee/Documents/personal/stock-market/src/main/resources/static/js/components/favorite.js`

## 다음 단계

1. 이 문서를 승인하거나 수정 요청
2. 승인된 P1/P2 항목별로 **설계 문서 보강** (`docs/designs/favorite/...`) 또는 이번 Plan 문서에 수정 섹션 추가
3. 승인 후 구현 단계로 이동

---

# Rev.2 재리뷰 (2026-04-23)

## Rev.2 개요

Phase 5 (P1 4건 반영) 완료 후 4개 에이전트(performance/architecture/simplicity/security)로 **타겟 리뷰** 수행. 초기 리뷰에서 이미 잡은 P2/P3 는 중복 방지를 위해 제외. Phase 5 가 **새로 도입한 이슈만** 집계.

**P1 해소 검증**: 4건 모두 정상 이행 (performance-oracle 확인)
- P1-1 병렬화 `CompletableFuture.runAsync + allOf`, thread-safe 맵 OK
- P1-2 Caffeine `LoadingCache` single-flight 공식 계약 부합 OK
- P1-3 `FailureReason.UNKNOWN` 및 `catch (RuntimeException)` 제거 OK
- P1-4 `RefreshedGlobalFavorites` 삭제, `List<EnrichedGlobalFavorite>` 반환 OK

**Phase 5 새 findings**: P1 3건 + P2 6건 + P3 2건

---

## 🔴 Rev.2 P1 — 머지 전 반영 권고 (Phase 5 도입)

### Rev2-P1-1. [Performance] Caffeine loader null 반환 → cold-cache 브루트포스 재시도
- 출처: `performance-oracle`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/economics/application/GlobalIndicatorCacheService.java:62-68`
- 현상: `loadAndSkipEmpty` 가 빈 결과 시 null 반환 → Caffeine 계약상 entry 미생성 → 다음 `getIndicator` 호출이 **매번** loader 재실행 → Trading Economics HTTP fetch 반복. 빈 결과를 지속적으로 내는 지표(신규 추가 TYPE, HTML 구조 변경으로 파싱 0건 등)가 하나라도 있으면 매 대시보드 요청마다 스크래핑 재수행.
- 영향: 풀 포화 가속, Trading Economics rate-limit/차단 위험
- 권고:
  1) negative cache 별도 레이어 (빈 결과 60s TTL 로 짧게 캐싱)
  2) `Caffeine.expireAfter(Expiry)` 로 empty 항목만 짧은 TTL 적용
  3) 또는 `loadAndSkipEmpty` 가 `List.of()` 반환 후 12h 캐싱을 수용 (간단하나 "12시간 동안 잘못된 빈 값" 리스크)

### Rev2-P1-2. [Performance + Security] 풀 포화 + `.join()` timeout 부재 → DoS/Tomcat 기아
- 출처: `performance-oracle`, `security-sentinel` (동일 사안 교차 확인)
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/infrastructure/config/GlobalFavoriteExecutorConfig.java:21`, `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:203-209`
- 현상: `newFixedThreadPool(4)` + 기본 `LinkedBlockingQueue`(unbounded) + `allOf(...).join()` 로 벽시계 timeout 없음
  - cold 시 카테고리당 HTTP 2~5s × 풀 2 배치 = 워스트 10s+
  - 공격자가 모든 카테고리 커버하는 관심 지표 등록 후 여러 세션 동시 호출 시 풀 포화 → 큐 무한 적재 → Tomcat 워커 `.join()` 블로킹 → favorite 기능 전면 마비
- 영향: cold 지연으로 Nginx timeout 초과 / 정상 트래픽에서도 업스트림 장애 1회로 장시간 마비
- 권고 (우선순위):
  1) `allOf(...).orTimeout(N, SECONDS)` 로 벽시계 상한 적용, 타임아웃된 카테고리는 `FailureReason.FETCH` 로 강등
  2) `ThreadPoolExecutor` 로 직접 구성 → bounded queue + `AbortPolicy` → fast-fail
  3) `globalIndicatorPort.fetchByIndicator` 하단 HTTP 클라이언트 connect/read timeout 고정 확인 (없으면 슬롯 영구 점유)

### Rev2-P1-3. [Performance] daemon thread + JVM shutdown 시 in-flight task 절단
- 출처: `performance-oracle`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/infrastructure/config/GlobalFavoriteExecutorConfig.java:34`
- 현상: `setDaemon(true)` + `destroyMethod=shutdown`. `shutdown()` 은 신규 task 거부만 하고, JVM exit 시 daemon 스레드는 즉시 종료 → 진행 중 HTTP fetch 가 중단되며 `.join()` 대기 중인 Tomcat 워커는 응답 쓰기 실패
- 영향: 롤링 배포/SIGTERM 시 in-flight 대시보드 요청이 500/connection-reset 반환. 원인 추적 어려움
- 권고: `setDaemon(false)` 로 변경 + 커스텀 close 메서드로 `shutdown()` → `awaitTermination(5s)` → `shutdownNow()`. Tomcat `graceful shutdown` 과 보조 맞춤

---

## 🟡 Rev.2 P2 — Phase 5 도입

### Rev2-P2-1. [Performance] `RejectedExecutionException` 미방어
- 출처: `performance-oracle`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:203-209`
- 현상: 풀이 shutdown 중이거나 bounded queue 도입 시 REE 발생 → future 가 exceptionally 완료 → `allOf(...).join()` 이 `CompletionException` 으로 상위까지 전파 → 5xx
- 권고: `allOf.exceptionally(e -> null).join()` 또는 try/catch + 부분 결과 기반 응답 유지

### Rev2-P2-2. [Security] Caffeine loader 예외 + Error/InterruptedException 폴백 부재
- 출처: `security-sentinel`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:241-256` (`fetchCategoryInto`)
- 현상: catch 블록이 `TradingEconomicsFetchException`/`ParseException` 2종만. loader 가 `IllegalStateException` 등 기타 `RuntimeException` 또는 `Error` 계열을 던지면 `fetchCategoryInto` 바깥으로 나감 → `allOf.join()` → `CompletionException` → 500 로그 폭주
- 권고: 외부 스크래핑 특성상 `catch (RuntimeException e)` 폴백 추가 권장 — 단 CLAUDE.md "방어 과잉 금지" 충돌 여지 → 태형님 결정 필요. 최소한 `log.error(..., e)` 를 `log.warn(..., e.toString())` 으로 강등해 로그 저장 비용 절감
- 참고: `InterruptedException` 은 runAsync 내부에서는 일반적으로 발생하지 않지만 shutdown 시 고려

### Rev2-P2-3. [Security] 병렬 실행 시 SecurityContext / MDC 미전파
- 출처: `security-sentinel`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:203-208`
- 현상: `CompletableFuture.runAsync(..., executor)` 가 기본 `MODE_THREADLOCAL` SecurityContext 와 MDC(request-id/userId) 를 새 스레드로 전파하지 않음 → 스크래핑 실패 로그의 request-id 가 끊겨 포렌식 난이도↑
- 권고: `GlobalFavoriteExecutorConfig` 에서 `DelegatingSecurityContextExecutorService` 래핑 + MDC 복사 `Runnable` 데코레이터

### Rev2-P2-4. [Architecture] Cache 추상화 관례 이탈 — `GlobalIndicatorCacheService` 단독 Caffeine 직접 보유
- 출처: `architecture-strategist`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/economics/application/GlobalIndicatorCacheService.java:22-30`
- 현상: 타 캐시 서비스(`StockPriceCacheConfig`, `ExchangeRateCacheConfig` 등)는 모두 `CaffeineCacheManager` bean + `@Cacheable` 패턴. 이번만 예외
- 영향: Actuator `CachesEndpoint` 에서 `globalIndicators` 미노출, 운영 관측 일관성 약화
- 권고: 단기 — Javadoc 에 "single-flight 요구로 인한 이탈" 명시. 중기 — ARCHITECTURE.md §5 에 예외 규정 추가 또는 `CaffeineCacheManager` + loader 주입 패턴으로 일원화 검토

### Rev2-P2-5. [Architecture] `GlobalIndicatorCacheConfig` 위치 부적합
- 출처: `architecture-strategist`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/economics/infrastructure/global/tradingeconomics/config/GlobalIndicatorCacheConfig.java:10-16`
- 현상: `CacheManager` bean 제거 후 TTL/size/이름 상수만 남음. 사용처는 application 계층 Service 하나. application → infrastructure 의 역방향 import 발생. 이름도 "tradingeconomics" 인데 실제로는 캐시 정책 상수
- 권고: 상수를 `GlobalIndicatorCacheService` 내부 `private static final` 로 흡수하거나 application 계층으로 이동. `"globalIndicators"` 캐시명은 현재 아무도 참조 안 함 → YAGNI 제거

### Rev2-P2-6. [Architecture] Controller 수동 `valueOf` — 자동 enum 바인딩 관례 이탈
- 출처: `architecture-strategist`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/presentation/FavoriteIndicatorController.java:82-86`
- 현상: 같은 Controller 47번 라인은 `@RequestParam FavoriteIndicatorSourceType` 으로 Spring 자동 enum 바인딩을 쓰는데, 신규 엔드포인트만 수동 `valueOf`
- 권고: `@PathVariable GlobalEconomicIndicatorType indicatorType` 로 전환. 잘못된 값은 Spring 이 `MethodArgumentTypeMismatchException` 으로 400 매핑 (기존 `GlobalExceptionHandler` 에 해당 핸들러가 있는지만 확인)

### Rev2-P2-7. [Simplicity] `NamedThreadFactory` 정적 중첩 클래스 → 람다 인라인
- 출처: `code-simplicity-reviewer`
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/infrastructure/config/GlobalFavoriteExecutorConfig.java:25, 28-37`
- 현상: 10줄 ThreadFactory 구현이 1회 사용. SAM 람다로 5줄 수렴 가능
- 권고: `AtomicInteger counter = new AtomicInteger(); return Executors.newFixedThreadPool(POOL_SIZE, r -> { ... })`

### Rev2-P2-8. [Simplicity ↔ Architecture 충돌] 7-args 명시 생성자
- 출처: `code-simplicity-reviewer` vs `architecture-strategist` 의견 상충
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/favorite/application/FavoriteIndicatorService.java:48-63`
- 시각 A (simplicity): CLAUDE.md §4-11 "Lombok 사용 필수" 를 근거로 `@RequiredArgsConstructor` 복귀 권장. 필드에 `@Qualifier(GlobalFavoriteExecutorConfig.BEAN_NAME)` 부착 후 Lombok `copyableAnnotations` + Spring per-parameter 해석으로 15줄 삭제 가능
- 시각 B (architecture): `KisStockPriceAdapter` 등 프로젝트 내 선례가 동일하게 명시 생성자 + `@Qualifier` 조합을 사용하므로 관례 일치. 유지 가능
- 권고: 태형님이 "Lombok 일관성" 선호면 A, "Qualifier 명시성" 선호면 B. 실제 작동성에 차이 없음

---

## 🔵 Rev.2 P3

### Rev2-P3-1. [Simplicity] `getIndicator` 의 null→`List.of()` 치환 이유 주석
- 파일: `src/main/java/com/thlee/stock/market/stockmarket/economics/application/GlobalIndicatorCacheService.java:32-35`
- 권고: "로더가 빈 결과를 null 로 치환 → 캐시 entry 없음" 한 줄 주석 (선택 사항)

### Rev2-P3-2. 검증 결과 (교차 확인 — 이슈 아님)
- `@Transactional(readOnly=true)` + 병렬 executor: HTTP 스크래핑 경로에 JPA 쿼리 없음 → 영속성 컨텍스트 전파 이슈 없음 (performance)
- 요청 간 공유 상태: `snapshotMap`/`categoryFailure` 모두 메서드 로컬 변수 → 교차 사용자 누수 벡터 아님 (security)
- JVM 종료 시 외부 세션 유출: RestClient/Apache HC 기반이면 OS 가 소켓 회수 (security)
- 순차→병렬 등가성: 카테고리 중복 없음(`EnumSet`), 부분 실패 격리(`refreshable=false`) 보존 (simplicity)

---

## Rev.2 반영 우선순위 제안

**이번 이슈 내 반영 권장 (P1만)**:
- [ ] Rev2-P1-2 `allOf(...).orTimeout` 추가 + (선택) bounded queue 로 풀 포화 fast-fail
- [ ] Rev2-P1-3 daemon=false + graceful shutdown
- [ ] Rev2-P1-1 negative cache 또는 empty 의 짧은 TTL 도입

**별도 이슈 분리 권장**:
- Rev2-P2-2, Rev2-P2-3 (예외 폴백, SecurityContext/MDC 전파)
- Rev2-P2-4 (Cache 추상화 관례 정리) — ARCHITECTURE.md 갱신 포함
- Rev2-P2-5 (Config 위치 재정렬)
- Rev2-P2-6 (Controller 자동 enum 바인딩)
- Rev2-P2-7, Rev2-P2-8 (Lombok/람다 단순화) — 합쳐서 간단한 후속 PR

## 관련 파일 (Rev.2 추가)

- `/Users/thlee/Documents/personal/stock-market/src/main/java/com/thlee/stock/market/stockmarket/favorite/infrastructure/config/GlobalFavoriteExecutorConfig.java` (신규)
- 기타 Rev.1 관련 파일 목록 동일
