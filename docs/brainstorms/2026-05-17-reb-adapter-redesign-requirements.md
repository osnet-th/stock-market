---
date: 2026-05-17
topic: reb-adapter-redesign
parent_issue: 41
parent_plan: docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md
related_plan: docs/plans/2026-05-10-001-refactor-realestate-review-findings-plan.md
status: draft
---

# REB(한국부동산원 R-ONE) 어댑터 재설계

## Problem Frame

issue #41 부동산 시장 데이터 대시보드 MVP에서 REB 어댑터는 plan 작성 시 추정으로 구현되었다. 운영 dry-run(2026-05-17) 결과 R-ONE Open API의 실제 호출 모델이 본 plan의 어댑터 모델과 **구조적으로 호환되지 않음**이 확인되었다.

본 brainstorm은 REB 어댑터 재설계를 위한 사실/옵션/결정 항목을 정리한다. 본 brainstorm 자체는 결론을 내지 않고, 후속 plan 단계에서 결정에 필요한 정보를 모은다.

## 확인된 사실 (R-ONE API 모델)

### 3-step 호출 모델

R-ONE Open API는 3개의 endpoint로 분리되어 있다:

| Step | Endpoint | 용도 | 응답 |
|---|---|---|---|
| 1 | `/r-one/openapi/SttsApiTbl.do` | 통계표 목록 메타조회 | 전체 738건 통계표 메타정보 (STATBL_ID, STATBL_NM, DTACYCLE_CD 등) |
| 2 | `/r-one/openapi/SttsApiTblItm.do` | STATBL별 항목(분류) 조회 | 통계표별 ITM_ID 목록 (지역분류 등) |
| 3 | `/r-one/openapi/SttsApiTblData.do` | 실제 데이터 조회 | STATBL_ID + ITM_ID + 추가 필수 파라미터로 시계열 데이터 |

### 공통 파라미터 (Step 1·2)

| 변수명 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `KEY` | STRING | 필수 | 인증키 |
| `Type` | STRING | 필수 | xml / json |
| `pIndex` | INTEGER | 필수 | 페이지 |
| `pSize` | INTEGER | 필수 | 페이지당 row 수 (sample key는 5 고정, 정식 100) |
| `STATBL_ID` | STRING | 선택 | 통계표 ID로 필터 |

### Step 3 (SttsApiTblData.do) — 추가 필수 파라미터 미확정

dry-run에서 다음 조합으로 시도했으나 모두 `{"RESULT":{"CODE":"ERROR-300","MESSAGE":"필수 값이 누락되어 있습니다"}}` 응답:

- `STATBL_ID`만 → ERROR-300
- `STATBL_ID + ITM_ID + DTACYCLE_SE + WRTTIME_IDTFR_ID` → ERROR-300

**미확정 사항**: 정확한 필수 파라미터 셋 명세는 R-ONE 회원가입 + Open API 활용신청 승인 후 받는 가이드 PDF에 있을 것으로 추정. 추측 시도는 추가 진행하지 않음 (추측 구현 금지 정책).

## 핵심 통계표 매핑 (738건 중 plan 카테고리 대응)

dry-run에서 `SttsApiTbl.do`로 전체 목록을 받아 plan brainstorm의 T1~T8 카테고리에 대응하는 통계표를 식별:

| plan 카테고리 | STATBL_ID | DTACYCLE | 통계표명 |
|---|---|---|---|
| **T2 매매 가격** (주택종합) | `A_2024_00017` | 월 | (월) 계절조정 매매가격지수_주택종합 |
| **T2 매매 가격** (주택종합, 계절조정 제외) | `A_2024_00016` | 월 | (월) 매매가격지수_주택종합 |
| **T2 매매 가격** (아파트) | `A_2024_00048` | 월 | (월) 계절조정 매매가격지수_아파트 |
| **T2 매매 가격** (연립/다세대) | `A_2024_00083` | 월 | (월) 계절조정 매매가격지수_연립/다세대 |
| **T2 매매 가격** (단독주택) | `A_2024_00117` | 월 | (월) 계절조정 매매가격지수_단독주택 |
| **T3 전세 가격** (주택종합) | `A_2024_00020` | 월 | (월) 계절조정 전세가격지수_주택종합 |
| **T3 전세 가격** (아파트) | `A_2024_00053` | 월 | (월) 계절조정 전세가격지수_아파트 |
| **T3 전세 가격** (연립/다세대) | `A_2024_00088` | 월 | (월) 계절조정 전세가격지수_연립/다세대 |
| **T3 전세 가격** (단독주택) | `A_2024_00122` | 월 | (월) 계절조정 전세가격지수_단독주택 |
| **T3 월세 가격** (아파트) | `T244963131668796` | 월 | (월) 규모별 월세가격지수_아파트 |
| **T3 월세 가격** (연립/다세대) | `T243363131726437` | 월 | (월) 규모별 월세가격지수_연립/다세대 |
| **T1 거래량** (주택매매) | `A_2024_00604` | 월 | (월) 거래규모별 주택매매거래현황 |
| **T1 거래량** (아파트매매) | `A_2024_00612` | 월 | (월) 거래규모별 아파트매매거래현황 |
| **T4 지가변동률** (지역별) | `A_2024_00903` | 월 | (월) 지역별 지가변동률 |
| **T4 지가변동률** (용도지역별) | `A_2024_00007` | 월 | (월) 용도지역별 지가변동률 |
| **오피스텔 매매** (2024~) | `T244523133223976` | 월 | (월) 오피스텔 규모별 매매가격지수(2024년1월~) |
| **오피스텔 전세** (2024~) | 후속 조사 | 월 | (월) 오피스텔 규모별 전세가격지수(2024년1월~) |
| **오피스텔 월세** (2024~) | `T248783133246653` | 월 | (월) 오피스텔 규모별 월세가격지수(2024년1월~) |
| **오피스텔 전월세전환율** (2024~) | `T241163133546529` | 월 | (월) 오피스텔 전월세전환율(2024년1월~) |

> 오피스텔은 시점별로 STATBL_ID가 분리됨 (2018~2020.6 / 2020.7~2023.12 / 2024~). 운영 단계에서 현재 시점 STATBL_ID만 호출하면 됨.

### 미발견 카테고리

- **T2 실거래가격지수** — "실거래" 키워드 매칭 0건. R-ONE이 별도 endpoint로 제공하거나 통계표명이 다를 수 있음. 후속 조사
- **T3 전세가율 / 전월세전환율** (오피스텔 외) — 미발견. 추가 조사 필요

## 구조적 미스매치 (왜 어댑터 전면 재설계가 필요한가)

| 비교 항목 | 현재 plan/어댑터 모델 | R-ONE 실제 모델 |
|---|---|---|
| **호출 단위** | 시군구(`region_code` 5자리) × 카테고리 × 날짜 | STATBL_ID × ITM_ID(지역분류) × 기간 |
| **지역 식별** | 행정구역코드 (예: 11680 강남구) | ITM_ID (예: 500001 전국, 500004 6대광역시 등) |
| **시군구 단위 제공** | 56 region 모두 호출 | **부분 제공** — 전국/수도권/광역시도+일부 시군구만 ITM_ID 존재 |
| **호출 횟수** | region × category 조합 = 약 600+회/일 | STATBL_ID × ITM_ID 조합 = 적음 (광역 단위) |
| **응답 단위** | 1 row per region | 1 row per (STATBL_ID, ITM_ID, 기간) |

→ **본질적 미스매치**: 본 plan은 "시군구별 시계열"을 가정. R-ONE은 "광역 단위 시계열 + 일부 시군구". 시군구 단위 데이터가 광범위 부재.

## 결정해야 할 항목 (후속 plan에서 답해야 함)

### Q1. 지역 단위 모델

R-ONE 한계를 어떻게 수용할지:

| 옵션 | 설명 | 영향 |
|---|---|---|
| **A. 광역시도 단위로 plan 모델 변경** | 시군구가 아닌 시도(서울/경기/...) 단위로 R-ONE 데이터 수집 | Entity·API contract·UI 전반 변경. 사용자 UX 영향(시군구 → 시도) |
| **B. 시군구 모델 유지, REB는 광역만 채움** | 시군구 카드는 MOLIT 실거래만, R-ONE은 시도 단위 별도 카드 | 카드 출처별 분리 표시 필요. UI 복잡도 ↑ |
| **C. ITM_ID 시군구 제공 통계표만 한정** | 시군구 ITM_ID 있는 STATBL만 어댑터 활성화 | 카버리지 ↓ (어떤 통계는 시도까지만) |
| **D. REB 어댑터 폐기** | T2/T3 가격지수는 KOSIS로 우회 | KOSIS 매핑 작업 별도 (KOSIS도 endpoint deprecated 이슈) |

### Q2. 다중 통계표 처리 모델

한 카테고리(예: T2 매매)에 STATBL_ID가 다수 (주택종합/아파트/연립/단독 4개). 어떻게 다룰지:

- **B1. 모든 4개 호출 + 분리 카드 표시** (현재 plan의 주택유형 확장과 같이)
- **B2. 주택종합 1개만 호출** (MVP 축소)
- **B3. 사용자 필터로 선택 호출** (UX 복잡)

### Q3. SttsApiTblData.do 필수 파라미터 확정

dry-run에서 추측 시도 실패. 정확한 명세 확보 경로:

- **C1. R-ONE 회원가입 + Open API 정식 활용신청 → 가이드 PDF**
- **C2. R-ONE 운영팀 문의 (이메일/전화)**
- **C3. 다른 오픈소스 프로젝트에서 호출 예시 발견 시도**

### Q4. ITM_ID 매핑 전략

A_2024_00017 한 통계표만으로도 42개 ITM_ID 존재 (전국/수도권/지방/광역시도/...). 호출 시 모든 ITM_ID 호출할지 일부만 할지:

- **D1. 모든 ITM_ID 호출 + 메모리 필터링** (호출 부하 ↑)
- **D2. 필요한 지역 ITM_ID만 사전 매핑** (매핑 작업 필요)
- **D3. 통계표별 ITM_ID 캐시 + 동적 호출**

## 영향 범위 (재설계 시 변경 영역)

### Domain

- `RealEstateMarketIndicator.regionCode`: 5자리 → 가변 (시도 2자리 or 시군구 5자리)
- 또는 새 필드 `regionLevel` (SIDO/SIGUNGU/NATIONAL) 추가

### Application

- `RealEstateMarketSaveService` — 지역 단위 분기 처리
- `RealEstateMarketAggregator` — 시도/시군구 카드 분리 표시 로직

### Infrastructure

- `RebClient` — 3-step 호출 (목록 → 항목 → 데이터)로 재설계
- `RebAdapter` — STATBL_ID 매핑 + ITM_ID 캐시 + 응답 정규화
- 또는 `RebMetadataCache` (warmup listener에서 통계표/항목 메타 일괄 로드)

### Presentation

- API contract: `region_code` 파라미터 시도/시군구 모두 허용?
- 응답 cards에 "지역단위" 메타 (SIDO vs SIGUNGU) 노출 여부

### Database

- 기존 `real_estate_market_latest` Unique Key: (region_code, category, source, indicator_code)
- 시도+시군구 동시 저장 시 region_code 형식 통일 필요

## 후속 작업

본 brainstorm 이후 진행 순서:

1. **사용자 결정** — Q1~Q4 의사결정
2. **R-ONE 가이드 PDF 확보** (Q3 해소)
3. **별도 plan 작성** — `docs/plans/YYYY-MM-DD-NNN-feat-reb-adapter-redesign-plan.md`
   - 결정된 모델 기반 Implementation Units 정의
   - Entity 변경/마이그레이션 항목 (Approval Gate)
   - API contract 변경 (Approval Gate)
4. **구현 + 운영 dry-run 검증**
5. **plan 2026-05-10-001의 Operations Follow-up에서 REB 항목 ✅ 완료로 갱신**

## Scope Boundaries

- 본 brainstorm은 **결정을 내리지 않는다** — 정보 수집과 옵션 정리만.
- 본 brainstorm은 REB **데이터 수집/매핑** 범위. 같은 R-ONE 출처를 사용하는 다른 도메인(예: 한국부동산원 통계포털) 통합은 범위 밖.
- 통계표 매핑은 plan 단계에서 확정 — 본 문서의 매핑 표는 후보 목록.

## Sources & References

- **운영 dry-run 로그**: 2026-05-17 batch 결과 (`success=522, failure=94, savedRows=361` — REB는 fetch failed 168건)
- **R-ONE API**:
  - `SttsApiTbl.do` (통계표 목록) — 사용자 제공 + dry-run 검증
  - `SttsApiTblItm.do` (항목 조회) — dry-run 검증
  - `SttsApiTblData.do` (데이터 조회) — 필수 파라미터 미확정
- **parent plan**: `docs/plans/2026-05-06-001-feat-real-estate-market-data-plan.md`
- **review findings plan**: `docs/plans/2026-05-10-001-refactor-realestate-review-findings-plan.md` (Operations Follow-up 섹션)
- **issue**: #41
