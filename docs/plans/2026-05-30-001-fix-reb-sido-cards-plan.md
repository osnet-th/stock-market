---
title: REB 광역(SIDO) 카드 미노출 정정 — 구현 plan
date: 2026-05-30
status: completed
brainstorm: docs/brainstorms/2026-05-30-001-reb-sido-cards-not-shown.md
solution: docs/solutions/ui-bugs/reb-sido-cards-not-shown-2026-05-30.md
related: "이슈 #58, PR #70 머지 후 후속 fix"
---

# REB 광역(SIDO) 카드 미노출 정정 plan

## Goal

PR #70 머지 후 REB 카드가 화면 광역 섹션에 노출되지 않는 결함 두 개를 정정. 구체 원인과 정정 방향은 브레인스토밍 문서 참조.

## Scope Boundaries

포함:
- metadata indicator-code ↔ adapter 저장 코드 일치
- 시군구 진입 시 상위 SIDO 카드 attach
- 운영 점검 중 발견된 RebClient envelope 보정 동반

비포함:
- 아파트/연립/단독 STATBL 활성화 (Q1=B 전환)
- 거래량 주택종합 metadata 추가
- ACTUAL_TRANSACTION_PRICE_INDEX / MONTHLY_RENT_PRICE_INDEX STATBL 보강
- OFFICIAL_STATS count=1/region 보강
- A_2024_00903 fetchAllPages MAX_PAGES=20 truncation 개선
- 배치 elapsed 3분+ 최적화

## Implementation Units

### Unit 1 — STATBL entry에 indicator-code 필드 추가
- Files:
  - `src/main/java/com/thlee/stock/market/stockmarket/realestate/infrastructure/config/RebStatblProperties.java`
  - `src/main/resources/application.yml`
- Approach: `Entry` 클래스에 `indicatorCode` nullable 필드 추가. yml의 4개 대표 entry(sale-price-total / rent-price-total / trade-volume-apt / land-price-change)에만 indicator-code 명시.
- Verification: 부팅 성공, `RebStatblProperties` 검증 통과.

### Unit 2 — RebAdapter 저장 코드 의미 코드로 변경 + 미매핑 skip
- Files:
  - `src/main/java/com/thlee/stock/market/stockmarket/realestate/infrastructure/source/reb/RebAdapter.java`
- Approach: `StatblTarget` record에 `indicatorCode` 추가. `of()` 가 entry.indicatorCode null/blank이면 null 반환. `buildIndicator`는 `target.indicatorCode()` 사용.
- Verification: 배치 후 `real_estate_market_indicator.source='REB'` 행이 `SALE_PRICE_INDEX/JEONSE_PRICE_INDEX/APT_TRANSACTION_COUNT/LAND_PRICE_CHANGE_RATE` 4종으로 저장.

### Unit 3 — QueryService에서 시군구→상위 SIDO attach
- Files:
  - `src/main/java/com/thlee/stock/market/stockmarket/realestate/application/RealEstateMarketQueryService.java`
- Approach: `getTab(regionCode, ...)` 진입 시 `regionCode.length()==5`이면 앞 2자리로 추가 `buildCardsFor` 호출 → 결과 카드를 cards에 합침.
- Verification: `tabs/PRICE_INDEX?regionCode=11110` 응답 cards에 MOLIT 시군구 카드 + REB SIDO 카드 둘 다 포함.

### Unit 4 — RebClient envelope 보정 (운영 점검 후속)
- Files:
  - `src/main/java/com/thlee/stock/market/stockmarket/realestate/infrastructure/source/reb/RebClient.java`
- Approach: PATH_ITM/PATH_DATA에 `.do` suffix, `INFO-000` 성공 코드 + 레거시 fallback, 중첩 `RESULT` / `row` 키 재귀 탐색.
- Verification: ITM 단계에서 ITM 행이 정상 파싱되고 DATA 단계에서 다차원 GRP/CLS/ITM 매칭이 동작.

### Unit 5 — 운영 데이터 정리 + 학습/플랜/브레인스토밍 문서화
- Files:
  - `docs/brainstorms/2026-05-30-001-reb-sido-cards-not-shown.md`
  - `docs/plans/2026-05-30-001-fix-reb-sido-cards-plan.md`
  - `docs/solutions/ui-bugs/reb-sido-cards-not-shown-2026-05-30.md`
- Approach: 운영 DB의 기존 `source=REB` 행 일괄 삭제 → 다음 배치에서 의미 코드로 재적재. 본 fix의 원인/정정/예방을 학습 문서로 정본화.
- Verification: `docs/solutions/ui-bugs/` 에 학습 문서 존재. DB 행 68건(17 SIDO × 4 indicator).

## Requirements Trace

- [x] Unit 1 — Entry indicator-code 필드 + yml 매핑
- [x] Unit 2 — RebAdapter 저장 코드 의미 코드로 변경
- [x] Unit 3 — 시군구→SIDO attach
- [x] Unit 4 — RebClient envelope 보정
- [x] Unit 5 — 문서화 (brainstorm + plan + 학습)

## Verification

- 빌드: `./gradlew compileJava` 성공
- 런타임 부팅: 검증 통과
- API: 광역/시군구 진입 시 카드 정상 회신
- 화면: 광역 섹션에 REB 카드 노출
