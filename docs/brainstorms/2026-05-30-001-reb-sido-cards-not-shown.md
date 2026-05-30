---
title: REB 광역(SIDO) 카드 화면 미노출 — 원인 정리 및 정정 방향
date: 2026-05-30
status: completed
related: "이슈 #58, PR #70 머지 후 운영 검증에서 발견된 후속 결함"
---

# REB 광역(SIDO) 카드 화면 미노출

## 문제

이슈 #58 / PR #70 (REB R-ONE 광역시도 어댑터 재설계) 머지 후 운영 검증에서 REB 카드가 화면 광역 섹션에 노출되지 않음.

증상:
- DB(`real_estate_market_indicator`)에 `source=REB` 153 행 정상 적재
- `/api/realestate/sources/availability` 응답 `reb.available=true`
- 그러나 `GET /api/realestate/market/tabs/{category}?regionCode=11` 의 `cards`가 빈 배열
- 시군구(예: `regionCode=11110`) 진입 시에도 REB 광역 카드가 응답에 포함되지 않음
- 화면에 "광역 통계 준비중" 또는 빈 광역 섹션 표시

## 원인

두 결함이 겹쳐 사용자 시점 증상으로 합쳐졌다.

### (1) metadata indicator-code ↔ adapter 저장 코드 불일치

- `realestate-indicator-metadata.yml` 은 의미 코드(`SALE_PRICE_INDEX`, `JEONSE_PRICE_INDEX` 등)로 metadata 6건 등록
- `RebAdapter` 는 indicator_code를 `"REB_" + statblId` (예: `REB_A_2024_00017`) 로 저장
- `RealEstateMarketQueryService.buildCardsFor` → `findHistory(..., key.indicatorCode(), ...)` 매칭 키가 metadata key의 의미 코드 → indicator 테이블에는 STATBL ID로 저장되어 매칭 0건
- 결과: `cards=[]`

### (2) 시군구 진입 시 상위 SIDO 카드 attach 누락

- 도메인은 시군구(5자리) + 광역(2자리) hybrid (Q1=B)
- MOLIT/KOSIS는 시군구, REB는 광역 단위로 적재
- `getTab(regionCode=11110)` 호출 시 그 시군구의 cards만 회신 → MOLIT 시군구 카드만 있고 REB 광역 카드 누락
- 프론트는 한 화면에서 두 단위를 함께 렌더링해야 하므로 백엔드에서 두 단위 row를 함께 응답 필요

## 정정 방향

- **Q1=A 정책 명문화**: 4종 묶음(주택종합/아파트/연립/단독) 중 대표 1종(`-total`)만 metadata와 매핑하여 적재. 나머지 STATBL은 indicator-code 미부여로 어댑터에서 skip.
- **매핑 표현 위치**: `application.yml` 의 STATBL entry에 `indicator-code` 필드 추가. `RebStatblProperties.Entry` 에 nullable 필드로 수용.
- **저장 코드 일치**: `RebAdapter.buildIndicator` 가 STATBL ID 대신 entry.indicatorCode 사용. 미매핑 entry는 `of()` 단계에서 null 반환 → skip.
- **시군구 → SIDO attach**: `RealEstateMarketQueryService.getTab` 에서 `regionCode.length()==5` 이면 앞 2자리로 추가 `buildCardsFor` 호출하여 cards에 합침.
- **부수**: 운영 점검 중 발견된 `RebClient` envelope 보정(`.do` suffix, `INFO-000` result code, 중첩 row/RESULT 재귀 탐색) 포함.

## 영향 범위 / 비범위

영향:
- `RebStatblProperties.java` Entry 필드 추가
- `application.yml` STATBL 매핑 4건 (sale-total / rent-total / trade-volume-apt / land-price-change)
- `RebAdapter.java` 저장 코드 + skip 정책
- `RealEstateMarketQueryService.java` 시군구→SIDO attach
- `RebClient.java` envelope 보정 (운영 점검 후속)
- DB: 기존 `source=REB` 행 일괄 삭제 후 다음 배치에서 의미 코드로 재적재

비범위(별도 PR 권장):
- 아파트/연립/단독 STATBL 활성화 (Q1=B 전환 시 metadata yml 확장)
- 거래량 주택종합(A_2024_00604) metadata 추가
- ACTUAL_TRANSACTION_PRICE_INDEX / MONTHLY_RENT_PRICE_INDEX STATBL 보강
- `OFFICIAL_STATS count=1/region`, `MAX_PAGES=20 truncation`, 배치 elapsed 3분+ 개선

## 검증 기준

- DB: `source=REB` 행이 17 SIDO × 4 indicator = 68 건 적재됨
- API: `tabs/PRICE_INDEX?regionCode=11` cards에 `SALE_PRICE_INDEX/SIDO/REB` 1건
- API: `tabs/PRICE_INDEX?regionCode=11110` cards에 시군구 MOLIT 카드 + 광역 REB 카드 함께 회신
- 화면: 광역시도 직접 진입 또는 시군구 진입 시 광역 섹션에 REB 카드 노출

## 학습 정본

`docs/solutions/ui-bugs/reb-sido-cards-not-shown-2026-05-30.md`