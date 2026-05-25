---
title: "feat: REB R-ONE 어댑터 광역시도 단위 재설계"
type: feat
status: completed
date: 2026-05-24
completed_at: 2026-05-25
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
- **Q1 = B (재정의)**: 시군구 모델 유지 + REB는 광역(SIDO)만 채움. 기존 56 SIGUNGU row는 그대로, SIDO 17 row를 별도 추가. 도메인 객체에 `RegionLevel` 명시(query 편의용 derived) — Q1.A처럼 단일 모델로 통합하지 않고, Q1.B의 hybrid로 명세 일관성 정리.
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

- R1 (Unit 1, 1.5, 2): 광역시도 region 모델 도입 + 배치 경로 확장 (Q1=B)
- R2 (Unit 3, 4): R-ONE 3-step 호출 모델 + STATBL_ID 11개 매핑 (Q2=B1 + Q3=C1 주채널)
- R3 (Unit 4): RebMetadataCache로 모든 ITM_ID 사전 로딩 + 메모리 필터 (Q4=D1)
- R4 (Unit 5, 6): SaveService/API contract 시도 분기 (Q1=B 영향 + Unit 4 데이터 수집 필요)
- R5 (Unit 7a): Frontend 시도/시군구 카드 섹션 분리 (core). 폴리시(state matrix, favorite auto-include, a11y)는 Unit 7b로 디퍼
- R6 (Unit 8): 기존 plan(2026-05-10-001) Operations Follow-up REB 항목 갱신

## Decisions (brainstorm Q1~Q4)

### Q1=B (재정의): 시군구 모델 유지 + 광역 별도 row 추가 (hybrid)

- `RegionCode`를 가변 길이로 확장 (2자리 시도 / 5자리 시군구) — anchor 명시(`^(\d{2}|\d{5})$`)
- `RealEstateRegion` entity에 `level` 컬럼 추가 (`SIDO` / `SIGUNGU`) — **explicit discriminator**로 사용. SoT는 `region_code` length이며 `@PrePersist`/`@PreUpdate` hook에서 `RegionLevel.fromCode(regionCode)`로 강제 동기화(write path) + `@PostLoad` hook에서 컬럼 값과 fromCode 결과 불일치 시 **graceful auto-correct** (ERROR 로그 + in-memory `level` 덮어쓰기). 이전 fail-fast 정책은 단일 row 손상으로 전체 service 차단 위험이 있어 변경 — drift 영구 복구는 운영자가 SQL UPDATE로 수행. 단, `regionCode` 자체가 비정상 길이(2/5 외)인 경우는 데이터 corruption으로 간주하여 `IllegalStateException` throw 유지. Lombok `@Builder`/`@AllArgsConstructor`는 `level` 인자를 받지 않도록 `@Builder.Default` + `@Setter(AccessLevel.PRIVATE)` 적용
- 기존 sentinel 충돌 회피: `findAllSigunguOnly()` 등 `emdCode=""` 기반 쿼리를 **`level=SIGUNGU` predicate로 전부 재작성** (Unit 1.5에서 통합 처리). SIDO row와 SIGUNGU row 모두 `emd_code=""` sentinel 사용 가능
- `RealEstateMarketIndicator.regionCode`도 가변 형식 수용 (실제 컬럼 `length=8`)
- API contract: `regionCode` 파라미터가 2자리(시도) / 5자리(시군구) 모두 허용. **`level` 명시 파라미터는 도입하지 않음** — length가 deterministic discriminator이므로 redundant
- IndicatorKey PK 그대로 — region_code 컬럼이 2자리/5자리 자연 구분, source 컬럼이 REB/MOLIT/KOSIS/GG 구분
- 기존 56 SIGUNGU row 무변경, SIDO 17 row append (시군구 rollup 없음)

### Q2=B1: 4개 통계표 모두 + 4 카테고리

| 카테고리 | 주택종합 | 아파트 | 연립/다세대 | 단독주택 |
|---|---|---|---|---|
| 매매가격지수 | `A_2024_00017` | `A_2024_00048` | `A_2024_00083` | `A_2024_00117` |
| 전세가격지수 | `A_2024_00020` | `A_2024_00053` | `A_2024_00088` | `A_2024_00122` |
| 거래량 | `A_2024_00604` | `A_2024_00612` | (별도) | (별도) |
| 지가변동률 | `A_2024_00903` | — | — | — |

→ properties `realestate.reb.statbl-ids` 매핑 (구조화된 yml)

### Q3=C1 (주채널) + C2/C3 (보조)

- **C1 (주채널)**: 회원가입 → 활용신청 → 영업일 2~3일 대기 → 가이드 PDF. Unit 4 진입 게이트.
- **C3 (사전 확인, 1h timebox)**: 단일 owner가 GitHub code search (`SttsApiTblData.do` + 필수 파라미터 키워드)로 호출 예시 탐색. 1시간 후 결과 없으면 종료. 발견 시 C1 도착 전 가설 검증용으로만 사용.
- **C2 (보조 ping)**: C1 신청 후 영업일 2일 시점에 운영팀 이메일로 진척 문의(별도 owner 불필요, C1 owner가 처리).
- **Unit 4는 단일 단위**(4a/4b 분리 폐기) — PDF가 critical path임을 인정. C3가 가설을 제공할 수 있으나 실제 구현·머지는 PDF 도착 시점부터 진행. 4a를 별도로 머지하지 않으므로 dead skeleton/feature flag coordination 문제 회피.

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

- [x] **Unit 1: Domain — RegionLevel enum + RegionCode 가변 길이 + JPA derive enforcement**

**Goal:** 광역시도(2자리)와 시군구(5자리) region을 한 모델에 흡수 + level 컬럼 drift 원천 차단.

**Requirements:** R1

**Files:**
- Create:
  - `realestate/domain/model/RegionLevel.java` — `enum RegionLevel { SIDO, SIGUNGU }` + `fromCode(String)`
- Modify:
  - `realestate/domain/model/RegionCode.java` — **가변 길이 validation 완화** (정규식 `^(\d{2}|\d{5})$`) + `isSido()`/`isSigungu()` 메소드 추가 + `sidoCode()` SIDO 분기
  - `realestate/domain/model/RealEstateRegion.java` — `level` 필드 추가 + Lombok `@Builder.Default` + `@Setter(AccessLevel.PRIVATE)`로 외부 변경 차단
  - `realestate/infrastructure/persistence/RealEstateRegionEntity.java` — `level` 컬럼 + `@PrePersist`/`@PreUpdate` hook (`this.level = RegionLevel.fromCode(this.regionCode)`로 write path 강제 동기화) + `@PostLoad` hook (컬럼 값과 fromCode 불일치 시 ERROR 로그 + in-memory auto-correct, log은 row 단위 1회로 throttle. `regionCode` 자체가 비정상 길이일 때만 `IllegalStateException` throw)
  - `realestate/infrastructure/persistence/RealEstateRegionMapper.java` — Entity 생성자에 `level` 인자 미전달 (hook이 derive). Mapper의 toEntity 경로에서 level 생략

**Approach:**
- `RegionLevel.fromCode(String code)` — code.length()로 SIDO/SIGUNGU 판별
- SoT는 `region_code` length. `level` 컬럼은 query/index 편의용 derived persistent 필드이며, 모든 write path(생성자/Mapper/seed/직접 INSERT)는 hook이 동기화. JPA hydration 후 `@PostLoad`가 불일치 검출 → graceful auto-correct (ERROR 로그 1회/row + 메모리 보정). `ApplicationRunner.verifyLevelBackfilled()` pre-check은 마이그레이션 누락만 차단 (level=NULL row 카운트)
- 기존 시군구 호출처는 영향 없음 (5자리 입력 그대로 통과)
- **기존 56 SIGUNGU row backfill 순서** (HOIST): `@PrePersist`/`@PreUpdate`는 새 row에만 동작. 기존 row는 DB 직접 UPDATE 필요. 본 Unit에 신규 마이그레이션 SQL 포함:
  - Create: `src/main/resources/db/migration/add_real_estate_region_level.sql` — `ALTER TABLE real_estate_region ADD COLUMN level VARCHAR(16);` + `UPDATE real_estate_region SET level = CASE WHEN LENGTH(region_code) = 2 THEN 'SIDO' ELSE 'SIGUNGU' END WHERE level IS NULL;` + `ALTER TABLE real_estate_region ALTER COLUMN level SET NOT NULL;`
  - 운영 적용 순서: (1) 마이그레이션 SQL 수동 적용 → (2) 기존 row level 컬럼 backfill 완료 검증(`SELECT level, COUNT(*) FROM real_estate_region GROUP BY level` → SIGUNGU=56) → (3) Entity 변경 + JPA hook 코드 머지/배포 → (4) Unit 1.5의 `level=SIGUNGU` predicate 전환은 backfill 완료 후 진행
  - 순서가 어긋나면 `ApplicationRunner.verifyLevelBackfilled()` pre-check이 명확한 에러 메시지로 부팅 차단 (`@PostLoad`로 부풀려지지 않음)

**Approval Gate:** Entity 생성자/컬럼 변경 + JPA hook 도입 + DB 마이그레이션 SQL → ✋ 사용자 승인 필요

**Verification:**
- compileJava SUCCESSFUL
- 마이그레이션 SQL 적용 후 기존 56 SIGUNGU row의 `level=SIGUNGU` 확인
- `@PostLoad` hook으로 임의 SQL UPDATE(`UPDATE real_estate_region SET level='SIDO' WHERE region_code='11680'`) 시 hydration에서 ERROR 로그(row 단위 1회 throttle) + in-memory level 보정 동작 검증. `region_code` 자체가 비정상 길이("123" 등)일 때만 `IllegalStateException` throw 검증
- 기존 SIGUNGU region 호출 시 `level() == SIGUNGU` 반환
- 시도 코드 "11" 전달 시 `level() == SIDO` 반환
- `RegionCode.of("11")`, `RegionCode.of("11680")` 통과 / `"1"`, `"111"`, `"11abc"` throw
- **호출처 audit** — 다음 패턴 grep 후 회귀 지점 목록화 (코드/SQL 양쪽):
  - `regionCode.substring(0, 2)`, `getRegionCode().substring` — **`RealEstateFavoriteRegionService.java:60` 포함 확인**
  - `length() == 5`, `length()==5`, `length(region_code) = 5`
  - `startsWith(`, `ORDER BY region_code`(lexicographic에서 "11"이 "11000~11999" 사이에 끼어드는 정렬 회귀 확인)
  - `@Column(... length = 5`(다른 컬럼이 SIDO 코드를 reject할 가능성)
  - **`sigungu_name`, `getSigunguName`, `region.sigunguName`** — 빈 문자열을 'not found'로 오인하는 truthy 검사 / 라벨 합성 / ORDER BY / UNIQUE 영향 확인
  - **`emdCode=""`, `EMD_CODE = ''`, `findAllByEmdCode`** — SIGUNGU sentinel과 SIDO sentinel 충돌 audit (BLOCKER, 별도 결정 필요)

---

- [x] **Unit 1.5: Repository sentinel 충돌 해결 + Scheduler SIDO 경로 확장**

**Goal:** 기존 `emd_code=""` 기반 쿼리를 `level` 컬럼 predicate로 재작성 (sentinel 충돌 해결) + 배치 스케줄러가 SIDO region도 어댑터에 전달.

**Requirements:** R1

**Files:**
- Modify:
  - `realestate/infrastructure/persistence/RealEstateRegionRepository.java` (JpaRepository 인터페이스) — 기존 `findAllByEmdCodeOrderByDisplayOrderAsc` 호출처를 `level` predicate 기반 메소드로 대체
  - `realestate/infrastructure/persistence/RealEstateRegionRepositoryImpl.java` — `findAllSigunguOnly()` 구현을 **`findAllByLevelOrderByDisplayOrderAsc(SIGUNGU)`** 로 전환. `findAllSidoOnly()` 신규 추가
  - `realestate/application/RealEstateRegionService.java` — `findRegion(regionCode)` 내부 hardcoded `emdCode=""` 제거. 대신 `findByRegionCode(regionCode)` 단일 키 조회 또는 `level=fromCode(regionCode)` predicate. `listSidoGrouped()` audit
  - `realestate/application/RealEstateFavoriteRegionService.java` — `validateRegionExists()` 동일 audit, `buildCandidate()`의 `regionCode.substring(0, 2)`를 `RegionCode.sidoCode()` 사용으로 전환
  - `realestate/infrastructure/scheduler/RealEstateMarketBatchScheduler.java` — `submitAll()`에서 `findAllSigunguOnly()` + `findAllSidoOnly()` union iterate

**Approach:**
- BLOCKER 해소: 기존 `emd_code=""` predicate는 SIDO row(emd_code="" + level=SIDO)와 SIGUNGU row(emd_code="" + level=SIGUNGU)를 구분 못 함. `level` predicate 사용으로 충돌 완전 차단
- 각 어댑터의 `supportsRegion()` 분기로 SIDO X 비-REB 어댑터 / SIGUNGU X REB 어댑터의 wasted submit 차단 (Unit 4와 함께 구현)
- 호출처 grep audit: `findAllByEmdCode`, `emdCode = ""`, `EMD_CODE = ''`, `findAllSigunguOnly` 전수 검토

**Approval Gate:** Repository/Service 쿼리 시멘틱 변경 (기존 호출자의 결과 row 집합 동일성 보장) → ✋ 사용자 승인 필요

**Verification:**
- 부팅 후 batch 1회 실행 시 SIDO 17개 region이 REB 어댑터에 전달됨
- `findAllSigunguOnly()` 결과 row count = 56 (시드 후), SIDO 0건 (회귀 없음)
- `findRegion("11")` → SIDO row, `findRegion("11680")` → SIGUNGU row, `findRegion("99")` → empty
- `listSidoGrouped()` 응답이 SIDO row를 sigungu로 노출하지 않음
- 비-REB 어댑터 호출 카운터(MOLIT/KOSIS/GG)에 SIDO 호출 0건
- REB 어댑터 호출 카운터에 SIGUNGU 호출 0건

---

- [x] **Unit 2: Database — 시도 region 17개 마이그레이션**

**Goal:** `real_estate_region` 테이블에 17개 시도 row 추가.

**Requirements:** R1

**Files:**
- Modify:
  - `realestate/infrastructure/config/RealEstateMarketMetadataInitializer.java` — 신규 메소드 `seedSidosIfMissing()` 추가 (전체 `count()`와 무관하게 SIDO row 존재 여부만 검사)
  - `src/main/resources/realestate-region-codes.yml` — 17개 시도 entry append

**Approach:**
- 시드 데이터: 17개 시도 (`region_code` 2자리, `sido_code` 동일, `sigungu_name=""` sentinel, `emd_code=""` sentinel, `level=SIDO`)
- `sigungu_name=""` 사용 이유: entity가 `nullable=false`이므로 NULL 불가. `ddl-auto: update`로는 NOT NULL 완화 불가 (학습: `jpa-version-passthrough-ddl-auto-not-null-backfill-2026-04-28.md`). DDL ALTER 회피를 위해 sentinel 채택.
- `seedSidosIfMissing()`은 `existsByLevel(SIDO)` 또는 `count where length(region_code)=2`로 SIDO row 존재 여부만 확인 — 기존 `count()>0` 조기 종료 로직과 독립
- `seedRegionsIfEmpty()`(시군구 56)는 그대로 유지. SIDO 시드는 ApplicationRunner에서 항상 호출되며 idempotent (이미 있으면 skip)
- `display_order`: SIDO는 0~16 범위 등 시군구와 충돌하지 않는 별도 구간 할당 (yml에 명시)

**Approval Gate:** Entity 변경 + 데이터 마이그레이션 → ✋ 사용자 승인 필요

**Verification:**
- 부팅 후 `SELECT level, COUNT(*) FROM real_estate_region GROUP BY level` → SIDO=17, SIGUNGU=56
- 기존 시군구 호출이 영향 없는지 확인 (regression test)
- 재부팅 시 SIDO 중복 insert 없음 (idempotent)

---

- [x] **Unit 3: Properties — STATBL_ID 11개 yml 매핑**

**Goal:** R-ONE 통계표 ID 매핑을 application.yml에 properties로 분리.

**Requirements:** R2

**Files:**
- Create:
  - `realestate/infrastructure/config/RebStatblProperties.java` — 별도 `@ConfigurationProperties(prefix = "realestate.reb.statbl")` 빈
- Modify:
  - `src/main/resources/application.yml` — `realestate.reb.statbl` 매핑 추가 (기존 `realestate.reb.base-url`/`api-key`는 `SourceProps` 그대로 유지)

**Approach:**

```yaml
realestate:
  reb:
    base-url: https://www.reb.or.kr/r-one/openapi
    api-key: ${REB_API_KEY:}
    statbl:
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

- `RebStatblProperties`는 강타입 record (typo 방지). 향후 카테고리/주택유형 확장 시 record 확장.
- 공유 `SourceProps`(`RealEstateMarketProperties.reb`)는 손대지 않음 → 다른 7개 출처 bind 계약 보존.
- `RebClient`/`RebAdapter`만 `RebStatblProperties`를 주입받는다.

**Verification:**
- 부팅 시 `RebStatblProperties` 정상 바인딩 + 11개 필드 모두 non-null
- statbl 키 누락 시 부팅 실패 (record field 필수)
- 기존 7개 출처 SourceProps bind는 무영향 (회귀 검증)

---

- [x] **Unit 4: Infrastructure — RebClient 3-step + Cache + Adapter 재작성**

**Goal:** R-ONE 3-step 호출 모델 + ITM_ID 메모리 캐시 + region 매핑.

**Requirements:** R2, R3

**전제 조건:** R-ONE 회원가입 + 활용신청 승인 완료 + 가이드 PDF 확보 (C1). C3(GitHub 호출 예시 1h timebox)는 사전 가설 검증용으로 PDF 도착 전 단일 owner가 1시간 한정 진행하나, Unit 4 실제 구현·머지는 PDF 도착 시점부터 시작.

**Files:**
- Create:
  - `realestate/infrastructure/source/reb/RebMetadataCache.java` — Caffeine TTL 캐시. `maximumSize`는 cache key 구조에 맞춰 결정 (key=STATBL_ID, value=List<ItmRow>이면 maximumSize≈20 충분; 튜플 key면 실측 후 결정). `warmup()` / `itmIdsFor(statblId)` / `invalidate()` API. 단위 알림 threshold = 캐시 eviction rate > 1/min
  - `realestate/infrastructure/source/reb/dto/SttsApiTblItmRow.java` — 항목 row DTO
  - `realestate/infrastructure/source/reb/dto/SttsApiTblDataRow.java` — 데이터 row DTO
- Modify:
  - `realestate/infrastructure/source/reb/RebClient.java` — 3-step 분리 (`fetchTblList` / `fetchItm` / `fetchData`) + 필수 파라미터 binding (DTACYCLE_SE / WRTTIME_IDTFR_ID / PER_NM 등 PDF 확정값)
  - `realestate/infrastructure/source/reb/RebAdapter.java` — `supportsRegion` override(`level == SIDO`만 true) + STATBL_ID 매핑 + ITM_ID 매핑 + 응답 정규화 + 부분실패 격리 + **`private static final Map<String, String> SIDO_NAME_TO_CODE`** 내부 상수로 시도명("서울"/"서울특별시") → `sidoCode` 매핑(KosisAdapter `KOSIS_SIDO_TO_OFFICIAL` 패턴 동일)
- 비-REB 어댑터: 각 어댑터의 `supportsRegion` override로 SIDO row 차단 (MolitAdapter/KosisAdapter/GgAdapter)

**Approach:**
- `RebMetadataCache.warmup()` 정책:
  - **async-scheduled** at boot(`@PostConstruct` + `CompletableFuture.runAsync`) 또는 batch 첫 호출 시 트리거. 사용자 요청은 warmup 완료 전 `FetchResult.empty()` 반환(Unit 7 Loading state)
  - 통계표별 독립 try/catch — STATBL N개 중 M개 실패해도 N-M개는 정상 사용
  - STATBL 단위 negative cache(60s) — 즉시 재시도 storm 차단
- `RebAdapter.fetch(sidoRegion, ...)`:
  - 캐시된 ITM_ID 중 내부 상수 `SIDO_NAME_TO_CODE`로 `sidoCode` 매치 → 데이터 호출
  - mapping miss → `FetchResult.success(List.of())` + 경고 로그 (어댑터 전체 실패 아님)
  - resultCode "00"/"000" 모두 success 처리(#11)
- 적용 함정 ([학습](../solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md)):
  - User-Agent (#8) — `RealEstateRestClientConfig` 활용
  - Content-Type 무시 (#12) — String + ObjectMapper
  - contextPath (#15) — BaseUri 활용
  - 부분 실패 허용 (#16)
- **Gate 정책**: feature flag 도입하지 않음. 다른 어댑터(MOLIT/KOSIS 등)와 동일 패턴 — `api-key` 유무로만 게이트. api-key 미설정 시 `RebClient`가 `IllegalStateException` → Scheduler 항목 단위 skip. backend 가용성은 `/api/realestate/sources/availability` 응답(Unit 5에서 추가)에 `reb: { available: boolean, reason: string }` 노출 → Unit 7a가 "광역 통계 준비 중" UX 분기에 사용

**Verification:**
- 부팅 후 RebMetadataCache가 11개 STATBL_ID × ITM_ID 캐시 (async warmup 완료 후)
- `fetch(sidoRegion, ...)` 호출 시 응답 정상 + 시도 indicator 적재
- 11개 STATBL 중 1개 실패 시뮬레이션 → 나머지 10개는 정상 적재
- mapping miss 시 어댑터 다른 region 호출은 계속 진행
- 비-REB 어댑터에 SIDO row가 흘러가지 않음 (Unit 1.5 + Unit 4 supportsRegion 양방향)
- api-key 미설정 → RebClient `IllegalStateException` → Scheduler 항목 skip → 카드 빈 상태. `/availability` 응답에 `available=false, reason=api-key-missing`

**Approval Gate:** Adapter 전면 재작성 + RestClient 호출 모델 변경 → ✋ 사용자 승인 필요

---

- [x] **Unit 5: Application — SaveService 시도 분기 + Aggregator 시도 cards + Availability endpoint**

**Goal:** 시도 단위 indicator 적재 + 카드 응답에 시도/시군구 구분 + 소스 가용성 노출.

**Requirements:** R4

**Files:**
- Modify:
  - `realestate/application/RealEstateMarketSaveService.java` — 시도 region indicator도 upsert 처리
  - `realestate/application/RealEstateMarketAggregator.java` — 카드의 `regionLevel`은 `latest.getRegionCode()` length로 derive (single source = region_code length)
  - `realestate/presentation/dto/IndicatorCard.java` — `regionLevel` 필드 + (선택) `isExplicitFavorite: boolean` 필드 (Unit 7b의 favorite auto-include UX와 연계, 7b 디퍼 시 함께 디퍼)
- Create:
  - `realestate/application/SourceAvailabilityService.java` — application layer. API key 유무 + 최근 호출 성공 여부 조합으로 `SourceAvailability` 도메인 응답 생성 (비즈니스 로직)
  - `realestate/presentation/SourceAvailabilityController.java` — `GET /api/realestate/sources/availability` 핸들러. Service에 위임만 (HTTP/DTO 변환 책임). 응답: `{reb: {available, reason}, molit: {...}, kosis: {...}, gg: {...}}`
  - `realestate/presentation/dto/SourceAvailabilityResponse.java` — 응답 DTO

**Approach:**
- IndicatorCard의 `regionLevel`은 Aggregator.toCard 내부에서 `RegionLevel.fromCode(latest.getRegionCode())`로 derive. 메타 저장 없음
- Aggregator에서 시도 indicator는 별도 카드 그룹으로 노출 (Unit 7a 섹션 분리 전제)
- Availability 응답은 Unit 7b의 Source unavailable state 신호 채널

**Approval Gate:** IndicatorCard response shape 변경 + 신규 public endpoint 추가 → ✋ 사용자 승인 필요

---

- [x] **Unit 6: Presentation — API contract region 파라미터 확장**

**Goal:** `regionCode` 파라미터가 시도(2자리) / 시군구(5자리) 모두 허용. length가 deterministic discriminator이므로 별도 `level` 파라미터 도입하지 않음.

**Requirements:** R4

**Files:**
- Modify:
  - `realestate/presentation/RealEstateMarketController.java` — `@Pattern` 정규식 확장: `^(\d{2}|\d{5})$`
  - `realestate/presentation/RealEstateFavoriteRegionController.java` — 동일
  - `realestate/presentation/dto/FavoriteRegionRequest.java` — `regionCode` 정규식 동일

**Approach:**
- 정규식: 기존 `@Pattern(regexp = "\\d{5}")` → `@Pattern(regexp = "^(\\d{2}|\\d{5})$")` (anchor 명시, `"11abc"`/`"11999extra"` 차단)
- region level 추론은 서버 측 `RegionLevel.fromCode(regionCode)` 사용. 클라이언트가 level을 명시할 필요 없음 (length가 자연 discriminator)
- Favorite controller register/delete/list 시그니처는 변경 없음. PK `(user_id, sido_code, region_code, emd_code)`로 SIDO(`region_code=sido_code="11"`)와 SIGUNGU 자연 구분 — 단, `RealEstateFavoriteRegionService.buildCandidate()`의 `regionCode.substring(0, 2)`는 Unit 1.5에서 `RegionCode.sidoCode()` 사용으로 전환 (SIDO 입력에서도 정상 동작)
- 백워드 호환: 기존 5자리 클라이언트는 영향 없음

**Approval Gate:** public API contract 변경 → ✋ 사용자 승인 필요

---

- [x] **Unit 7a: Frontend (R5 core) — 시도/시군구 카드 섹션 분리 + regionLevel 렌더링 + 최소 unavailable 처리**

**Goal:** 대시보드에서 시도 단위 R-ONE 카드와 시군구 카드를 분리 노출 (R5 최소 요건). REB api-key 미설정 또는 최근 호출 실패 시 (`available=false`) 광역 섹션을 빈 카드가 아니라 "준비 중" 상태로 일관 표시.

**Requirements:** R5

**의존성:** Unit 5의 `GET /api/realestate/sources/availability` 엔드포인트 응답 사용 (`reb.available`, `reb.reason`).

**Files:**
- Modify:
  - `src/main/resources/static/partials/realestate.html` — 두 섹션 분리, `<section aria-labelledby>` 랜드마크 (광역 상단, 시군구 하단). 광역 섹션 내 "준비 중" 상태 슬롯 추가
  - `src/main/resources/static/js/components/realestate.js` — `regionLevel` 기반 분기 렌더링 + 초기 진입 시 `/api/realestate/sources/availability` 호출 → `reb.available=false`면 광역 섹션을 카드 그리드 대신 "광역 통계 준비 중" 안내 패널로 대체 (reason에 따라 단일 한 줄 문구)

**Approach:**
- 상단 "한국부동산원 공식 통계 (광역시도)" 섹션, 하단 "시군구 (MOLIT/KOSIS/GG)" 섹션
- `reb.available=true`: 카드 그리드 렌더링 (기존 empty/error UI 패턴 재사용)
- `reb.available=false`: 광역 섹션 영역을 "광역 통계 준비 중" 단일 안내 패널로 대체 (빈 카드 그리드 노출 금지). reason은 짧은 자연어 한 줄 (예: "데이터 소스 점검 중") 정도로 단순 노출. 상세 5-state matrix / 포커스 / aria-live는 Unit 7b 디퍼
- Favorite UI는 기존 시군구 흐름 그대로 (auto-include는 7b 디퍼)

**Verification:**
- 광역 섹션과 시군구 섹션이 시각적으로 분리되어 노출됨
- **api-key 미설정 (`available=false`) → 광역 섹션이 빈 카드가 아닌 "광역 통계 준비 중" 상태로 표시되는지 검증**
- **api-key 설정 + 호출 성공 (`available=true`) → 카드 그리드 정상 렌더링**
- 데스크톱/태블릿/모바일 3 breakpoint 회귀
- 기존 시군구 카드 회귀 없음

---

- [ ] **Unit 7b (DEFERRED to separate plan): Frontend 폴리시 — 상태 매트릭스, favorite auto-include, a11y**

**Status:** 본 plan 범위 밖. **별도 plan으로 분리.** brainstorm round-trip 필요 (favorite "상위 시도 auto-include"는 Q1~Q4에 없는 신규 product 결정).

**디퍼 항목 (참고용):**
- 5-state matrix (Loading/Empty/Partial/Error/Source unavailable)
- Partial+Error 동시 상태 처리
- Favorite removal flow (auto-included parent sido 라이프사이클)
- Empty favorites cold-start UX
- UI mechanism 명시 (토글/아코디언/모달)
- Focus management + aria-live + aria-busy
- 출처 라벨 mapping (REB/MOLIT/KOSIS/GG)
- 우선순위 규칙 + parent sido auto-include cap

**옮길 곳:** 신규 brainstorm + plan 작성 시 본 디퍼 항목 참고. 본 plan 머지 후 별도 issue로 등록 권장.

---

<!-- ARCHIVED: 이전 통합 Unit 7 명세 (7a + 7b 분할 전) — 참고용 보존 -->
<details>
<summary>이전 통합 Unit 7 명세</summary>

**Goal:** 대시보드 UI에서 시도 단위 R-ONE 카드를 **별도 섹션**으로 표시 + 상태/우선순위/접근성 일관 처리.

**Requirements:** R5

**Files:**
- Modify:
  - `src/main/resources/static/partials/realestate.html` — 광역/시군구 두 섹션 분리, `<section aria-labelledby>` 랜드마크
  - `src/main/resources/static/js/components/realestate.js` — `regionLevel` 기반 분기, 우선순위 정렬, 상태 분기 렌더링

**Approach — IA (정보 구조):**
- 페이지 상단 **"한국부동산원 공식 통계 (광역시도)"** 섹션 (출처 약어 'R-ONE' 대신 사용자 언어)
- 페이지 하단 **"시군구 (MOLIT/KOSIS/GG)"** 섹션
- 탭/토글 대신 **항상 양쪽 노출** — 동시 비교가 본 도메인의 주된 사용 패턴

**Approach — 우선순위 규칙:**
- 광역 섹션 첫 화면 노출 순서: (1) 즐겨찾기 시군구의 상위 시도 자동 포함 → (2) "전국" 행 → (3) 나머지 시도는 접힘("전체 시도 보기" 토글)
- 17개 일률 그리드 금지 (AI slop 패턴 회피)
- 카드 헤더: 출처 배지(text label, 색 보조만) + `regionLevel` 라벨

**Approach — 상태 매트릭스:**

| 상태 | 트리거 | UI |
|---|---|---|
| Loading | RebMetadataCache 첫 호출 / 데이터 fetch 중 | 카드 skeleton + "광역 통계 동기화 중" 캡션 |
| Empty (해당 카테고리 SIDO 미지원) | 예: 지가변동률은 광역 데이터만, 단독주택 거래량은 미발표 | 카드 숨기지 않고 흐림 + "이 카테고리는 시군구 단위만 제공" 안내 |
| Partial | 4개 주택유형 중 일부만 도착(Unit 4b 부분실패 정책) | 도착한 것만 노출 + 누락분은 "동기화 대기" 배지 |
| Error (STATBL 단위 실패) | RebClient/Adapter 예외 | 카드 단위 격리 에러 + 재시도 링크. 전체 섹션 차단 금지 |
| Source unavailable | API key 미발급 또는 최근 호출 실패 (`/availability` 응답 `available=false`) | 광역 섹션 전체 접힘 + 설명 + "설정에서 R-ONE 키 등록" 링크 |

**Approach — 반응형/접근성:**
- 데스크톱 3~4컬럼 / 태블릿 2컬럼 / 모바일 1컬럼
- 모바일: 광역 섹션 기본 접힘 + "광역 통계 보기" 토글 (above-the-fold는 즐겨찾기 시군구 우선)
- 출처/regionLevel은 텍스트 라벨 + 색은 보조(WCAG 색 대비 ≥ 4.5:1)
- `<section role="region" aria-labelledby="...">` 두 섹션 명시
- 카드 그룹 내 키보드 탭 순서 자연 흐름 보장
- 출처 약어 toolip/aria-label: "R-ONE = 한국부동산원 공식 통계"

**Approach — Favorite 흐름:**
- 기존 시군구 즐겨찾기 UI 옆에 "시도(광역) 추가" 진입점 추가. 데이터 모델은 Unit 6에서 `level` 파라미터 확장으로 처리
- 시군구 즐겨찾기 추가 시 상위 시도(앞 2자리) 카드를 광역 섹션에 자동 포함 (사용자가 직접 광역 즐겨찾기 안 해도 맥락 노출)

**Verification:**
- 5개 상태별 스크린샷 (Loading/Empty/Partial/Error/Source unavailable)
- 모바일·태블릿·데스크톱 3 breakpoint 회귀
- VoiceOver/NVDA로 섹션 랜드마크 탐색 가능
- 즐겨찾기 혼합(시도+시군구) 저장→조회→삭제 흐름 회귀

</details>

---

- [x] **Unit 8: Operations — plan 2026-05-10-001 Operations Follow-up 갱신**

**Goal:** review findings plan의 Operations Follow-up 섹션에서 REB 항목 ✅로 갱신.

**Requirements:** R6

**Files:**
- Modify:
  - `docs/plans/2026-05-10-001-refactor-realestate-review-findings-plan.md` — REB 항목 상태 변경
  - `docs/brainstorms/2026-05-17-reb-adapter-redesign-requirements.md` — status: completed

## Open Questions

### Resolved During Brainstorm

- **지역 단위 모델**: Q1=B 확정 (시군구 모델 유지 + 광역 row 별도 추가, hybrid). `level` 컬럼은 JPA hook으로 derive 강제
- **다중 통계표 처리**: Q2=B1 확정 (4개 통계표 모두)
- **필수 파라미터 확보**: Q3=C1 주채널 (PDF) + C3 보조 (1h timebox). C2는 C1 owner의 ping
- **ITM_ID 전략**: Q4=D1 확정 (모든 ITM 호출 + 메모리 필터). `maximumSize`는 cache key 구조에 맞춰 결정 + cache/heap 메트릭 + eviction rate 알림
- **API region 파라미터**: 정규식 `^(\d{2}|\d{5})$`만 확장. `level` 명시 파라미터는 도입하지 않음 (length가 deterministic discriminator)
- **Unit 4 분할 폐기**: 4a/4b 통합 단일 Unit으로 회귀. PDF가 critical path임을 인정, dead skeleton/feature flag coordination 비용 회피
- **Sentinel 충돌 해결**: `level=SIGUNGU`/`level=SIDO` predicate로 기존 `emd_code=""` 쿼리 전부 재작성 (Unit 1.5)
- **`RealEstateMarketIndicator.regionCode` 컬럼 길이**: 실제 컬럼은 `length=8` → 변경 불필요
- **시도/시군구 카드 시각적 분리**: 별도 섹션 (상단 광역, 하단 시군구). 모바일은 광역 섹션 기본 접힘 (Unit 7a IA)
- **Unit 7 분할**: 7a(R5 core: 섹션 분리)는 본 plan, 7b(state matrix/favorite auto-include/a11y)는 디퍼 — favorite auto-include는 brainstorm 재방문 필요
- **RebRegionNameResolver 별도 클래스 폐기**: RebAdapter 내부 `private static final Map`으로 inline (KosisAdapter 동일 패턴)
- **Feature flag 도입 폐기**: 다른 어댑터(MOLIT/KOSIS 등)와 동일 패턴으로 `api-key` 유무만 게이트. api-key 미설정 시 `IllegalStateException` → Scheduler 항목 skip. Unit 7a의 "광역 통계 준비 중" UX는 `/api/realestate/sources/availability` 응답이 `api-key` 유무 + 최근 호출 성공 여부 기반으로 판단

### Deferred to Implementation

- [Affects R2][Needs PDF] `SttsApiTblData.do` 필수 파라미터의 정확한 schema — Unit 4 진행 시 확정
- [Affects R3][Technical] RebMetadataCache TTL — 24h vs 더 긴 주기(1주/1개월). 운영 알림 + 수동 invalidate API로 stale window 보완. Unit 4 진입 시 확정
- [Affects R3][Technical] RebMetadataCache key 구조 — STATBL_ID → List<ItmRow> (entry count ≈ 11) vs (STATBL_ID, ITM_ID) tuple (수만 entries). 후자면 maximumSize 실측 필요. Unit 4 진입 시 PDF 기반 확정
- [Affects R3][Technical] yml `loadRegions()`의 `display_order` 처리 — 현재 sequence 자동 부여. SIDO 17개 추가 시 sequence 충돌 방지 위해 yml에 명시적 `display-order` 필드 도입 vs 전체 재배열 중 택일. Unit 2 진입 시 확정

## Phased Delivery

| Phase | Unit | 의존성 | 비고 |
|-------|------|------|------|
| 1 (즉시) | Unit 1 | — | Domain 모델 확장 + RegionCode 검증 완화 + JPA derive hook + level 컬럼 backfill SQL |
| 1 (즉시) | Unit 1.5 | Unit 1 | sentinel 충돌 해결 (`level` predicate로 쿼리 재작성) + Scheduler 양방향 iterate |
| 1 (즉시) | Unit 2 | Unit 1 | 시도 region 17개 마이그레이션 (Initializer `seedSidosIfMissing()`) |
| 1 (즉시) | Unit 3 | — | yml properties 매핑 — 별도 `RebStatblProperties` 빈 (병렬 가능) |
| 2 (PDF 후) | Unit 4 | Unit 1-3 + R-ONE PDF | 어댑터 통합 (3-step + Cache + Adapter + supportsRegion 양방향) |
| 2 (PDF 후) | Unit 5 | Unit 4 | Application 분기 + `/availability` endpoint |
| 2 (PDF 후) | Unit 6 | Unit 5 | API contract (정규식 확장만, level 파라미터 없음) |
| 3 (UX) | Unit 7a | Unit 5-6 | Frontend core: 섹션 분리 + regionLevel 렌더링 |
| — (디퍼) | Unit 7b | — | 별도 plan으로 분리. state matrix / favorite auto-include / a11y 폴리시 |
| 4 (마무리) | Unit 8 | 모든 Unit | docs 갱신 |

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| R-ONE PDF 승인 지연(영업일 2~3일) | Unit 1~3은 PDF 없이 진행. PDF 신청과 함께 C3(GitHub 호출 예시) 1시간 timebox 사전 가설 검증. PDF 도착 시점부터 Unit 4 시작 |
| `emd_code=""` sentinel이 SIDO와 SIGUNGU 양쪽에서 사용되어 기존 쿼리(findAllSigunguOnly 등)가 SIDO row를 잘못 반환 | Unit 1.5에서 `level=SIGUNGU`/`level=SIDO` predicate로 모든 호출처 재작성. grep audit로 사용처 전수 검증 |
| RegionLevel 컬럼과 region_code length 불일치(drift) | `@PrePersist`/`@PreUpdate` hook으로 write path 강제 동기화 + `@PostLoad` hook은 graceful (ERROR 로그 + in-memory auto-correct, row 단위 1회 throttle). 영구 복구는 운영자 SQL UPDATE |
| 기존 SIGUNGU 56 row에 level 컬럼이 backfill되지 않은 채 Entity 머지 시 `@PostLoad` 부팅 즉시 fail | Unit 1 신규 마이그레이션 SQL(`add_real_estate_region_level.sql`)을 코드 머지 전 수동 적용. 적용 순서를 Approval Gate에 명시 |
| 시도 region 추가가 기존 시군구 호출에 영향 | `seedSidosIfMissing()` SIDO-only check + regression test로 검증. `sigungu_name=""` sentinel로 NOT NULL 위반 회피 |
| 기존 `RegionCode.of()` 5자리 invariant가 SIDO에서 throw | Unit 1에서 검증 완화 (`^(\d{2}|\d{5})$` anchor) + `sidoCode()` SIDO 분기 — 호출처 전수 grep 후 확정 |
| 비-REB 어댑터에 SIDO row가 흘러가 wasted submit | Unit 1.5 + Unit 4의 `supportsRegion()` 양방향 분기로 차단 |
| API contract 변경이 기존 클라이언트 깨트림 | 정규식 확장(`\d{2}|\d{5}`)은 기존 5자리 입력 그대로 수용 — 백워드 호환 |
| RebMetadataCache 부팅 부하 | `@PostConstruct` 대신 lazy(첫 호출 시) + Caffeine TTL — 부팅은 빠르게 |
| UI 변경이 다른 도메인 partials에 회귀 | partial 단위 분리 + Stimulus/Alpine 격리 |
| RebMetadataCache OOM (ITM_ID 1000+ 시나리오) | Caffeine `maximumSize` 명시 + cache size/heap usage 메트릭 노출 + 알림 임계값 정의 (Unit 4b) |

## Sources & References

- **brainstorm**: `docs/brainstorms/2026-05-17-reb-adapter-redesign-requirements.md`
- **parent review plan**: `docs/plans/2026-05-10-001-refactor-realestate-review-findings-plan.md` (Operations Follow-up REB 항목)
- **parent plan**: `docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md` (T2/T3/T4 카테고리)
- **R-ONE Open API 명세 (Unit 4 권위)**: `.claude/analyzes/realestate/reb-r-one-openapi-spec.md` — 3개 엔드포인트(SttsApiTbl/SttsApiTblItm/SttsApiTblData) 요청/응답 schema + resultCode 매핑
- **학습**:
  - `docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md` (#8~#17 운영 함정)
- **issue**: #58 (본 plan), #41 (parent)
- **머지된 PR**: #57 (review findings 정리)