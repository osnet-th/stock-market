---
title: "feat: KIS 국내주식 종목정보 기반 종목 평가 기능"
type: feat
status: active
date: 2026-06-02
issue: 75
origin: docs/brainstorms/2026-06-02-kis-stock-evaluation-requirements.md
---

# feat: KIS 국내주식 종목정보 기반 종목 평가 기능

## Overview

보유 여부와 무관하게 KRX 종목을 검색·선택하여 KIS 국내주식[종목정보] API군(재무제표·비율 7종, 종목추정실적, 당사 신용가능종목, 예탁원 일정 12종, 상품기본조회)으로 평가하는 **독립 "종목 평가" 화면**을 신설한다. DB 저장 없는 온디맨드 호출이며, 기존 DART 재무상세와 분리된 별도 도메인(`stockevaluation`)으로 구현한다. (brainstorm: docs/brainstorms/2026-06-02-kis-stock-evaluation-requirements.md)

## Problem Statement / Motivation

매수 전 종목 판단용 리서치 화면이 없다. 종목 검색은 포트폴리오 추가 폼의 한 단계(`searchStock`→`selectStock`)일 뿐이고, 재무상세(DART)는 보유 종목 전용이다. "살지 말지" 판단 도구가 들어갈 자리가 없다.

## Proposed Solution

### Backend — `stockevaluation` 도메인 신규 (overseasnews YAGNI 패턴)

비즈니스 불변식·영속성이 없는 pass-through 기능이므로 domain/model 레이어를 생략하고 매핑은 서비스/클라이언트에 인라인한다 (overseasnews 선례 동일).

```
stockevaluation/
├── application/
│   ├── StockEvaluationService.java          # 유스케이스 (KIS 호출 → 응답 DTO 변환)
│   └── dto/
│       ├── StockBasicInfoResponse.java      # 상품기본조회
│       ├── FinanceStatementResponse.java    # 재무 7종 공통 (기간 레코드 리스트)
│       ├── EstimatePerformResponse.java     # 종목추정실적
│       ├── CreditEligibilityResponse.java   # 신용가능 여부
│       └── KsdScheduleResponse.java         # 예탁원 일정 공통
├── domain/model/
│   ├── FinanceStatementType.java            # enum: 재무 7종 → (path, tr_id) 매핑
│   └── KsdScheduleType.java                 # enum: 예탁원 12종 → (path, tr_id, 특수파라미터) 매핑
├── infrastructure/kis/
│   ├── KisStockInfoClient.java              # search-info / estimate-perform / credit-by-company
│   ├── KisFinanceClient.java                # 재무 7종 (FinanceStatementType 파라미터화)
│   ├── KisKsdScheduleClient.java            # 예탁원 12종 (KsdScheduleType 파라미터화)
│   └── dto/                                 # KIS raw output DTO (@Getter @NoArgsConstructor)
└── presentation/
    └── StockEvaluationController.java
```

> enum(type→path/tr_id)만 domain/model에 두는 이유: 7종/12종이 구조 동일이라 분기 상수를 한곳에 모아 클라이언트 메서드를 1개로 유지. 검증할 비즈니스 규칙은 없음.

### KIS 인프라 재사용 (변경 최소)

- 인증: 기존 `KisTokenManager` 그대로. **계좌번호 불필요**(시세/정보 API, 연구 확정).
- 호출: 단일 `output` API는 기존 `KisApiClient.get()` 재사용. 예탁원 연속조회(CTS/tr_cont)는 Phase 3에서 필요 시 기존 `getWithContinuation()` 활용, 1차는 1페이지(기간 충분)로 한정.
- 종목코드: `StockResponse.stockCode`가 KRX 6자리(`Stock.java:7` `005930`) → `FID_INPUT_ISCD`/`SHT_CD`/`PDNO`에 그대로 전달.

### Frontend — 독립 "종목 평가" 메뉴 (glossary/financial 패턴 복제)

- 좌측 사이드바 `menus` 배열에 `stock-eval` 추가(포트폴리오 다음 권장, icon `note` 재사용 → SVG 추가 불필요).
- partial `stock-eval.html` + component `stock-eval.js`(전역 객체 + spread 병합).
- 탭은 재무상세 탭 패턴(`portfolio-deposit-financial.html:234-244` + `financial.js:454-475 selectFinancialMenu`) 복제 → `[재무]/[추정·신용]/[일정]`.
- 화면 상단: KRX 검색(기존 `searchStocks` 재사용) → 종목 선택 → 기본정보 헤더 + 탭.

## API Reference (연구 확정 — 출처: koreainvestment/open-trading-api)

> path·tr_id·필수 파라미터·output 분기는 공식 저장소 소스 확정값. **응답 상세 필드 전체 목록은 구현 시 실제 응답으로 확정**(특히 estimate-perform output2~4 라벨).

**기본 (1)**

| 한글명 | Path | tr_id | 필수 파라미터 | output |
|---|---|---|---|---|
| 상품기본조회 | GET `/uapi/domestic-stock/v1/quotations/search-info` | `CTPF1604R` | `PDNO`(종목코드), `PRDT_TYPE_CD`(상품유형, 주식 추정 300 — 구현 시 확정) | 단일 `output` |

**재무 7종** (공통: `FID_DIV_CLS_CODE`=0:년/1:분기, `FID_COND_MRKT_DIV_CODE`=J, `FID_INPUT_ISCD`=종목코드, 단일 `output` 시계열)

| 한글명 | Path | tr_id |
|---|---|---|
| 대차대조표 | `/uapi/domestic-stock/v1/finance/balance-sheet` | `FHKST66430100` |
| 손익계산서 | `/uapi/domestic-stock/v1/finance/income-statement` | `FHKST66430200` |
| 재무비율 | `/uapi/domestic-stock/v1/finance/financial-ratio` | `FHKST66430300` |
| 수익성비율 | `/uapi/domestic-stock/v1/finance/profit-ratio` | `FHKST66430400` |
| 기타주요비율 | `/uapi/domestic-stock/v1/finance/other-major-ratios` | `FHKST66430500` |
| 안정성비율 | `/uapi/domestic-stock/v1/finance/stability-ratio` | `FHKST66430600` |
| 성장성비율 | `/uapi/domestic-stock/v1/finance/growth-ratio` | `FHKST66430800` |

**기타 2종**

| 한글명 | Path | tr_id | 핵심 파라미터 | output |
|---|---|---|---|---|
| 종목추정실적 | GET `/uapi/domestic-stock/v1/quotations/estimate-perform` | `HHKST668300C0` | `SHT_CD`(종목코드) | `output1~4` 다중 |
| 당사 신용가능종목 | GET `/uapi/domestic-stock/v1/quotations/credit-by-company` | `FHPST04770000` | `FID_RANK_SORT_CLS_CODE`(0/1), `FID_SLCT_YN`(0:가능/1:불가), `FID_INPUT_ISCD`(시장코드 0000:전체), `FID_COND_SCR_DIV_CODE`(20477), `FID_COND_MRKT_DIV_CODE`(J) | 단일 `output` **전체목록** |

**예탁원 12종** (공통: `F_DT`/`T_DT`(필수, YYYYMMDD), `CTS`(연속키 공백), `SHT_CD`(선택: 공백=전체/종목코드=필터), 단일 `output1`)

| 한글명 | Path | tr_id | 특수 파라미터 |
|---|---|---|---|
| 배당일정 | `/uapi/domestic-stock/v1/ksdinfo/dividend` | `HHKDB669102C0` | `GB1`(0:전체/1:결산/2:중간), `HIGH_GB` |
| 주식매수청구 | `/uapi/domestic-stock/v1/ksdinfo/purreq` | `HHKDB669103C0` | — |
| 합병/분할 | `/uapi/domestic-stock/v1/ksdinfo/merger-split` | `HHKDB669104C0` | — |
| 액면교체 | `/uapi/domestic-stock/v1/ksdinfo/rev-split` | `HHKDB669105C0` | `MARKET_GB` |
| 자본감소 | `/uapi/domestic-stock/v1/ksdinfo/cap-dcrs` | `HHKDB669106C0` | — |
| 상장정보 | `/uapi/domestic-stock/v1/ksdinfo/list-info` | `HHKDB669107C0` | — |
| 공모주청약 | `/uapi/domestic-stock/v1/ksdinfo/pub-offer` | `HHKDB669108C0` | — |
| 실권주 | `/uapi/domestic-stock/v1/ksdinfo/forfeit` | `HHKDB669109C0` | — |
| 의무예치 | `/uapi/domestic-stock/v1/ksdinfo/mand-deposit` | `HHKDB669110C0` | — |
| 유상증자 | `/uapi/domestic-stock/v1/ksdinfo/paidin-capin` | `HHKDB669100C0` | `GB1`(1:청약일별/2:기준일별) |
| 무상증자 | `/uapi/domestic-stock/v1/ksdinfo/bonus-issue` | `HHKDB669101C0` | — |
| 주주총회 | `/uapi/domestic-stock/v1/ksdinfo/sharehld-meet` | `HHKDB669111C0` | — |

### 신규 public API (Approval Gate — 신규 엔드포인트)

```
GET /api/stock-evaluation/{stockCode}/basic-info
GET /api/stock-evaluation/{stockCode}/finance/{type}?divCls=ANNUAL|QUARTER   # type: 재무 7종 enum
GET /api/stock-evaluation/{stockCode}/estimate-perform
GET /api/stock-evaluation/{stockCode}/credit-eligibility
GET /api/stock-evaluation/{stockCode}/schedules/{type}?fromDate=&toDate=     # type: 예탁원 12종 enum, SHT_CD={stockCode}
```

- 입력 검증: `stockCode` 6자리 숫자 regex, `type`은 enum 바인딩, 날짜 YYYYMMDD regex. (overseasnews 검증 패턴 재사용)
- 신용 여부: `credit-by-company`를 종목 시장코드로 호출 → 응답 목록에서 `stockCode` 멤버십으로 가능/불가 판정. **성능 주의**(목록 스캔) → 결과 단기 캐시 검토.
- 예탁원 기본 조회기간: `fromDate`=오늘−6개월, `toDate`=오늘+3개월 (미지정 시 서버 기본값), `SHT_CD`=종목코드.

---

## Implementation Phases

> 각 Phase는 독립 배포 가능. 한 번에 하나의 Phase만 진행, 완료 후 사용자 확인.

> **구현 상태 (2026-06-02)**: Phase 0~3 코드 전부 구현 완료. 검증 통과 — 컴파일, 앱 부팅(빈 와이어링), 전 엔드포인트 매핑(정상/400/502), 입력검증(잘못된 type·종목코드 400), JS 문법. ⚠️ **실데이터 표시 미검증** — 검증 환경의 `.env` KIS AppKey가 `EGW00103(유효하지 않은 AppKey)`로 거부됨. 유효 AppKey + 로그인 세션 환경에서 화면 표시 최종 확인 필요.

### Phase 0 — 도메인 셸 + 상품기본조회 + 화면 골격

**Backend**
- [x] `stockevaluation` 패키지 생성 (application/domain/infrastructure/presentation) — **Approval Gate: 패키지 구조**
- [x] `KisStockInfoClient.searchInfo(stockCode)` 구현 (`search-info`, CTPF1604R, PRDT_TYPE_CD 확정)
- [x] `StockBasicInfoResponse` + 서비스 인라인 매핑
- [x] `StockEvaluationController` + `GET /{stockCode}/basic-info` (입력 검증 포함) — **Approval Gate: 신규 API**

**Frontend**
- [x] `app.js` `validPages`/`menus`/`partialNames`/`navigateTo` 4곳 + `index.html` placeholder·script 2곳에 `stock-eval` 등록 — **Approval Gate: 신규 메뉴**
- [x] `partials/stock-eval.html`: KRX 검색창 + 종목 선택 + 기본정보 헤더 + 빈 탭 골격
- [x] `js/components/stock-eval.js`: `StockEvalComponent`(검색 재사용, 탭 상태), `app.js` spread 병합
- [x] `api.js`에 평가 API 섹션 추가 (`getStockBasicInfo`)
- [x] 검증: 메뉴 진입 → 종목 검색·선택 → 기본정보 표시

### Phase 1 — 재무제표·비율 7종 ([재무] 탭)

**Backend**
- [x] `FinanceStatementType` enum (7종 → path/tr_id)
- [x] `KisFinanceClient.fetch(type, stockCode, divCls)` (단일 메서드, KisApiClient.get 재사용)
- [x] `FinanceStatementResponse` (기간 레코드 리스트; **응답 필드 실제 확인 후 확정**)
- [x] `GET /{stockCode}/finance/{type}?divCls=` 서비스/컨트롤러

**Frontend**
- [x] [재무] 탭: 7개 하위 선택(대차대조표/…/성장성) + 연/분기 토글
- [x] `api.js getFinance(stockCode, type, divCls)`
- [x] 표 렌더링(기간 컬럼), generation counter로 탭 전환 레이스 방지 (financial.js 패턴)
- [x] 검증: 7종 조회·표시, 빈 응답/에러 UX

### Phase 2 — 종목추정실적 + 신용가능종목 ([추정·신용] 탭)

**Backend**
- [x] `KisStockInfoClient.estimatePerform(stockCode)` (output1~4 **실제 응답 매핑 확인**)
- [x] `KisStockInfoClient.creditEligibility(stockCode, marketCode)` + 목록 멤버십 판정
- [x] `EstimatePerformResponse`, `CreditEligibilityResponse`
- [x] `GET /{stockCode}/estimate-perform`, `GET /{stockCode}/credit-eligibility`

**Frontend**
- [x] [추정·신용] 탭: 추정실적 표 + 신용거래 가능/불가 배지
- [x] `api.js` 함수 2종
- [x] 검증: 추정 데이터 표시, 신용 여부 정확성(가능/불가 종목 교차확인)

### Phase 3 — 예탁원 일정 12종 ([일정] 탭)

**Backend**
- [x] `KsdScheduleType` enum (12종 → path/tr_id/특수파라미터 GB1·MARKET_GB)
- [x] `KisKsdScheduleClient.fetch(type, stockCode, fromDate, toDate)` (SHT_CD=종목코드 필터)
- [x] `KsdScheduleResponse` (일정 레코드 리스트; **필드 실제 확인**)
- [x] `GET /{stockCode}/schedules/{type}?fromDate=&toDate=` (기본 기간 적용)
- [x] (선택) 연속조회 필요 시 `getWithContinuation` 적용

**Frontend**
- [x] [일정] 탭: 12종 일정 선택 + 기간 선택(기본값) + 목록 표시
- [x] `api.js getSchedule(stockCode, type, fromDate, toDate)`
- [x] 검증: 일정 조회, 데이터 없는 종목/기간 UX

---

## Approval Gates (작업 중 중단·확인)

- [Phase 0] 신규 도메인 패키지 구조
- [Phase 0~3] 신규 public API 엔드포인트 추가
- [Phase 0] 신규 프론트 메뉴/라우트 추가
- Phase 간 연속 진행 시 (한 번에 하나의 Phase)

## Risks

- **응답 필드 미확정**: path/tr_id는 확정이나 일부 응답 필드 전체 목록·estimate-perform output2~4 라벨 미확정 → 각 Phase에서 실제 응답으로 DTO 확정 (추측 매핑 금지).
- **신용가능종목 목록 스캔**: 단일 종목 판정을 위해 시장 전체 목록 조회 → 호출 비용/지연. 단기 캐시 또는 호출 범위 최소화 검토.
- **KIS rate limit**: 다수 API 동시 호출 시 한도 초과 가능 → 탭 진입 시 필요한 것만 호출(지연 로딩), 동시 호출 자제.
- **PRDT_TYPE_CD 값**: search-info 상품유형코드 국내주식 값 구현 시 확정 필요(300 추정).

## Open Questions (구현 중 해소)

- [Phase 0] `search-info` `PRDT_TYPE_CD` 국내주식 정확값.
- [Phase 1] 재무 7종 각 응답 필드 전체 목록.
- [Phase 2] `estimate-perform` output1~4 필드 라벨·구조; 신용 판정용 시장코드 결정(0000 전체 vs 시장별).
- [Phase 3] 예탁원 일정별 응답 필드, GB1/MARKET_GB 코드표, 연속조회 필요 여부.

## Next Steps

1. 본 plan 승인
2. 이슈 #75 기반 feature 브랜치 생성
3. Phase 0부터 순차 구현 (Approval Gate 준수)
