---
title: "feat: REB R-ONE 어댑터 광역시도 단위 재설계"
type: feat
status: active
date: 2026-05-24
origin: docs/brainstorms/2026-05-17-reb-adapter-redesign-requirements.md
parent_review: docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md
issue: 58
parent_issue: 41
branch: feat/issue-58-reb-adapter-redesign
---

# feat: REB R-ONE 어댑터 광역시도 단위 재설계

## Overview

운영 dry-run(2026-05-17)에서 R-ONE Open API가 본 도메인 모델(시군구 단위)과 구조적으로 호환되지 않음이 확인됐다. R-ONE은 시군구 ITM_ID를 부분만 제공하고, 통계표 X 항목(ITM) X 기간 단위로 호출 모델이 별개다. 본 plan은 REB 어댑터를 **광역시도 단위 + 4개 주택유형 통계표 통합**으로 전면 재설계한다.

brainstorm Q1~Q4 결정사항을 충실히 반영:
- **Q1 = A**: 광역시도 단위로 plan 모델 변경
- **Q2 = B1**: 4개 통계표 모두 (주택종합/아파트/연립/단독)
- **Q3 = C1**: R-ONE 회원가입 + 활용신청 → 가이드 PDF
- **Q4 = D1**: 모든 ITM_ID 호출 + 메모리 필터

## Problem Frame

기존 plan(2026-05-06-001)은 모든 데이터 출처가 시군구 단위(5자리 행정구역코드)로 호출 가능하다고 가정. 그러나 R-ONE은:

1. **3-step 호출 모델**: `SttsApiTbl.do`(목록) → `SttsApiTblItm.do`(항목) → `SttsApiTblData.do`(데이터)
2. **ITM_ID 단위 분류**: 전국 / 수도권 / 지방 / 광역시도 위주. 시군구 ITM_ID는 일부 통계표만 부분 제공
3. **단일 STATBL_ID 한정 시 주택유형 누락**: 주택종합/아파트/연립/단독이 별개 STATBL_ID

따라서 REB는 다른 출처(MOLIT/KOSIS/GG)와 동일한 시군구 모델로 흡수 불가. plan 모델을 광역시도 단위로 확장하고 REB는 광역만 채우는 방식으로 처리한다.

## Requirements Trace

각 Unit은 brainstorm 결정과 1:1 매핑:

- R1 (Unit 1-2): 광역시도 region 모델 도입 (Q1=A)
- R2 (Unit 3-4): R-ONE 3-step 호출 모델 + STATBL_ID 16개 매핑 (Q2=B1 + Q3=C1)
- R3 (Unit 4): RebMetadataCache로 모든 ITM_ID 사전 로딩 + 메모리 필터 (Q4=D1)
- R4 (Unit 5-6): SaveService/API contract 시도 분기 (Q1=A 영향)
- R5 (Unit 7): Frontend 시도 단위 카드 분리 노출
- R6 (Unit 8): 기존 plan(2026-05-10-001) Operations Follow-up REB 항목 갱신

## Decisions (brainstorm Q1~Q4)

### Q1=A: 광역시도 단위로 plan 모델 변경

- `RegionCode`를 가변 길이로 확장 (2자리 시도 / 5자리 시군구)
- `RealEstateRegion` entity에 `level` 컬럼 추가 (`SIDO` / `SIGUNGU`)
- `RealEstateMarketIndicator.regionCode`도 가변 형식 수용
- API contract: `regionCode` 파라미터가 2자리(시도) / 5자리(시군구) 모두 허용

### Q2=B1: 4개 통계표 모두 + 4 카테고리

| 카테고리 | 주택종합 | 아파트 | 연립/다세대 | 단독주택 |
|---|---|---|---|---|
| 매매가격지수 | `A_2024_00017` | `A_2024_00048` | `A_2024_00083` | `A_2024_00117` |
| 전세가격지수 | `A_2024_00020` | `A_2024_00053` | `A_2024_00088` | `A_2024_00122` |
| 거래량 | `A_2024_00604` | `A_2024_00612` | (별도) | (별도) |
| 지가변동률 | `A_2024_00903` | — | — | — |

→ properties `realestate.reb.statbl-ids` 매핑 (구조화된 yml)

### Q3=C1: R-ONE 가이드 PDF 확보

- 회원가입 → 활용신청 → 영업일 2~3일 대기
- PDF에서 `SttsApiTblData.do` 필수 파라미터 전체 확보 (DTACYCLE_SE, WRTTIME_IDTFR_ID, PER_NM 등)
- PDF 없이는 Unit 4(Infrastructure) 진행 불가 — Unit 1~3은 PDF 대기 동안 진행 가능

### Q4=D1: 모든 ITM_ID 호출 + 메모리 필터

- `RebMetadataCache` 컴포넌트 신설 (Spring `@Component`)
- 부팅 시 또는 첫 호출 시 `SttsApiTblItm.do`로 STATBL_ID별 ITM_ID 전체 캐시
- 데이터 호출(`SttsApiTblData.do`)은 ITM_ID 명시 없이 받고 메모리에서 region 필터
- TTL: 24시간 (KOSIS와 동일 패턴)
- 호출 부하: 통계표당 1회/일

## Scope Boundaries

- 본 plan은 **REB 어댑터 재설계** 한정. MOLIT/KOSIS/GG 등 다른 출처는 본 plan 범위 밖
- **MOLIT 주택유형 확장**(오피스텔/연립/단독)은 별도 plan — 본 plan에서는 region 모델만 변경, MOLIT은 기존 아파트 그대로
- **HUG/HUB 폐기 결정**은 별도 plan — 본 plan은 비활성화 유지
- **시군구 region 데이터 마이그레이션**은 본 plan에서 변경 없음 — 기존 56 region 그대로 유지하고 시도 region 17개를 추가
- **광역시도 카드 UI 표시**는 시군구 카드와 시각적으로 구분 (출처 표기 명시)

## Implementation Units

- [ ] **Unit 1: Domain — RegionLevel enum + RegionCode 가변 길이**

**Goal:** 광역시도(2자리)와 시군구(5자리) region을 한 모델에 흡수.

**Requirements:** R1

**Files:**
- Create:
  - `realestate/domain/model/RegionLevel.java` — `enum RegionLevel { SIDO, SIGUNGU }`
- Modify:
  - `realestate/domain/model/RegionCode.java` — `isSido()`, `isSigungu()` 메소드 + 가변 길이 validation
  - `realestate/domain/model/RealEstateRegion.java` — `level` 필드 추가
- Update:
  - `realestate/infrastructure/persistence/RealEstateRegionEntity.java` — `level` 컬럼

**Approach:**
- `RegionLevel.fromCode(String code)` — code.length()로 SIDO/SIGUNGU 판별
- `RegionCode.value()` 반환은 그대로, level 메소드만 추가
- 기존 시군구 호출처는 영향 없음 (메소드 추가만)

**Verification:**
- compileJava SUCCESSFUL
- 기존 SIGUNGU region 호출 시 `level() == SIGUNGU` 반환
- 시도 코드 "11" 전달 시 `level() == SIDO` 반환

---

- [ ] **Unit 2: Database — 시도 region 17개 마이그레이션**

**Goal:** `real_estate_region` 테이블에 17개 시도 row 추가.

**Requirements:** R1

**Files:**
- Modify:
  - `realestate/infrastructure/persistence/RealEstateRegionApplicationRunner.java` — 시도 17개 시드 데이터 적재
- 또는 Create:
  - `realestate/infrastructure/persistence/RealEstateSidoSeeder.java` (분리)

**Approach:**
- 시드 데이터: 17개 시도 (`region_code` 2자리, `sido_code` 동일, `sigungu_name` null, `level` = SIDO)
- 기존 시군구 데이터(emd_code=""인 56 row)는 그대로 유지
- ApplicationRunner는 idempotent — 이미 적재된 시도는 skip

**Approval Gate:** Entity 변경 + 데이터 마이그레이션 → ✋ 사용자 승인 필요

**Verification:**
- 부팅 후 `SELECT level, COUNT(*) FROM real_estate_region GROUP BY level` → SIDO=17, SIGUNGU=56
- 기존 시군구 호출이 영향 없는지 확인 (regression test)

---

- [ ] **Unit 3: Properties — STATBL_ID 16개 yml 매핑**

**Goal:** R-ONE 통계표 ID 매핑을 application.yml에 properties로 분리.

**Requirements:** R2

**Files:**
- Modify:
  - `realestate/infrastructure/config/RealEstateMarketProperties.java` — `RebProps`에 `statblIds` 필드 추가
  - `src/main/resources/application.yml` — `realestate.reb.statbl-ids` 매핑

**Approach:**

```yaml
realestate:
  reb:
    base-url: https://www.reb.or.kr/r-one/openapi
    api-key: ${REB_API_KEY:}
    statbl-ids:
      sale-price-total: A_2024_00017      # 매매가격지수 주택종합
      sale-price-apt: A_2024_00048        # 매매가격지수 아파트
      sale-price-rowhouse: A_2024_00083   # 매매가격지수 연립/다세대
      sale-price-singlehouse: A_2024_00117 # 매매가격지수 단독주택
      rent-price-total: A_2024_00020      # 전세가격지수 주택종합
      rent-price-apt: A_2024_00053
      rent-price-rowhouse: A_2024_00088
      rent-price-singlehouse: A_2024_00122
      trade-volume-total: A_2024_00604
      trade-volume-apt: A_2024_00612
      land-price-change: A_2024_00903
```

- `RebProps.statblIds`는 `Map<String, String>` 또는 강타입 record. 강타입 권장 (typo 방지).

**Verification:**
- 부팅 시 `RealEstateMarketProperties.reb.statblIds` 정상 바인딩
- statbl_id 누락 시 부팅 실패 (validation)

---

- [ ] **Unit 4: Infrastructure (R-ONE PDF 후) — RebClient 3-step + Cache + Adapter 재작성**

**Goal:** R-ONE 3-step 호출 모델 + ITM_ID 메모리 캐시 + region 매핑.

**Requirements:** R2, R3

**전제 조건:** R-ONE 회원가입 + 활용신청 승인 완료 + 가이드 PDF 확보.

**Files:**
- Create:
  - `realestate/infrastructure/source/reb/RebMetadataCache.java` — ITM_ID 캐시 (Caffeine TTL 24h)
  - `realestate/infrastructure/source/reb/dto/SttsApiTblItmRow.java` — 항목 row DTO
  - `realestate/infrastructure/source/reb/dto/SttsApiTblDataRow.java` — 데이터 row DTO
- Modify:
  - `realestate/infrastructure/source/reb/RebClient.java` — 3-step 분리 (fetchTblList / fetchItm / fetchData)
  - `realestate/infrastructure/source/reb/RebAdapter.java` — 시도 region 호출 + STATBL_ID 매핑 + ITM_ID 매핑 + 응답 정규화

**Approach:**
- RebMetadataCache.warmup() — Spring `@PostConstruct` 또는 첫 호출 시 STATBL별 ITM_ID 로드
- ITM_ID → 시도명("서울"/"경기"...) 매핑 + region.sidoCode 매핑
- 적용 함정 ([학습](../solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md)):
  - User-Agent (#8) — RealEstateRestClientConfig 활용
  - Content-Type 무시 (#12) — String + ObjectMapper
  - contextPath (#15) — BaseUri 활용
  - resultCode "00"/"000" (#11) — 호환 처리
  - 부분 실패 허용 (#16) — 통계표 한 개 실패가 다른 통계표 호출 막지 않게

**Verification:**
- 부팅 후 RebMetadataCache가 11개 STATBL_ID × ITM_ID 캐시
- `fetch(sidoRegion, ...)` 호출 시 응답 정상 + 시도 indicator 적재
- 기존 시군구 region 호출은 RebAdapter.supportsRegion()=false로 skip

**Approval Gate:** Adapter 전면 재작성 + RestClient 호출 모델 변경 → ✋ 사용자 승인 필요

---

- [ ] **Unit 5: Application — SaveService 시도 분기 + Aggregator 시도 cards**

**Goal:** 시도 단위 indicator 적재 + 카드 응답에 시도/시군구 구분.

**Requirements:** R4

**Files:**
- Modify:
  - `realestate/application/RealEstateMarketSaveService.java` — 시도 region indicator도 upsert 처리
  - `realestate/application/RealEstateMarketAggregator.java` — 카드에 `regionLevel` 메타 노출
  - `realestate/presentation/dto/IndicatorCard.java` — `regionLevel` 필드 추가

**Approach:**
- 기존 `IndicatorKey` PK는 그대로 (region_code, category, source, indicator_code). 시도/시군구는 region_code 길이로 자연 구분
- Aggregator에서 시도 indicator는 별도 카드 그룹으로 노출 (예: "전국/광역" 섹션)

---

- [ ] **Unit 6: Presentation — API contract region 파라미터 확장**

**Goal:** `regionCode` 파라미터가 시도(2자리) / 시군구(5자리) 모두 허용.

**Requirements:** R4

**Files:**
- Modify:
  - `realestate/presentation/RealEstateMarketController.java` — `@Pattern` 정규식 확장: `\d{2}|\d{5}`
  - `realestate/presentation/dto/FavoriteRegionRequest.java` — 동일

**Approach:**
- 기존 `@Pattern(regexp = "\\d{5}")` → `@Pattern(regexp = "\\d{2}|\\d{5}")` (2자리 또는 5자리)
- 백워드 호환: 기존 클라이언트의 5자리 호출은 영향 없음

**Approval Gate:** public API contract 변경 → ✋ 사용자 승인 필요

---

- [ ] **Unit 7: Frontend — 시도 단위 카드 분리 노출**

**Goal:** 대시보드 UI에서 시도 단위 R-ONE 카드를 별도 섹션으로 표시.

**Requirements:** R5

**Files:**
- Modify:
  - `src/main/resources/static/partials/realestate.html` — 시도 cards 섹션 추가
  - `src/main/resources/static/js/components/realestate.js` — `regionLevel` 기반 분기 렌더링

**Approach:**
- 카드 그룹: "광역 (R-ONE 통계)" — 시도 17개 카드 또는 사용자 시·도 선택
- 시군구 카드 위 또는 별도 탭으로 노출
- 데이터 없음 표시 (시도 미지원 통계 = R-ONE 외 출처)

---

- [ ] **Unit 8: Operations — plan 2026-05-10-001 Operations Follow-up 갱신**

**Goal:** review findings plan의 Operations Follow-up 섹션에서 REB 항목 ✅로 갱신.

**Requirements:** R6

**Files:**
- Modify:
  - `docs/plans/2026-05-10-001-refactor-realestate-review-findings-plan.md` — REB 항목 상태 변경
  - `docs/brainstorms/2026-05-17-reb-adapter-redesign-requirements.md` — status: completed

## Open Questions

### Resolved During Brainstorm

- **지역 단위 모델**: Q1=A 확정 (광역시도 단위로 plan 변경)
- **다중 통계표 처리**: Q2=B1 확정 (4개 통계표 모두)
- **필수 파라미터 확보**: Q3=C1 확정 (가이드 PDF)
- **ITM_ID 전략**: Q4=D1 확정 (모든 ITM 호출 + 메모리 필터)

### Deferred to Implementation

- [Affects R2][Needs PDF] `SttsApiTblData.do` 필수 파라미터의 정확한 schema — PDF 받기 전까지 Unit 4 진행 불가
- [Affects R3][Technical] RebMetadataCache TTL은 24h이 적정한가? 통계표 메타는 1년 단위 변경이라 더 길어도 OK
- [Affects R4][UX] 시도 카드와 시군구 카드의 시각적 분리 — 별도 섹션 / 탭 / 토글 중 어느 게 직관적인지 UI 설계 시 결정
- [Affects R1][Technical] `RealEstateMarketIndicator.regionCode` 길이 컬럼이 5인데 SIDO는 2자리. DB 컬럼 길이 조정 필요? — VARCHAR(5)는 2자리도 수용 가능, 변경 불필요

## Phased Delivery

| Phase | Unit | 의존성 | 비고 |
|-------|------|------|------|
| 1 (즉시) | Unit 1 | — | Domain 모델 확장 |
| 1 (즉시) | Unit 2 | Unit 1 | 시도 region 17개 마이그레이션 |
| 1 (즉시) | Unit 3 | — | yml properties 매핑 (병렬 가능) |
| 2 (PDF 후) | Unit 4 | Unit 1-3 + R-ONE PDF | 핵심 어댑터 재작성 |
| 2 (PDF 후) | Unit 5 | Unit 4 | Application 분기 |
| 2 (PDF 후) | Unit 6 | Unit 5 | API contract |
| 3 (UX) | Unit 7 | Unit 5-6 | Frontend |
| 4 (마무리) | Unit 8 | 모든 Unit | docs 갱신 |

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| R-ONE PDF 승인 지연(영업일 2~3일) | Unit 1~3은 PDF 없이 진행 가능. Unit 4부터 PDF 필수 |
| 시도 region 추가가 기존 시군구 호출에 영향 | ApplicationRunner idempotent + regression test로 검증 |
| API contract 변경이 기존 클라이언트 깨트림 | 정규식 확장(`\d{2}|\d{5}`)은 기존 5자리 입력 그대로 수용 — 백워드 호환 |
| RebMetadataCache 부팅 부하 | `@PostConstruct` 대신 lazy(첫 호출 시) + Caffeine TTL — 부팅은 빠르게 |
| UI 변경이 다른 도메인 partials에 회귀 | partial 단위 분리 + Stimulus/Alpine 격리 |

## Sources & References

- **brainstorm**: `docs/brainstorms/2026-05-17-reb-adapter-redesign-requirements.md`
- **parent review plan**: `docs/plans/2026-05-10-001-refactor-realestate-review-findings-plan.md` (Operations Follow-up REB 항목)
- **parent plan**: `docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md` (T2/T3/T4 카테고리)
- **학습**:
  - `docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md` (#8~#17 운영 함정)
- **issue**: #58 (본 plan), #41 (parent)
- **머지된 PR**: #57 (review findings 정리)