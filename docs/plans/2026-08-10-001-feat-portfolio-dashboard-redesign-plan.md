---
title: 포트폴리오 화면 대시보드형 재설계
type: feat
status: active
date: 2026-08-10
issue: https://github.com/osnet-th/stock-market/issues/110
origin: docs/brainstorms/2026-08-10-portfolio-dashboard-redesign-brainstorm.md
gate: docs/gates/2026-08-10-portfolio-dashboard-redesign-gates.md
---

# 포트폴리오 화면 대시보드형 재설계 (#110)

## Overview

포트폴리오 화면을 목업 v3 기준 **4탭 대시보드**(보유 자산 / 매도 이력 / 목표 배분 / 분석)로 재구성하고, v3에 누락된 기능을 보강한다. 함께 ① 뉴스를 키워드 메뉴로 완전 이관, ② 연금 자산군(`PENSION`) 신설, ③ 신규 집계 4종(자산 추이 스냅샷·배당/이자·CAGR·환차손익)을 추가한다.

작업을 4단계로 나눈다. **Phase 1~2는 프론트엔드 전용**, **Phase 3~4는 Entity·API 변경**을 포함하므로 각 Phase 착수 전 태형님 승인을 받는다.

| Phase | 내용 | 변경 성격 |
|---|---|---|
| 1 | 프론트 4탭 재구성 + 자산 수정 버튼 + 자산군별 액션 규칙 | 프론트 전용 |
| 2 | 뉴스 → 키워드 메뉴 이관 | 프론트 전용 |
| 3 | 연금 자산군 신설 | Entity·API 추가 (승인 게이트) |
| 4 | 신규 집계 4종 | Entity·API 추가 (승인 게이트) |

---

## Phase 1 — 프론트 4탭 재구성

### 변경 파일

| 파일 | 변경 |
|---|---|
| `static/partials/portfolio.html` | 셸(헤더 + 4탭 내비 + 리마인더 + 모달 마운트 지점)만 남기고 탭 본문 분리 |
| `static/partials/portfolio-holdings.html` | 신규 — 보유 자산 탭(KPI·차트·테이블) |
| `static/partials/portfolio-sales.html` | 신규 — 매도 이력 탭 |
| `static/partials/portfolio-targets.html` | 신규 — 목표 배분 탭 |
| `static/partials/portfolio-analysis.html` | 신규 — 분석 탭(종목 목록 + 재무상세 패널) |
| `static/js/app.js` | `partialNames`에 신규 partial 4종 추가 |
| `static/js/components/portfolio.js` | `activeTab` 4값 확장, 테이블 렌더 헬퍼·액션 노출 규칙 헬퍼 추가 |

`portfolio.html`이 696줄이고 4탭이면 1,500줄을 넘기므로 탭 단위로 partial을 쪼갠다. 기존 모달 partial 4종(`portfolio-add`·`portfolio-edit`·`portfolio-sale`·`portfolio-deposit-financial`)은 그대로 둔다.

### 화면 규칙

- **헤더**: `USD/KRW` · 시세 캐시 나이·TTL(`getCacheAgoText`/`getCacheRemainingText` 재사용) · `시세 새로고침`(`loadStockPrices` 호출) · 마감 알림 토글 · `+ 자산 추가`
- **탭 내비**: 보유 자산 / 매도 이력 / 목표 배분 / 분석 + 배지 카운트(보유 건수 · 매도 건수 · 밴드 이탈 건수 · 분석 대상 종목 수)
- **보유 자산 탭**
  - KPI 4장: 총 자산 / 총 평가손익(환차손익 병기) / 누적 수익률(CAGR·보유일수 병기) / 이달 배당·이자 — Phase 4 완료 전까지 미구현 값은 `—` 표시
  - 자산 추이 라인차트 + `스냅샷 저장` — Phase 4 완료 전까지 안내 문구로 대체
  - 도넛 + 우측 범례
  - 테이블: `종목·상품 / 수량 / 평단·금리 / 현재가·만기 / 평가액 / 평가손익 / 수익률 / 비중 / 관리`
  - 그룹 헤더에 건수·원금·평가액·손익·수익률·비중 소계, `모두 펼치기` / `모두 접기`
  - 주식 그룹은 `stockDetail.country` 기준으로 국내/해외 서브그룹(표시 레벨만 — `AssetType.STOCK` 유지)
  - 현금성 자산은 예금/적금/CMA 서브그룹(`getCashSubTypeKey`·`getCashGroupSummary` 재사용)
  - 계좌 연결/미연결 라벨 + 헤더에 미연결 건수
  - 자산군에 항목이 0건이면 미등록 안내 + `첫 자산 추가하기` CTA
- **매도 이력 탭**: KPI 4장(건수·실입금 합계·실현손익·실현수익률) + 월별 그룹 + 미입금 집계 + 상세/수정/삭제 — **전체 기간 조회**(`getAllUserSaleHistories` 그대로)
- **목표 배분 탭**: 밴드 ±%p 조절, 목표 설정 모달, 0기준 편차 막대 + 밴드 음영(#107 구현 재사용), 리밸런싱 제안 + `체크리스트 복사`(클립보드)
- **분석 탭**: 좌측 종목 목록 → 우측 재무상세 패널. **현재 필터 전부 유지**(CFS/OFS · 보고서코드 · 연도 · 계정 필터 · 공시 유형·기간). 패널 폭 드래그 + `localStorage` 저장(`financialPanelWidth` 그대로)
- 로딩 스피너 / 에러 문구 / 재시도 버튼 유지

### 행 액션 — 수정 버튼 추가 + 자산군별 노출 규칙

| 자산군 | 노출 버튼 | 개수 |
|---|---|---|
| 주식(KR/US, ETF 아님) | 키워드 · 재무상세 · AI분석 · 추가매수 · 매도 · 이력 · 수정 · 삭제 | 8 |
| ETF · 펀드 | 추가매수 · 매도 · 이력 · 수정 · 삭제 | 5 |
| 금 · 원자재 · 암호화폐 | 추가매수 · 매도 · 이력 · 수정 · 삭제 | 5 |
| 예금 · 적금 · CMA | 납입 · 이력 · 수정 · 삭제 | 4 |
| 채권 · 연금 | 납입 · 이력 · 수정 · 삭제 | 4 |
| 부동산 · 기타 | 수정 · 삭제 | 2 |

- 관리 열 폭 `340px` → `390px`, 행 최소 폭 `1420px` → `1470px`
- 노출 판정은 `portfolio.js`에 헬퍼로 모은다: `canKeyword(item)` · `canFinancial(item)` · `canTrade(item)` · `canDeposit(item)` — 현행 `x-show` 조건식을 그대로 옮기는 것이며 새 규칙이 아니다
- 매도 이력이 있는 항목의 삭제는 차단 모달(`hasSaleHistories` 가드 유지)
- 매도 이력 수정 폼(`submitSaleEdit`)은 상세 모달 안에 유지

### Implementation Steps

- [x] `portfolio.html`을 셸로 축소, 탭 내비·헤더 구성
- [x] `portfolio-holdings.html` 신규 — KPI·차트 자리·도넛·테이블·그룹 소계·서브그룹
- [x] `portfolio-sales.html` 신규 — 기존 매도 이력 탭 마크업 이전 + 전체 기간
- [x] `portfolio-targets.html` 신규 — 기존 목표 배분 카드 이전 + 밴드 조절 + 리밸런싱 제안 + 체크리스트 복사
- [x] `portfolio-analysis.html` 신규 — 종목 목록 + 기존 재무 슬라이드 패널 연결(마크업 이전 없이 재사용, 필터 전부 유지)
- [x] `app.js` `partialNames` 확장 + `index.html` 마운트 지점 추가
- [x] `portfolio.js` — `activeTab` 4값, 액션 노출 헬퍼, 테이블 행 포맷 헬퍼, 그룹 소계·서브그룹 헬퍼, 미사용 헬퍼 8종 제거
- [x] 목 하네스(`python3 -m http.server`) 실측 검증 — `node` 미설치로 `node --check` 대신 브라우저 `new Function(src)` 파싱 검증

### Phase 1 실행 중 확정된 차이 (work 문서에 상세)

- 행 액션은 **7개가 최대**(주식 KR/US 비ETF). 매수·매도 API 가 주식 전용이라 금·원자재·암호화폐에는 붙이지 않았고, 매수·납입 이력은 각 모달에 이미 있어 별도 `이력` 버튼을 만들지 않았다 → 관리 열 `300px`, 행 최소 폭 `1280px`로 충분(계획했던 390px/1470px 확대 불필요)
- 재무상세 패널은 분석 탭으로 **이동하지 않고 재사용**. 분석 탭은 종목 목록만 담당하고 선택 시 기존 슬라이드 패널이 열린다
- 리밸런싱 총 이동 금액은 상위 버킷/투자자산 내부 **레벨을 분리**해 계산(합산 시 같은 금액 중복 계상 문제 발견·수정)

---

## Phase 2 — 뉴스 → 키워드 메뉴 이관

### 제거 대상 (`portfolio.js` / `portfolio.html`)

- 상태: `portfolio.news`, `portfolio.overseasNews`, `selectedNewsItemId`, `collectingItemId`, `_overseasNewsGeneration`, `_overseasNewsDebounceTimer`
- 메서드: `selectPortfolioNewsItem`, `loadPortfolioNews`, `collectPortfolioNews`, `toggleOverseasNews`, `switchOverseasNewsTab`, `loadOverseasNews`, `loadMoreOverseasNews`, `getNewsEnabledCount`
- 마크업: 카드 클릭 시 뉴스 펼침, 뉴스 목록·페이징, `뉴스 수집` 버튼, 해외속보/해외뉴스종합 탭 패널·더보기

### 유지·변경

- 행 액션 `키워드` → **등록 전용 모달**. 이미 등록된 종목은 버튼을 `등록됨` 비활성 상태로 표시해 중복 등록 방지
- 등록은 기존 `toggleNews`의 등록 경로(`PATCH /api/portfolio/items/{itemId}/news`)를 재사용 — 서버 변경 없음
- 키워드 **삭제**는 키워드 메뉴에서 수행. 포트폴리오에는 삭제 경로를 두지 않는다
- `findKeywordIdByItemName`은 키워드 메뉴 연동에 필요한지 확인 후 미사용이면 제거

### Implementation Steps

- [x] 뉴스 상태·메서드 제거 (상태 7종 · 메서드 9종, 176줄)
- [x] 뉴스 마크업 제거 (Phase 1 셸 재작성 시 이미 제거됨)
- [x] 키워드 등록 모달 신규 + 등록 상태 표시(`등록됨` 비활성)
- [x] 키워드 메뉴 연동 확인 — 서버 `toggleNews`가 `keywordService.registerKeyword(itemName, item.getRegion(), userId)`로 등록, 키워드 메뉴의 `API.getKeywords(userId)`와 동일 저장소 (코드 수준 확인, 실서버 미검증)
- [x] 회귀 확인 — `static/` 전체 grep 결과 뉴스 심볼 잔여 참조 0건
- [x] `api.js`의 `getOverseasBreakingNews` / `getOverseasComprehensiveNews` 래퍼 — **유지 확정** (태형님, 2026-08-10 "유지해"). 후속 정리에서 죽은 코드로 오인하지 않도록 사유 주석 추가

---

## Phase 3 — 연금 자산군 신설 (승인 게이트)

### 스키마 제안

`AssetType`에 `PENSION("연금")` 추가 (9종 → 10종).

`PensionItemEntity extends PortfolioItemEntity` — `@Table(name = "pension_detail")`, `@DiscriminatorValue("PENSION")`, `FundItemEntity` 패턴 준용.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `sub_type` | VARCHAR(20) | `IRP` / `PENSION_SAVING` / `DC` / `DB` |
| `provider` | VARCHAR(100) | 운용사 |
| `evaluated_amount` | NUMERIC(18,2) | 평가액(수기 갱신) |
| `monthly_deposit_amount` | NUMERIC(18,2) NULL | 월 자동이체 금액 |
| `deposit_day` | INTEGER NULL | 자동이체일 |

- 평가 방식: 시세 연동 없이 **평가액 직접 입력**(부동산과 동일 계열) → `PortfolioEvaluationService`에 분기 추가
- 납입: `monthly_deposit_amount`가 있으면 납입 대상(`isDepositTarget`)에 포함 — 기존 `DepositHistory` 재사용
- 배분: 안전자산 버킷에 편입, `AllocationTarget` 자산군 목록에 `PENSION` 추가
- 마이그레이션: `src/main/resources/db/migration/pension_detail_2026_08_10.sql`
- 기존 데이터 자동 이관 없음(범위 밖) — 사용자가 수정으로 변경

### API

- `POST /api/portfolio/items/pension`
- `PUT /api/portfolio/items/pension/{itemId}`
- 조회는 기존 `GET /api/portfolio/items` 응답에 `pensionDetail` 추가

### Implementation Steps

- [x] `AssetType.PENSION` 추가
- [x] `PensionItemEntity` + `PensionSubType` + `PensionDetail` + mapper 3곳 + `PensionDetailResponse`
- [x] 마이그레이션 SQL (`pension_detail_2026_08_10.sql`)
- [x] `PortfolioService` add/update 유스케이스 + Controller 엔드포인트 2종 + 요청 DTO 2종
- [x] `PortfolioEvaluationService` 평가 분기 (evaluatedAmount, 미입력 시 원금)
- [x] 배분 반영 — `AssetClassification.SAFE_TYPES`에 편입해 `AllocationStatusService`가 자동 처리 (별도 수정 불필요)
- [x] 납입 대상 확장 — `validateDepositTarget` · `isDepositOverdue` · `depositTargetIds` 필터
- [x] `financial.js` `assetTypeConfig`에 `PENSION` 추가
- [x] 자산 추가/수정 모달 자산군 확장 + `api.js` 메서드 2종
- [x] 기존 테스트 재구성 생성자 호출 9곳에 인자 추가 → `compileTestJava` · `test --tests "*Portfolio*"` (59 tests) PASS

### Phase 3 실행 중 확정된 차이

- 프론트 `getEvalAmount()`도 PENSION 분기가 필요했다(서버만 고치면 목록이 원금으로 표시됨) — 서버와 동일 규칙으로 추가
- 배분 서비스는 `AssetClassification` 한 곳만 고치면 되어 계획했던 `AllocationTargetService`/`AllocationStatusService` 직접 수정은 불필요했다

---

## Phase 4 — 신규 집계 4종 (승인 게이트)

### 4-1. 자산 추이 스냅샷

**테이블** `portfolio_snapshot`

| 컬럼 | 타입 |
|---|---|
| `id` | BIGSERIAL PK |
| `user_id` | BIGINT NOT NULL |
| `snapshot_date` | DATE NOT NULL |
| `total_evaluated` | NUMERIC(18,2) NOT NULL |
| `total_invested` | NUMERIC(18,2) NOT NULL |
| `created_at` | TIMESTAMP NOT NULL |

- `UNIQUE (user_id, snapshot_date)` — 같은 날 재저장은 **덮어쓰기(upsert)**
- `POST /api/portfolio/snapshots` — 오늘자 저장. 평가액은 `PortfolioEvaluationService` 재사용
- `GET /api/portfolio/snapshots?months=12` — 최근 N개월 조회
- 자동 저장(스케줄러)은 이번 범위 밖 — 수동 `스냅샷 저장` 버튼만

### 4-2. 배당 · 이자 집계

`GET /api/portfolio/income/summary`

```
{
  "monthAmount": 412000,        // 이달 배당 + 이자
  "yearEstimate": 4550000,      // 연 예상
  "dividendYield": 2.14,        // 시가배당률(%)
  "basis": "KR_STOCK_KSD_ONLY", // 집계 기준
  "excludedCount": 3            // 배당 데이터 없어 제외된 항목 수
}
```

- 국내 주식 배당: `KisKsdScheduleClient`(KSD 배당 일정) × 보유 수량
- 예적금·채권 이자: 금리·만기 기반 — 기존 `getExpectedMaturityAmount` 산출 로직 재사용
- **해외 주식 배당 데이터 소스 없음** → 집계에서 제외하고 `basis`·`excludedCount`로 명시
- 신규 `PortfolioIncomeService` (application)

### 4-3. CAGR · 보유일수

`GET /api/portfolio/summary`

```
{
  "totalEvaluated": ..., "totalInvested": ...,
  "profit": ..., "profitRate": ...,
  "holdingDays": 1043,
  "cagr": 11.4,
  "fxProfit": 1234567,
  "fxUnknownCount": 2     // 매수 환율 미기록 항목 수
}
```

- 보유일수 = `today − min(StockPurchaseHistory.purchasedAt)`. 매수 이력이 없는 수기 자산은 산출에서 제외
- CAGR = `(평가액 / 투자원금)^(365 / 보유일수) − 1`. 보유일수 < 30이면 `null`(표시는 `—`)

### 4-4. 환차손익

- `stock_purchase_history`에 `fx_rate NUMERIC(12,4) NULL` 컬럼 추가 + 마이그레이션
- 해외 주식 매수 등록·수정 시 적용 환율 저장 (`AddStockPurchaseParam`·매수 이력 수정 파라미터 확장)
- 환차손익 = `Σ 수량 × 매수단가 × (현재환율 − 매수환율)`, `fx_rate`가 NULL인 이력은 제외하고 `fxUnknownCount`로 노출
- 결과는 4-3의 `/api/portfolio/summary`에 포함

### Implementation Steps

- [x] `portfolio_snapshot` 도메인·Entity·Repository(port+impl)·마이그레이션
- [x] 스냅샷 저장/조회 API 2종 (`POST /snapshots`, `GET /snapshots?months=12`)
- [x] `PortfolioIncomeService` + `/income/summary`
- [x] `stock_purchase_history.fx_rate` 컬럼 + 마이그레이션 + 도메인/Entity/매퍼/DTO + 매수 등록·수정 경로 반영
- [x] `/api/portfolio/summary` (CAGR·보유일수·환차손익) — `PortfolioSummaryService`
- [x] 프론트 KPI 4장·자산 추이 라인차트·스냅샷 저장 버튼 연결
- [x] `compileJava`·`compileTestJava`·`test --tests "*Portfolio*"` (59 tests) PASS

### Phase 4 실행 중 확정된 차이

- **배당 산출 방식 변경**: plan 의 "KSD 배당 일정 × 보유 수량" 대신 `StockDetail.dividendYield × 평가액` 기반. KSD 는 종목당 외부 API 1회라 화면 진입마다 호출하기엔 비용·실패 위험이 크다. 이달 금액은 연 예상의 1/12(월 평균 환산)이며 `basis = ESTIMATED_MONTHLY_AVERAGE` 와 화면 캡션으로 명시. KSD 실지급일 기준 집계는 후속 과제
- **매수 환율은 역산 저장**: 프런트가 환율을 따로 보내지 않으므로 `investedAmountKrw / (수량 × 매수단가)` 로 산출
- **`renderTrendChart` 이름 충돌** — `salary.js` 에 동명 메서드가 있어 포트폴리오 쪽이 덮어써지는 버그 발견 → `renderPortfolioTrendChart` 로 분리

---

## Validation

- `node --check src/main/resources/static/js/components/portfolio.js`
- `./gradlew compileJava` (Phase 3~4)
- worktree에서 `SERVER_PORT=8081 ./gradlew bootRun` (dev 프로필) 후 브라우저 실측
  - 4탭 전환 · 배지 카운트
  - 행 액션 자산군별 노출 규칙(자산군 10종 시드로 확인)
  - 자산 수정 모달 정상 동작
  - 매도 이력 전체 기간 · 목표 배분 저장 · 분석 탭 필터 유지
  - 뉴스 제거 후 콘솔 에러 없음
  - 스냅샷 저장 → 차트 반영, 배당·CAGR·환차손익 값 표시
- 자산군 10종 시드 데이터로 각 Phase 종료 시 스크린샷 증빙

## 리스크

- **범위가 큼** — Phase 1만으로도 `portfolio.html` 전면 재작성. Phase 단위로 커밋을 나눠 되돌릴 수 있게 한다
- **해외 주식 배당 데이터 부재** — 이달 배당·이자가 국내 종목 위주로만 집계됨. 화면에 집계 기준을 명시해 오해를 막는다
- **기존 매수 이력의 환율 미기록** — 환차손익이 부분 집계됨. `fxUnknownCount`를 화면에 노출
- **`ddl-auto=update` 환경에서 `@DiscriminatorValue` 추가** — `pension_detail` 테이블은 마이그레이션 SQL로 명시 생성하고 자동 생성에 의존하지 않는다 (#66 전례)
- **분석 탭 이전 시 재무 패널 회귀** — `financial.js` 상태가 `portfolio` 객체에 얹혀 있어 partial 분리 시 참조가 끊길 수 있다. 상태 소유는 그대로 두고 마크업만 이동한다
