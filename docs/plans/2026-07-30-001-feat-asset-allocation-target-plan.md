---
title: 포트폴리오 목표 자산 배분 비율
type: feat
status: active
date: 2026-07-30
issue: https://github.com/osnet-th/stock-market/issues/102
origin: docs/brainstorms/2026-07-30-asset-allocation-target-brainstorm.md
gate: docs/gates/2026-07-30-asset-allocation-target-gates.md
---

# 포트폴리오 목표 자산 배분 비율 (#102)

## Overview

상위 안전/투자 목표 비율 + 투자자산 내 자산군별 목표 비율을 설정하고, 평가액 기준 현재 배분과 비교해 편차(금액·%p)를 표시한다. 허용밴드(기본 ±5%p) 초과 시 경고 강조. GOLD 항목은 KRX 금시세로 현재가 평가(신규 외부 연동). 암호화폐는 배분 전 기능에서 제외. 대시보드 포트폴리오 카드에 배분 요약 추가.

## 현재 구조 (근거)

- `AssetType` 9종, `PortfolioItem` + 타입별 Detail 패턴 (`PortfolioItem.java:28-32`)
- 평가: `PortfolioEvaluationService.buildEvaluation` — STOCK만 현재가, 나머지 원금 (`PortfolioEvaluationService.java:65-113`)
- 기존 `/api/portfolio/allocation` — **원금 기준** AssetType별 비중만 반환, 목표 개념 없음 (`PortfolioAllocationService.java`)
- GOLD 항목: `gold_detail` 테이블 존재하나 추가 컬럼 없음 → **수량(그램) 정보가 없어 시세 평가 불가** (`GoldItemEntity.java`)
- 대시보드 카드: 항목 개수·유형별 건수·뉴스 활성 개수만 (`home.js:50-63`)
- 스키마: `ddl-auto: update` + `db/migration/*.sql` 백업 관례
- data.go.kr 연동 관례 존재: `DATAGOKR_SERVICE_KEY` (금융위 1160100 KRX 상장종목정보)

## 승인 게이트 대상 (본 plan 승인 시 일괄 승인 처리)

1. **Entity 신규 2건**: `AllocationTargetEntity`(allocation_target) + `AllocationTargetAssetEntity`(allocation_target_asset)
2. **Entity 수정 1건**: `GoldItemEntity`에 `quantity_grams` 컬럼 추가 (+ `GoldDetail` 도메인 모델 신규)
3. **신규 공개 API 3건**: GET/PUT `/api/portfolio/allocation/target`, GET `/api/portfolio/allocation/status`
4. **기존 공개 API 확장 1건**: `/items/general` add/update 요청에 optional `quantityGrams` 추가 (GOLD 전용)
5. **신규 외부 연동**: data.go.kr 금융위원회_일반상품시세정보 `GetGeneralProductInfoService/getGoldPriceInfo` (KRX 금시세)
6. **비즈니스 로직 변경**: `PortfolioEvaluationService`에 GOLD 시세 평가 추가

## 설계

### Entity / Domain

**allocation_target** (사용자당 1행, user_id unique)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | |
| user_id | bigint, unique | ID 참조 |
| safe_ratio | numeric(5,2) | 안전자산 목표 % |
| invest_ratio | numeric(5,2) | 투자자산 목표 % (safe+invest=100) |
| band_pct_point | numeric(4,2) | 허용밴드 %p, 기본 5 |
| created_at / updated_at | timestamp | |

**allocation_target_asset** (투자자산 내 자산군별 목표, target당 N행)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | bigint PK | |
| allocation_target_id | bigint | ID 참조 (연관관계 금지) |
| asset_type | varchar | STOCK·FUND·GOLD·COMMODITY·REAL_ESTATE·OTHER만 허용 (CRYPTO 금지) |
| target_ratio | numeric(5,2) | 합=100 |

- 도메인 모델 `AllocationTarget`: 검증 — safe+invest=100, 자산군 합=100, 각 0~100, band 0~20, CRYPTO 불가
- 안전/투자 분류 상수: 도메인에 `AssetClassification.isSafe(AssetType)` — CASH·BOND=안전 (고정 매핑)
- `GoldDetail` 도메인 모델: `quantityGrams`(BigDecimal, nullable 아님 — detail 자체가 optional) + `PortfolioItem.goldDetail` 추가 (재구성 생성자·mapper 확장, 기존 detail 패턴 동일)
- port: `portfolio/domain/repository/AllocationTargetRepository` + `portfolio/domain/service/GoldPriceProvider` (`Optional<BigDecimal> getPricePerGram()`)

### Infrastructure

- `AllocationTargetEntity`·`AllocationTargetAssetEntity` + JpaRepository + `AllocationTargetRepositoryImpl` + mapper (기존 패턴)
- `GoldItemEntity.quantityGrams` (nullable numeric) — ddl-auto가 컬럼 추가, `db/migration/gold_detail_quantity_grams.sql` 백업 작성
- `portfolio/infrastructure/goldprice/KrxGoldPriceAdapter` — data.go.kr `getGoldPriceInfo` 최근 영업일 종가(금 99.99K 1g), **in-memory 캐시 TTL 1시간**, 실패 시 `Optional.empty()` (예외 전파 금지)
- `application.yml`: `gold.api.base-url=https://apis.data.go.kr/1160100/service/GetGeneralProductInfoService/getGoldPriceInfo`, service-key는 `DATAGOKR_SERVICE_KEY` 재사용

### Application

- `PortfolioEvaluationService.buildEvaluation`: GOLD && goldDetail 존재 && 시세 성공 → `evaluated = quantityGrams × pricePerGram` (그 외 원금 유지). 시세는 호출당 1회 조회
- `AllocationTargetService`: `getTarget(userId)` / `saveTarget(userId, param)` 업서트 + 검증
- `AllocationStatusService`: 평가 결과(**CRYPTO 제외, ACTIVE만**) + target → 현황 계산
  - bucket(SAFE/INVEST)별·투자 자산군별: `currentAmount, currentRatio, targetRatio, targetAmount, deviationAmount(=current−target×total), deviationPctPoint, bandExceeded(|deviation|>band)`
  - `configured=false`(목표 미설정), `totalEvaluated=0`이면 현황 생략, `excludedCryptoCount` 포함
- 기존 `PortfolioAllocationService`·`/allocation` 불변

### Presentation (PortfolioController 추가)

| 엔드포인트 | 동작 |
|---|---|
| `GET /api/portfolio/allocation/target` | 설정 조회 (미설정 시 body null 200) |
| `PUT /api/portfolio/allocation/target` | 설정 업서트 (검증 실패 400) |
| `GET /api/portfolio/allocation/status` | 배분 현황 (목표·현재·편차·밴드초과·excludedCryptoCount) |

- `/items/general` add/update 요청 DTO에 `quantityGrams`(optional, GOLD 외 타입에서 값 존재 시 400)
- `PortfolioItemResponse`에 `goldDetail { quantityGrams }` 추가

### Frontend

- `partials/portfolio.html`: 기존 자산 비중 영역에 "자산 배분 목표" 섹션 — 상위 2버킷 막대(목표 마커 + 현재 비율), 투자 내 자산군 행별 편차 `+N원 (+N.N%p)`, 밴드 초과 시 경고색, CRYPTO 보유 시 "배분 제외: 암호화폐 N건" 안내, [목표 설정] 버튼 → 모달(비율 입력, 합 100 실시간 검증, 밴드 입력)
- `js/components/portfolio.js`: `allocationStatus` 상태 + `loadAllocationStatus()`(loadPortfolio와 병렬) + 모달 상태/저장/검증 메서드
- `js/api.js`: `getAllocationTarget` / `saveAllocationTarget` / `getAllocationStatus`
- `partials/portfolio-add.html`·`portfolio-edit.html`: 일반 자산 폼에서 GOLD 선택 시 "보유 중량(g)" 입력 노출
- `partials/home.html` + `js/components/home.js`: 포트폴리오 카드 업그레이드 — status 조회 성공 시 총 평가액·안전/투자 현재vs목표·밴드 초과 경고 배지, 실패/미설정 시 기존 개수 표시 유지

## Implementation Steps

### Phase 1: 목표 설정 백엔드
- [x] `AllocationTarget` 도메인 모델 + 검증 + `AssetClassification`
- [x] Entity 2종 + JpaRepository + RepositoryImpl + mapper + 백업 SQL
- [x] `AllocationTargetService` (get/save 업서트)
- [x] GET/PUT `/api/portfolio/allocation/target` + DTO

### Phase 2: 금 시세 연동 + GOLD 평가
- [x] `GoldDetail` 도메인 + `PortfolioItem.goldDetail` (재구성 생성자·create/update 경로)
- [x] `GoldItemEntity.quantityGrams` + mapper + 백업 SQL
- [x] `/items/general` add/update에 `quantityGrams` 처리 + `PortfolioItemResponse.goldDetail`
- [x] `GoldPriceProvider` port + `KrxGoldPriceAdapter` (캐시·fallback) + application.yml
- [x] `PortfolioEvaluationService` GOLD 평가 반영

### Phase 3: 배분 현황 API
- [x] `AllocationStatusService` — CRYPTO 제외 집계, 버킷/자산군 편차·밴드 계산
- [x] GET `/api/portfolio/allocation/status` + DTO

### Phase 4: 포트폴리오 페이지 UI
- [x] api.js 3종 + portfolio.js 상태·로드·모달·검증
- [x] portfolio.html 배분 섹션 + 설정 모달
- [x] portfolio-add/edit GOLD 중량 입력

### Phase 5: 대시보드
- [x] home.js status 조회(실패 무해화) + home.html 카드 업그레이드

## Validation

- `./gradlew compileJava` (또는 build) — 백엔드 컴파일
- `node --check` — 수정 JS 문법
- KRX 금시세 API 실호출 확인 (서비스키 구독 여부 포함) — 실패 시 태형님에게 data.go.kr "금융위원회_일반상품시세정보" 활용신청 요청
- 스크래치패드 목 데이터 하네스(기존 방식)로 UI 확인: 목표 미설정/설정/밴드 초과/CRYPTO 보유/총액 0
- 실서버 확인 항목은 validation 문서에 기록

## Risks

- `PortfolioItem` 재구성 생성자 파라미터 추가 — 호출부 전체 컴파일 확인 필요 (컴파일 타임에 잡힘)
- DATAGOKR_SERVICE_KEY가 일반상품시세정보에 미구독이면 금 시세 실패 → 원금 fallback으로 동작은 유지, 활용신청 필요
- 금시세 API는 전일 종가 기준(장중 실시간 아님) — 일 단위 리밸런싱 판단 용도로 수용
- 평가액 합산에 STOCK 외화 environments(investedAmountKrw) 기존 로직 그대로 사용 — 배분 계산도 동일 기준

## Out of Scope

- 자동 리밸런싱 실행, 메일/푸시 알림, 안전자산 내부 세부 비율, 매핑 재정의
- 암호화폐·펀드·부동산 시세 연동
- 기존 `/api/portfolio/allocation`(원금 기준 도넛)·등록 화면 자산 유형 선택지 변경
