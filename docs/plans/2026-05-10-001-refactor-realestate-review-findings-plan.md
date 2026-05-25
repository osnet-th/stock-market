---
title: "refactor: realestate review findings follow-up (P1 잔여 10건 + P2 핵심)"
type: refactor
status: active
date: 2026-05-10
origin: docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md
parent_review: docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md
issue: 41
branch: TBD (별도 worktree 권장)
---

# refactor: realestate review findings follow-up

## Overview

`/ce:review` (issue #41 PR 직전 실행) 결과의 P1 잔여 10건과 P2 핵심 항목을 정리한 후속 작업 plan. 본 PR(parent plan `2026-05-06-001-...`)의 Unit 9에서 P0 3건 + P1 #12/#13 동시 해결됨. 나머지 항목을 우선순위 그룹별로 별도 unit으로 분리해 incremental 진행한다.

## Problem Frame

부동산 신규 도메인 PR review에서 P0 3건은 즉시 해소했으나, P1 잔여 10건은 데이터 정합성·보안·컨벤션·contract 측면에서 후속 정리가 필요하다. 본 plan은 별도 plan으로 분리해 parent PR 머지를 차단하지 않으면서, 후속 PR 단위로 분할 추적한다.

## Requirements Trace

각 Unit은 review finding 번호와 1:1 매핑된다.

- R1 (Unit 10): #4 previousReferenceText 보존 정합 + #11 compareKey VO 추출
- R2 (Unit 11): #5 인증키 로그 누출 마스킹 + #6 anonymous principal 401 가드
- R3 (Unit 12): #7 DELETE body 제거 + #8 request body 검증 + #15 query param 케이스 통일
- R4 (Unit 13): #9 메소드 길이 정리 + #10 URL 파싱 공통 유틸
- R5 (Unit 14): #14 단위 테스트 우선순위 5종 작성 (명시 요청 시)

## Scope Boundaries

- P0 3건은 parent plan Unit 9에서 완료 — 본 plan 범위 밖.
- P2/P3 finding 19건은 advisory로 분류된 항목 위주이므로 본 plan에는 권장만 기록하고 unit화하지 않는다.
- 외부 인증키 발급/통계표 ID 매핑(P2 #27)은 운영 단계 작업 — plan 범위 밖.
- 챗봇 부동산 인식(P2 #21)은 도메인 횡단 + ChatMode public API 변경 → 별도 brainstorm 필요.

## Implementation Units

- [x] **Unit 10: 데이터 정합성 — compareKey VO + previousReferenceText fix**

**Goal:** P1 #4 + #11 묶음 해소. compareKey 정의 중복 제거 + referenceText 롤오버 시 previous 보존.

**Requirements:** R1

**Files:**
- Create:
  - `realestate/domain/model/IndicatorKey.java` — `record IndicatorKey(regionCode, category, source, indicatorCode)` (referenceText 제외 — PK 단위)
- Modify:
  - `realestate/domain/model/RealEstateMarketIndicator.java` — `compareKey()` 제거, `pkKey()` 추가 (IndicatorKey 반환)
  - `realestate/domain/model/RealEstateMarketLatest.java` — 동일 (compareKey 제거, pkKey 추가)
  - `realestate/application/RealEstateMarketSaveService.java`
    - `loadLatestMap` 키를 `IndicatorKey` 기반으로 변경 (referenceText 제외)
    - `mergeLatest`에서 `existing != null` 케이스가 referenceText 변경 시에도 정상 lookup
    - `previousReferenceText = referenceChanged ? existing.getReferenceText() : existing.getPreviousReferenceText()` 정상 동작
  - `realestate/infrastructure/persistence/RealEstateMarketLatestEntity.java` — `update()` 메소드 호출 흐름으로 전환하거나 dead code 제거

**Approach:**
- root cause: `compareKey`에 referenceText가 포함돼 롤오버 시 lookup miss → existing=null → previous=null. PK 단위(`region+category+source+indicatorCode`)로 키 분리하면 자연 해결.
- IndicatorKey record는 `equals/hashCode` 자동 생성으로 Map 키 안전.
- SaveService.upsert는 source 단위로 좁혀 호출되므로 이미 동일 source 내부에서 indicatorCode 단위 비교만 필요 — IndicatorKey 4-field로 충분.
- 변경 후 `compareKey` 호출처가 남아있는지 grep 필요 (현재 SaveService.filterChanged + mergeLatest 2군데).

**Test scenarios:**
- 명시 요청 시 (Unit 14에서 다룸):
  - referenceText 'A'→'B' 롤오버 시 latest의 previousReferenceText='A' 보존 확인
  - 첫 INSERT 시 previousReferenceText=null
  - referenceText 동일 + value 변경 시 previousReferenceText 미변경

**Verification:**
- compileJava SUCCESSFUL
- grep `compareKey` 호출처 0건
- 운영 dry-run: 두 cycle batch 후 `SELECT region_code, indicator_code, reference_text, previous_reference_text FROM real_estate_market_latest`에서 previous가 직전 reference로 채워짐

---

- [x] **Unit 11: 보안 hardening — 인증키 로그 마스킹 + auth 가드**

**Goal:** P1 #5 + #6 묶음. 외부 API 인증키가 로그/예외 메시지로 누출되지 않게 + anonymous principal에 401 (500 X) 응답.

**Requirements:** R2

**Files:**
- Create:
  - `realestate/infrastructure/source/common/SecretMaskingFilter.java` (또는 `logging/infrastructure/SecretMaskingPatternLayout.java`) — logback PatternLayout 확장으로 `(serviceKey|KEY|apiKey|authkey|crtfc-key)=[^&\s"]+` 마스킹
- Modify:
  - 8 어댑터/Client (`MolitClient`, `RebClient`, `HubAdapter`, `KosisAdapter`, `SubscriptionHomeAdapter`, `HugAdapter`, `SeoulOpenDataAdapter`, `GgDataDreamAdapter`):
    - RestClientException catch 블록에서 `e.getMessage()` 직접 wrap 대신 sanitized 메시지로 감싸기 — 또는 `*ApiException(message, cause)`의 message에서 query string 제거
  - `realestate/infrastructure/scheduler/RealEstateMarketBatchScheduler.java:92` — `log.warn(..., e.getCause())` → `log.warn(..., maskSecrets(rootMessage(e)))`
  - `realestate/presentation/RealEstateFavoriteRegionController.java:51-53` — `currentUserId()` 방어 cast:
    ```java
    private Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken
                || !(auth.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return id;
    }
    ```
  - 또는 `realestate/presentation/RealEstateSecurityContext.java` 헬퍼 신설 (stocknote 패턴 미러)

**Approach:**
- 인증키 마스킹은 logback layout 단계가 가장 광범위. 단 application code에서 직접 `log.warn` 호출 시점에 sanitize도 보강 (defense in depth).
- `currentUserId()` 패턴은 stocknote의 `StockNoteSecurityContext.currentUserId()` 그대로 미러링하여 일관성 확보.

**Test scenarios:**
- 명시 요청 시: WireMock으로 RestClient에 5xx 응답 주입 → 어댑터 예외 메시지에 `serviceKey=` 평문 미포함 검증.
- anonymous request → 401 응답.

**Verification:**
- 부팅 + 의도적 RestClient 실패 시 로그 grep `serviceKey=[A-Za-z0-9]` 0건
- JWT 미부착 GET `/api/realestate/favorites/regions` → HTTP 401

---

- [x] **Unit 12: API contract polish — DELETE body 제거 + 검증 + 케이스 통일**

**Goal:** P1 #7, #8, #15 묶음. shipped 전 contract lock-in 회피.

**Requirements:** R3

**Files:**
- Modify:
  - `realestate/presentation/RealEstateFavoriteRegionController.java`:
    - `unregister(@RequestBody FavoriteRegionRequest)` → `unregister(@RequestParam String regionCode, @RequestParam(required=false) String emdCode)`
    - `register(@RequestBody @Valid FavoriteRegionRequest)` — `@Valid` 추가
  - `realestate/presentation/dto/FavoriteRegionRequest.java` — record fields에 `@NotBlank @Pattern("\\d{5}") String regionCode`, `@Pattern("\\d{8}") String emdCode` (null 허용은 record 자체)
  - `realestate/presentation/RealEstateMarketController.java`:
    - `region_code` → `regionCode`
    - `region_codes` → `regionCodes` + `@Size(min=1, max=10)` (DoS 가드)
  - `static/js/api.js`:
    - `getRealEstateSummary`/`getRealEstateTab`/`getRealEstateComparison`/`removeRealEstateFavoriteRegion`의 query param 케이스 통일
    - DELETE 호출은 body 대신 query string으로 전환

**Approach:**
- house style은 camelCase + `@Valid` 검증. 같은 저장소 `FavoriteIndicatorController`/`StockNoteController` 등 기존 controller 패턴 미러.
- `region_codes` 길이 cap 10 — comparison endpoint 의도(선택 region + 비교 N개) 충족 + DoS 가드.

**Test scenarios:**
- POST `/favorites/regions` body `{regionCode:""}` → 400
- DELETE `/favorites/regions?regionCode=...` → 200/204
- GET `/comparison?regionCodes=...` 11개 전달 → 400

**Verification:**
- compileJava SUCCESSFUL
- 프론트 대시보드 정상 동작 (api.js 동기 변경 확인)
- curl 통합 검증

---

- [x] **Unit 13: 코드 컨벤션 정리 — 메소드 길이 + URL 파싱 공통 유틸**

**Goal:** P1 #9 + #10 묶음. `code-convention.md` 10줄 한도 위반 9곳 정리 + 8 어댑터 URL 파싱 헬퍼 통합.

**Requirements:** R4

**Files:**
- Create:
  - `realestate/infrastructure/source/common/SourceUriBuilder.java` — `UriComponentsBuilder.fromHttpUrl(baseUrl).path(extra).queryParam(...).build()` 헬퍼. 8 어댑터의 `stripScheme/stripContext/extractPort` 모두 대체
- Modify:
  - 8 어댑터/Client — `stripScheme/stripContext/extractPort/extractHost` private 메소드 모두 제거 + `SourceUriBuilder.build(baseUrl, path, params)` 호출로 교체
  - `realestate/infrastructure/scheduler/RealEstateMarketBatchScheduler.runDailyBatch` — `submitAll(regions, adapters, window)` + `awaitFutures(futures)` + `logSummary(...)` helper 추출 (47줄 → 핵심 5-8줄)
  - `realestate/application/RealEstateMarketQueryService` — `getSummary/getTab/getComparison`의 metaMap 순회 패턴을 `buildCardsFor(...)` private helper로 추출 (3 메소드 중복 제거)
  - `realestate/application/RealEstateMarketAggregator.toCard/estimateJeonseRatio` — 빌더 호출이 framework callback 패턴이므로 §예외 적용 + 주석으로 사유 명시 (분리 비용 vs 가독성 비교)
  - `realestate/application/RealEstateFavoriteRegionService.register` — `buildCandidate(userId, region)` + `recoverDuplicate(userId, regionCode, emdCode)` helper 추출

**Approach:**
- `SourceUriBuilder`는 baseUrl이 `https://host:port/context` 형태일 때 query/trailing slash 안전 처리. JDK `URI.create` + Spring `UriComponentsBuilder` 조합.
- 메소드 분리 시 SRP 기준: "조회 vs 계산 vs 변환 vs 부수효과" 경계로.
- IndicatorCard record 빌더처럼 framework callback이 명백한 곳은 분리 강제 X — `code-convention.md §예외 정책` 주석 추가.

**Test scenarios:**
- 명시 요청 시: `SourceUriBuilder` 단위 테스트 — trailing slash, query string, 포트 포함, http vs https 케이스.

**Verification:**
- compileJava SUCCESSFUL
- 8 어댑터에서 `stripScheme`/`stripContext` grep 0건
- 메소드 길이 측정: 모든 application/scheduler 메소드 20줄 이하 (framework callback 외)

---

- [ ] **Unit 14: 단위 테스트 우선순위 5종 (명시 요청 시 진행)**

**Goal:** P1 #14 — 회귀 위험 큰 핵심 로직 5종에 단위 테스트 추가.

**Requirements:** R5

**전제 조건:** 사용자 명시 요청 후 진행 (CLAUDE.md 정책 "테스트는 명시적 요청 시에만 작성").

**Files:**
- Create:
  - `src/test/java/.../realestate/application/RealEstateMarketSaveServiceTest.java` — compareKey 비교 + previousReferenceText 보존 (Unit 10 fix 회귀 가드)
  - `src/test/java/.../realestate/application/RealEstateMarketAggregatorTest.java` — 평균/중위/changeRate/전세가율 추정 + 0-나눗셈 가드
  - `src/test/java/.../realestate/infrastructure/source/molit/dto/MolitApiResponseTest.java` — items.item 단일/배열 정규화 + isSuccess 분기
  - `src/test/java/.../realestate/infrastructure/scheduler/RealEstateMarketBatchSchedulerTest.java` — Timeout/Interrupted/Execution catch 분기 격리 (Mockito + ThreadPoolTaskExecutor stub)
  - `src/test/java/.../realestate/infrastructure/source/common/SourceUriBuilderTest.java` — Unit 13 산출물 회귀 가드

**Approach:**
- Aggregator/MolitApiResponse는 순수 함수형 — Mockito 없이 plain JUnit.
- SaveService는 Mockito + `@MockitoBean` 또는 `@MockBean`으로 Repository stub.
- BatchScheduler는 통합 테스트(@SpringBootTest) 비용 큰 영역이므로 Mockito + 단위 정도로 한정.

**Test scenarios:**
- 본 unit 자체가 테스트 작성. 각 테스트 클래스의 happy/edge/error 시나리오는 테스트 코드에 명시.

**Verification:**
- `./gradlew test` 통과
- jacoco coverage 80%+ 핵심 5 클래스

## Open Questions

### Resolved During Planning

- **단위 테스트 작성 시점**: parent PR 머지 후 후속 PR로 진행. 명시 요청 받기 전까지 보류.
- **plan 분리 vs 통합**: 별도 plan으로 분리 — parent PR 머지 차단하지 않기 위함.
- **branch 전략**: 본 plan은 별도 worktree 권장. Unit 별로 incremental commit.

### Deferred to Implementation

- [Affects R3][Technical] DELETE의 query param 형식 — 단일 `?regionCode=A&emdCode=B`로 충분한가? (예: 한 번에 여러 region 해제 batch가 필요한가? 현재 UI 흐름은 1건씩이므로 단일 형식 OK)
- [Affects R4][Technical] `SourceUriBuilder`가 모든 8 어댑터의 baseUrl 형태(http vs https, 포트 포함, path 있음 vs 없음)를 커버하는지 — 운영 시점에 어댑터별 dry-run으로 확인
- [Affects R2][Needs research] logback `PatternLayout` 확장 vs `MaskingConverter`(Spring Boot 3+) — 어느 쪽이 ECOS/뉴스 도메인의 기존 logging 정책과 충돌 없는지 확인
- [Affects R1][Technical] `IndicatorKey` 도입 시 cache 키도 함께 IndicatorKey로 변경할지 (현재 WarmupListener가 `region::category` 문자열 사용) — Unit 10에서 결정

## Phased Delivery

| Phase | Unit | 우선순위 사유 |
|-------|------|-------------|
| 1 (즉시) | Unit 10 | 데이터 정합성 — previousReferenceText 침묵 회귀 차단 |
| 1 (즉시) | Unit 11 | 보안 — 인증키 로그 누출 + 401 가드 |
| 2 (단순) | Unit 12 | API contract — shipped 전 lock-in 회피 |
| 3 (구조) | Unit 13 | 컨벤션/duplication 정리 |
| 4 (옵션) | Unit 14 | 단위 테스트 — 명시 요청 시 진행 |

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Unit 10에서 IndicatorKey 도입 시 `compareKey` 호출처가 cache key 등에 잠재 의존 | grep 전수 확인 + 변경 후 통합 검증 |
| Unit 11 logback 마스킹이 다른 도메인 로그 형식에 영향 | 도메인 한정 logger pattern으로 격리 가능. 실패 시 application code 단계 sanitize fallback |
| Unit 12 query param 케이스 변경이 이미 shipped 클라이언트에 영향 | parent PR이 아직 머지 전 → contract lock-in 전이라 안전. 머지 후 진행 시 deprecation 경로 필요 |
| Unit 13 메소드 분리가 가독성을 해칠 가능성 | framework callback/builder 패턴은 §예외 적용 후 주석 명시. 무차별 분리 금지 |

## Operations Follow-up (운영 시점 확인 — 본 plan 범위 밖)

운영 dry-run(2026-05-12) 결과를 반영해 상태를 갱신한다.

| 항목 | 상태 | 비고 |
|------|------|------|
| MOLIT 주택유형 확장 (오피스텔/연립다세대/단독다가구/토지) | ⏸ 미정 | parent plan에서 정의됐으나 구현은 아파트만. Entity·API contract 변경 → 별도 brainstorm/plan 필요 |
| HUB endpoint 결정 (raw `getHpBasisOulnInfo` vs KOSIS 대체) | ⏸ 비활성화 | 16개 endpoint 모두 raw microdata. `HUB_API_KEY` 빈값 유지로 자동 skip |
| HUG 분양보증사고 어댑터 재작성 vs 폐기 vs KOSIS 우회 | ⏸ 비활성화 | data.go.kr 링크데이터셋. HUG 자체 OPEN API(www.khug.or.kr) 존재하나 envelope/schema 별도 — `HugAdapter` javadoc 참조 |
| 청약홈 응답 정상 적재 확인 | ✅ 완료 | dry-run에서 56 region × count=3 = 168 row 저장 확인. 응답 필드명 일치 검증됨 |
| 청약홈 추가 operation (오피스텔/공공임대 등 7개) | ⏸ 미정 | 주택유형 확장과 함께 도입 |
| MOLIT WAF 차단 (User-Agent 누락 시 HTTP 400) | ✅ 완료 | `RealEstateRestClientConfig`에 default User-Agent 헤더 적용. 직접 호출 시 정상 응답 확인 |
| MOLIT 일부 region에서 응답 envelope 매핑 실패 | ✅ 완료 | 빈 응답 시 MOLIT이 `items: ""`(빈 문자열) 반환하는 케이스 + resultCode `"000"`(3자리) 호환 처리. `MolitApiResponse.Body.items`를 `Object`로 받고 `rows()`에서 String/Map 분기. dry-run에서 MOLIT 144 row 적재 확인 |
| REB endpoint contextPath 누락 (HTTP 404) | ✅ 완료 | `RebClient`에서 `base.contextPath()` 합쳐서 호출 — `/r-one/openapi/SttsApiTblData.do`로 정정 |
| REB 통계표 ID 매핑 (STATBL_ID/ITM_ID) | ✅ 완료 (2026-05-25, issue #58) | **별도 plan `docs/plans/2026-05-24-001-feat-reb-adapter-redesign-plan.md`로 분리 후 구현 완료**. Q1=B(시군구 모델 유지 + 광역 row 별도 추가, hybrid) + Q2=B1(4개 통계표) + Q4=D1(모든 ITM_ID + 메모리 필터) 결정. `RebStatblProperties` (11 STATBL_ID + DTACYCLE_CD) 강타입 바인딩, `RebMetadataCache` (Caffeine TTL 24h + negative cache 60s), `RebClient` 3-step + 페이징 누적, `RebAdapter` SIDO 전용(`supportsRegion=isSido()`) + 시도명 47-entry 양보적 매핑. 비-REB 어댑터는 `isSigungu()`로 SIDO row wasted submit 차단. `region_code` length 자연 discriminator + `RegionLevel` enum + JPA `@PrePersist`/`@PostLoad` hook으로 drift fail-fast. R-ONE API 명세는 `.claude/analyzes/realestate/reb-r-one-openapi-spec.md` 권위 문서로 캡처 |
| KOSIS endpoint + itmId 매핑 | ✅ 완료 | (1) endpoint: `/openapi/Param/statisticsParameterData.do` (Param path 추가). (2) itmId=`13103871087T1` (미분양현황, DT_MLTM_2082). (3) objL1=ALL/objL2=ALL 한 번 호출 후 응답 C1_NM/C2_NM 매칭. (4) Content-Type이 text/html이어도 body는 JSON이라 `String.class`로 받아 ObjectMapper로 파싱. (5) 시도명 정규화 매핑(서울→서울특별시 등). (6) 시점은 `newEstPrdCnt=3`로 발표 lag 회피. dry-run 결과 56 region × 1 indicator = 56 row 적재 확인 |
| GG_DATA_DREAM 어댑터 정상화 | ✅ 완료 | (1) 보안 차단 자동 해제됨(2026-05-18). (2) plan 추정 path `/AptMaeMul` → 정확한 4개 endpoint로 정정: `/Apttradedelng`, `/Apttradedelngdetail`, `/Coaltnmlpxhoutrade`, `/Snglnsmulthstrade`. (3) SIGUNGU_CODE → SIGUN_CD 파라미터명 정정. (4) DEAL_AMT → DELNG_AMT 응답 필드명 정정. (5) Content-Type이 text/html이어도 body는 JSON이라 String + ObjectMapper로 직접 파싱(KOSIS와 동일 패턴). (6) 4개 endpoint 부분 실패 허용 — 한 endpoint 실패가 다른 호출을 막지 않게 try-catch + continue. dry-run 결과 248 row 적재 확인 |
| SEOUL_OPEN_DATA https 호환성 | ✅ 완료 | https 미지원 확정(TLS protocol version 에러). `application.yml` http로 되돌림. dry-run에서 25 region × count=1 = 25 row 저장 확인 |
| `SecretMasker` path-segment 키 마스킹 보강 | ⏸ 미보강 | 서울 OpenData가 path-segment에 키 노출(`/{KEY}/json/...`). Unit 11의 query-param 정규식만으로 미커버 — http로 복귀하면서 우선순위 ↑ |
| `SecretMasker` 응답 body snippet 포함 (진단성 강화) | ✅ 완료 | 4xx/5xx 응답 시 status·body 300자 snippet을 `sanitize()` 결과 message에 포함 — caused by 로그로 root cause 즉시 식별 가능 |

## Sources & References

- **Origin**: parent plan `docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md`
- **Review 출처**: `/ce:review` 결과 (P0 3건 → parent Unit 9 / P1 잔여 10건 → 본 plan)
- **학습**:
  - `docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md`
  - `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`
  - `docs/solutions/architecture-patterns/global-indicator-history-mirroring.md`
- **Issue**: #41 (parent), 후속 issue 별도 발급 권장