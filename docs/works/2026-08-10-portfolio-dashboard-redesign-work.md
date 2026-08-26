# 포트폴리오 화면 대시보드형 재설계 Work 기록 (#110)

gate: docs/gates/2026-08-10-portfolio-dashboard-redesign-gates.md
plan: docs/plans/2026-08-10-001-feat-portfolio-dashboard-redesign-plan.md

## Phase 1 — 프론트 4탭 재구성 (완료)

### 변경 파일

| 파일 | 변경 |
|---|---|
| `static/partials/portfolio.html` | 696줄 → 셸(헤더 + 4탭 내비 + 로딩/빈 상태 + 목표 설정 모달)로 축소 |
| `static/partials/portfolio-holdings.html` | 신규 — KPI 4장, 자산 추이 자리, 도넛 + 범례, 테이블형 보유 목록 |
| `static/partials/portfolio-sales.html` | 신규 — 매도 이력 탭(전체 기간) |
| `static/partials/portfolio-targets.html` | 신규 — 목표 배분 + 리밸런싱 제안 |
| `static/partials/portfolio-analysis.html` | 신규 — 분석 대상 종목 목록 |
| `static/js/components/portfolio.js` | `activeTab` 4값 확장, 테이블·액션·리밸런싱 헬퍼 추가, 미사용 헬퍼 8종 제거 |
| `static/js/app.js` | `partialNames`에 신규 partial 4종 추가 |
| `static/index.html` | 신규 partial 마운트 지점 4개 추가 (셸 바로 다음 순서) |

### 추가한 헬퍼 (`portfolio.js`)

- 그룹/행: `getHoldingGroups`, `getDaysUntil`, `getRowQuantity`, `getRowAvgLabel`, `getRowCurrentLabel`, `getRowProfitRate`, `getRowWeight`
- 요약: `getTotalProfit`, `getTotalProfitRate`, `getUnlinkedStockCount`, `getLatestPriceCache`
- 섹션: `expandAllSections`, `collapseAllSections` (`toggleSection` 인자는 assetType → sectionKey)
- 액션 노출 규칙: `canKeyword`, `canFinancial`, `canPurchase`, `canSell`, `canDelete`, `getAnalysisTargets`
- 리밸런싱: `_rebalanceEntries`, `getRebalanceActions`, `getBandExceededCount`, `getRebalanceMoveTotal`, `getRebalanceMoveNote`, `adjustBand`, `copyRebalanceChecklist`

### 제거한 헬퍼 (테이블 전환으로 미사용)

`getStockPriceSummary`, `getProfitRate`, `getSubTotalInvested`, `getSubTotalEvalAmount`, `getSubTotalProfitRate`, `isCashGroupStart`, `getCashGroupSummary`, `getNewsEnabledCount`

- 카드형 목록에서 한 줄 요약으로 쓰던 헬퍼들이다. 테이블에서는 열 단위 포맷터(`getRow*`)와 그룹 소계(`getHoldingGroups`)가 같은 정보를 담당한다.
- 제거 후 `static/` 전체에서 잔여 참조 없음을 grep 으로 확인했다.

### 그룹 구성

`getHoldingGroups()`가 자산군 순서대로 그룹을 만든다.

- 주식은 `stockDetail.country` 기준으로 `STOCK_KR` / `STOCK_OVERSEAS` 두 그룹으로 **표시만** 분리 (`AssetType.STOCK` 유지 — brainstorm Open Question 3 결정)
- 현금성 자산은 예금/적금/CMA `subGroups`를 함께 반환해 서브 소계 행을 그린다
- 그룹 헤더에 건수 · 원금 · 평가액 · 평가손익 · 수익률 · 비중 표시

### 행 액션 노출 규칙 (실측 결과)

| 자산군 | 실제 노출 |
|---|---|
| 주식 KR/US 비ETF | 키워드 · 재무 · AI · 매수 · 매도 · 수정 · 삭제 (7) |
| 주식 ETF | 매수 · 매도 · 수정 · 삭제 (4) |
| 펀드 | 키워드 · 납입 · 수정 · 삭제 (4) |
| 현금성(예금·적금·CMA) | 납입 · 수정 · 삭제 (3) |
| 채권 · 부동산 · 금 · 암호화폐 · 기타 | 키워드 · 수정 · 삭제 (3) |

- **plan 표와의 차이**: plan 에는 금·원자재·암호화폐에도 매수/매도를, 모든 행에 `이력` 버튼을 두는 것으로 적었으나, 매수·매도 API 는 `/items/stock/**` 로 주식 전용이고 매수·납입 이력은 이미 각 모달 안에 들어 있다. 실제 백엔드가 지원하는 범위로 맞췄고 `이력` 버튼은 만들지 않았다.
- 최대 버튼 수가 7개로 줄어 관리 열은 `300px`(행 최소 폭 `1280px`)로 충분했다. plan 의 `390px` / `1470px` 확대는 불필요했다.
- 매도 이력이 있는 항목은 삭제 버튼 `disabled` + 안내 title (실측 3건 확인)

### 리밸런싱 제안 — 이동 금액 계산 정정

최초 구현은 상위 버킷(전체 대비)과 투자자산 내부(투자자산 총액 대비) 편차를 한 배열에 합쳐 `합계 / 2`를 총 이동 금액으로 썼다. 기준이 다른 두 레벨을 더해 같은 돈을 두 번 세는 문제가 실측에서 드러났다(9,909만원 표기).

- `_rebalanceEntries(level)`로 레벨을 분리하고, 각 항목에 `level` / `levelLabel`을 부여
- `getRebalanceMoveTotal()`은 **상위 버킷이 밴드를 벗어난 경우 버킷 기준 금액**을 쓰고, 버킷이 밴드 안이면 자산군 기준 합계/2로 대체
- 화면과 체크리스트 복사 텍스트에 `상위 배분` / `투자자산 내부` 라벨 표기
- 정정 후 실측: 총 이동 금액 3,224만원 (상위 버킷 기준)

### 분석 탭 구현 방식

plan 에는 재무상세 패널을 분석 탭으로 "이동"한다고 적었으나, 실제로는 `portfolio-deposit-financial.html`의 슬라이드 패널(약 700줄, 차트·리사이즈·localStorage 폭 저장 포함)을 **이동하지 않고 그대로 재사용**했다. 분석 탭은 좌측 종목 목록만 담당하고, 종목 클릭 시 `openStockDetail(item)`으로 기존 패널이 우측에서 열린다.

- 이유: 상태가 `portfolio` 객체에 얹혀 있고 partial 이 형제 노드로 마운트되는 구조라, 마크업을 옮기면 회귀 위험만 커지고 얻는 것이 없다
- 결과적으로 재무 필터(CFS/OFS · 보고서코드 · 공시 유형·기간 · SEC 분기/연간 · EDGAR 링크)는 전부 그대로 유지된다

## 검증

### 실행 환경

`node`가 이 머신에 설치돼 있지 않아 plan 의 `node --check`는 수행하지 못했다. 대신 브라우저에서 `new Function(src)`로 파싱 검증했다(OK, 93,010 bytes).

서버 기동 대신 **목 데이터 하네스**로 확인했다(로컬 실행 함정 메모의 #93 패턴).

- `static/` 전체를 스크래치패드에 복사 → `js/harness-mock.js`로 `API` 객체 내용 치환(`const API`라 재바인딩 불가 → `Object.assign`), `<head>`에 accessToken/userId 주입
- `python3 -m http.server 8099`로 서빙 후 브라우저 페인에서 확인
- 시드: 자산 10종 15건(국내주식 3 · 해외주식 2 · 채권 · 펀드 · 금 · 암호화폐 · 부동산 · 기타 · 현금성 4), 매도 3건, 배분 현황(밴드 이탈 5건)

### 확인 항목

| 항목 | 결과 |
|---|---|
| partial 5종 마운트 | PASS (portfolio / holdings / sales / targets / analysis 모두 children=1) |
| 4탭 전환 · 배지 카운트 | PASS (보유 15 · 매도 3 · 목표 배분 5 · 분석 4) |
| 헤더 시세 캐시 표기 | PASS ("3분 30초 전 데이터 · 11분 55초 후 갱신") |
| KPI 4장 | PASS (총 자산 3억 10만원 / +1,478만원 / +5.18% / 배당·이자 `—`) |
| 도넛 + 범례 | PASS (자산군 8종, 중앙 총 평가 정렬) |
| 그룹 소계 · 국내/해외 분리 | PASS (국내 3건 · 해외 2건, 각 원금·평가·손익·수익률·비중) |
| 현금성 서브그룹 | PASS (예금 1 / 적금 1 / CMA 2, 서브 소계 표시) |
| 행 셀 값 | PASS (수량·평단·현재가·평가액·손익·수익률·비중, USD 통화 표기, 만기 `D-207`, `수시입출`) |
| 액션 노출 규칙 | PASS (위 표와 일치, 15행 전수 확인) |
| 삭제 차단 | PASS (매도 이력 3건 disabled) |
| 미납 뱃지 | PASS (overdue 2건만 visible, 나머지 display:none) |
| 매도 이력 탭 | PASS (KPI 4 · 월별 그룹 · 기여율 · 수정/상세) |
| 목표 배분 탭 | PASS (편차 막대 · 밴드 ±1.5%p 조절 · 범례 · 암호화폐 제외 안내) |
| 리밸런싱 제안 | PASS (레벨 라벨 · 총 이동 금액 3,224만원 · 체크리스트 복사) |
| 분석 탭 → 재무 패널 | PASS (삼성전자 선택 시 DART 패널 "연도별 추세 / 공시" 정상 오픈) |
| 콘솔 에러 | 포트폴리오 관련 없음 (favorites/home/ecos 오류는 하네스 목이 빈 응답을 준 탓) |

### 미검증

- 실제 서버(`bootRun`) 기동 상태에서의 API 연동 — 하네스 목으로 대체
- 모바일 반응형 (테이블은 `overflow-x-auto` 가로 스크롤 전제)
- `adjustBand()` 실제 저장 (하네스에서는 `saveAllocationTarget` 목이 빈 응답)
- `copyRebalanceChecklist()` 클립보드 권한 동작

## Phase 2 — 뉴스 → 키워드 메뉴 이관 (완료)

### 변경 파일

| 파일 | 변경 |
|---|---|
| `static/js/components/portfolio.js` | 뉴스 상태 7종·메서드 9종 제거(176줄), `toggleNews` → 키워드 등록 전용 4종으로 교체 |
| `static/partials/portfolio.html` | 키워드 등록 모달 추가 |
| `static/partials/portfolio-holdings.html` | 키워드 버튼을 등록 전용으로 변경(등록된 종목은 `등록됨` 비활성) |
| `static/js/api.js` | 해외뉴스 래퍼 유지 사유 주석 추가 |

### 제거한 상태

`news`, `overseasNews`, `selectedNewsItemId`, `selectedNewsKeywordId`, `collectingItemId`, `_overseasNewsGeneration`, `_overseasNewsDebounceTimer`

### 제거한 메서드

`findKeywordIdByItemName`, `selectPortfolioNewsItem`, `loadPortfolioNews`, `collectPortfolioNews`, `toggleOverseasNews`, `switchOverseasNewsTab`, `loadOverseasNews`, `loadMoreOverseasNews`, `toggleNews`

### 추가한 메서드

`openKeywordModal`, `closeKeywordModal`, `submitKeyword`, `getKeywordRegion`

- 등록은 기존 `PATCH /api/portfolio/items/{itemId}/news` (enabled=true)를 그대로 사용 — 서버 변경 없음
- 서버 `PortfolioService.toggleNews`는 `keywordService.registerKeyword(itemName, item.getRegion(), userId)`로 키워드를 만든다. 키워드 메뉴가 읽는 `API.getKeywords(userId)`와 같은 저장소라 연동은 코드 수준에서 확인했다
- **정정**: `getKeywordRegion`을 처음에 `stockDetail.country`로 판정했는데, 서버가 쓰는 값은 `item.region`(`DOMESTIC`/`INTERNATIONAL`)이다. 모달에 실제 등록값과 다른 분류가 보일 수 있어 `item.region` 기준으로 바꿨다
- 키워드 **삭제**는 포트폴리오에 두지 않았다(브레인스토밍 결정). 서버의 `enabled=false` 분기는 API 완결성을 위해 그대로 둔다

### 남긴 것 (의도적)

`api.js`의 `getOverseasBreakingNews` / `getOverseasComprehensiveNews` 래퍼 2종은 이번에 호출부가 사라져 사용처가 없다. 백엔드 `/api/overseas-news/**` 는 그대로 살아 있고 키워드 메뉴 편입 가능성이 열려 있다.

- **태형님 결정 (2026-08-10, "유지해")**: 래퍼 유지
- 후속 리뷰·정리 작업에서 죽은 코드로 오인해 지우지 않도록 `api.js`에 사유 주석을 남겼다

### 검증

| 항목 | 결과 |
|---|---|
| 뉴스 심볼 잔여 참조 (`static/` 전체 grep) | PASS (0건) |
| 런타임 상태 제거 | PASS (`news`/`overseasNews`/`selectedNewsItemId` 모두 없음) |
| 키워드 버튼 상태 | PASS (등록된 2건 `등록됨` disabled, 나머지 `키워드` 활성) |
| 등록된 종목 재등록 차단 | PASS (`openKeywordModal`이 모달을 열지 않음) |
| 키워드 등록 모달 | PASS (안내 문구 · 키워드 · 분류 표시, 취소/등록 버튼) |
| 분류 표기 | PASS (국내주식 `국내`, 해외주식 `해외`, region 없는 항목 `국내` 폴백) |
| 콘솔 에러 | 포트폴리오 관련 없음 |

미검증: 실제 서버에서 등록 후 키워드 메뉴 목록에 반영되는지 (하네스 목이 `getKeywords`를 빈 응답으로 대체)

## Phase 3 — 연금 자산군 신설 (완료)

### 신규 파일

| 파일 | 내용 |
|---|---|
| `domain/model/enums/PensionSubType.java` | `IRP` / `PENSION_SAVING` / `DC` / `DB` |
| `domain/model/PensionDetail.java` | subType · provider · evaluatedAmount · monthlyDepositAmount · depositDay (평가액 음수 검증) |
| `infrastructure/persistence/PensionItemEntity.java` | `pension_detail` 테이블, `@DiscriminatorValue("PENSION")` |
| `application/dto/PensionDetailResponse.java` | 응답 DTO |
| `presentation/dto/PensionItemAddRequest.java` / `PensionItemUpdateRequest.java` | 요청 DTO |
| `db/migration/pension_detail_2026_08_10.sql` | 테이블 생성 + 롤백 주석 |

### 수정 파일 (백엔드)

| 파일 | 변경 |
|---|---|
| `AssetType.java` | `PENSION("연금")` 추가 (9종 → 10종) |
| `AssetClassification.java` | `SAFE_TYPES`에 `PENSION` 편입 → 배분에서 안전자산으로 자동 집계 |
| `PortfolioItem.java` | `pensionDetail` 필드·재구성 생성자 파라미터, `createWithPension`, `updatePensionDetail` |
| `PortfolioItemMapper.java` | toDomain 분기 · toEntity `case PENSION` · `resolveAssetType` |
| `PortfolioItemResponse.java` | `pensionDetail` 필드 |
| `PortfolioService.java` | `addPensionItem` / `updatePensionItem`, 납입 대상·미납 판정·`depositTargetIds` 필터에 PENSION 추가 |
| `PortfolioController.java` | `POST /api/portfolio/items/pension`, `PUT /api/portfolio/items/pension/{itemId}` |
| `PortfolioEvaluationService.java` | PENSION 평가 분기 (evaluatedAmount, 미입력 시 원금) |

### 수정 파일 (프론트)

`api.js`(add/updatePensionItem), `financial.js`(assetTypeConfig `PENSION` sky), `portfolio.js`(getEvalAmount·getItemSummary·assetTypeOrder·isDepositTarget·depositReminderMeta·add/edit 분기), `portfolio-add.html`·`portfolio-edit.html`(연금 폼), `portfolio-deposit-financial.html`(리마인더 뱃지를 `getAssetTypeLabel`로 일반화)

### 설계 메모

- 연금은 시세 연동 대상이 아니라 **평가액을 사용자가 직접 갱신**한다. `investedAmount`=납입 원금, `pensionDetail.evaluatedAmount`=평가액으로 분리해 평가손익이 표시된다
- `evaluatedAmount`가 null이면 원금으로 평가한다 (서버·프론트 동일 규칙)
- `AssetType` switch 가 exhaustive(`default` 없음)라 Mapper 누락이 컴파일 에러로 잡힌다 — 의도적으로 그대로 뒀다
- 기존 테스트 4개 파일의 `new PortfolioItem(...)` 재구성 생성자 호출 9곳에 `pensionDetail` 인자(null)를 추가했다. 레거시 오버로드를 남기지 않아 앞으로도 누락이 컴파일 단계에서 드러난다

### 검증

| 항목 | 결과 |
|---|---|
| `./gradlew compileJava` | PASS |
| `./gradlew compileTestJava` | PASS |
| `./gradlew test --tests "*Portfolio*"` | PASS (59 tests, 0 failures, 0 errors) |
| 자산군 그룹 노출 | PASS (`PENSION(2)` 그룹, 정렬 위치 CASH 앞) |
| 평가액·평가손익 | PASS (IRP 원금 14,200,000 → 평가 16,850,000, +2,650,000 / +18.66%) |
| 그룹 소계 | PASS (2건 · 원금 2,400만원 · 평가 27,270,000 · +13.63%) |
| 행 액션 | PASS (키워드 · 납입 · 수정 · 삭제) |
| 서브 요약 문구 | PASS (`IRP · 미래에셋`, `연금저축 · 한국투자 · 월 300,000원 · ⚠ 미납`) |
| 도넛 범례 | PASS (연금 8.3%) |
| 자산 추가 모달 | PASS (자산군 7종에 `연금` 노출, 유형 4종 · 운용사 · 평가액 · 월납입액 · 납입일) |
| 자산 수정 모달 | PASS (editForm에 subType·provider·evaluatedAmount 정상 로드) |

### 실행 중 수정

프론트 `getEvalAmount()`가 STOCK만 특수 처리해서 연금이 원금으로 평가됐다. `pensionDetail.evaluatedAmount`를 쓰도록 분기를 추가해 서버 평가 로직과 일치시켰다.

## Phase 4 — 신규 집계 4종 (완료)

### 신규 파일

| 파일 | 내용 |
|---|---|
| `domain/model/PortfolioSnapshot.java` | 일자별 스냅샷 도메인 (`create` / `refresh`) |
| `domain/repository/PortfolioSnapshotRepository.java` | 포트 |
| `infrastructure/persistence/PortfolioSnapshotEntity.java` | `portfolio_snapshot`, `(user_id, snapshot_date)` 유니크 |
| `infrastructure/persistence/PortfolioSnapshotJpaRepository.java` / `...RepositoryImpl.java` | 어댑터 |
| `application/PortfolioSummaryService.java` | 스냅샷 저장/조회 + 요약(CAGR·보유일수·환차손익) |
| `application/PortfolioIncomeService.java` | 배당·이자 집계 |
| `application/dto/PortfolioSnapshotResponse.java` / `PortfolioSummaryResponse.java` / `PortfolioIncomeResponse.java` | 응답 DTO |
| `db/migration/portfolio_snapshot_2026_08_10.sql` | 스냅샷 테이블 |
| `db/migration/stock_purchase_fx_rate_2026_08_10.sql` | 매수 환율 컬럼 |

### 신규 API

| 메서드 | 경로 | 응답 |
|---|---|---|
| GET | `/api/portfolio/summary` | `totalEvaluated · totalInvested · profit · profitRate · holdingDays · cagr · fxProfit · fxUnknownCount` |
| GET | `/api/portfolio/income/summary` | `monthAmount · yearEstimate · dividendYield · basis · excludedCount` |
| POST | `/api/portfolio/snapshots` | 오늘자 스냅샷 저장(같은 날 덮어쓰기) |
| GET | `/api/portfolio/snapshots?months=12` | 최근 N개월 (1~60 클램프) |

### 산출 규칙

- **CAGR** = `(평가액 / 원금)^(365 / 보유일수) − 1`. 보유일수는 `StockPurchaseHistory.purchasedAt` 최소값 기준이며, **30일 미만이면 null**(짧은 기간에 값이 폭주해 무의미)
- **환차손익** = `Σ 수량 × 매수단가 × (현재환율 − 매수환율)`. 매수 환율은 `investedAmountKrw / (수량 × 매수단가)`로 역산해 저장한다(프런트가 환율을 따로 보내지 않음). 환율 미기록 이력은 제외하고 `fxUnknownCount`로 노출
- **스냅샷**은 수동 저장만. 자동 적재 스케줄러는 범위 밖

### 배당·이자 — 계획과 다른 산출 방식 (확인 필요)

plan 은 "국내 주식 배당은 KSD 배당 일정 × 보유 수량"이었으나, 실제로는 **`StockDetail.dividendYield`(사용자 입력 시가배당률) × 평가액**으로 연 예상을 산출했다.

- 이유: KSD 조회는 종목당 1회 외부 API 호출이라 보유 종목 수만큼 지연·실패 위험이 붙는다. 화면 진입마다 호출하기엔 비용이 크고, 실패 시 KPI가 통째로 비어버린다
- 이자(예적금·채권)는 원금 × 금리로 결정적으로 계산한다
- **이달 금액은 연 예상액의 1/12(월 평균 환산)이며 실제 지급일 기준이 아니다.** 응답의 `basis = ESTIMATED_MONTHLY_AVERAGE`와 화면 캡션("월 평균 환산 기준")으로 명시했다
- 배당률·금리가 비어 있는 항목은 `excludedCount`로 센다
- KSD 실제 지급일 기준 집계는 후속 과제로 남긴다

### 프론트 연동

- `portfolio.js`: `loadSummary` · `loadIncomeSummary` · `loadSnapshots` · `saveSnapshot` · `getLatestSnapshotLabel` · `renderPortfolioTrendChart` 추가. `loadPortfolio`에서 await 없이 호출해 실패해도 목록 표시를 막지 않는다
- `portfolio-holdings.html`: KPI 4장을 실제 응답에 연결, 자산 추이 라인차트(총 자산 실선 + 투자원금 점선) + `스냅샷 저장` 버튼 + 범례
- `app.js`: 차트 정리 대상에 `trendChartInstance` 추가(페이지 이탈·partial 재마운트 양쪽)

### 실행 중 발견·수정한 문제

**1. 메서드 이름 충돌 (실제 버그)**
처음에 차트 렌더 함수를 `renderTrendChart`로 만들었는데, `salary.js`에 같은 이름이 이미 있었다. 컴포넌트들이 하나의 dashboard 객체로 병합되고 `salary.js`가 `portfolio.js`보다 뒤에 로드돼 **포트폴리오 쪽이 통째로 덮어써졌다**(차트가 그려지지 않음). `renderPortfolioTrendChart`로 분리했다.
재발 방지로 `js/components/*.js` 전체를 스캔해 4칸 들여쓰기 메서드 정의의 중복을 확인했고, 포트폴리오가 관련된 충돌은 이 1건뿐이었다(수정 후 0건).

**2. 캔버스 0 크기 렌더**
`x-show`가 아직 `display:none`인 시점에 차트를 만들면 캔버스가 0으로 잡힌다. 처음엔 `requestAnimationFrame` 재시도로 했다가, 탭이 백그라운드면 rAF가 호출되지 않아 `setTimeout(50ms)` + 재시도 10회 상한으로 바꿨다.

**3. 카드 높이 폭주**
`maintainAspectRatio: false` + `flex-1`이라 카드가 계속 늘어났다. 컨테이너 높이를 220px로 고정했다.

### 검증

| 항목 | 결과 |
|---|---|
| `./gradlew compileJava` / `compileTestJava` | PASS |
| `./gradlew test --tests "*Portfolio*"` | PASS (59 tests, 0 failures) |
| 메서드 이름 충돌 스캔 | PASS (포트폴리오 관련 0건) |
| KPI — 총 자산 | PASS (`3억 2,737만원` / `327,369,078원`) |
| KPI — 총 평가손익 | PASS (`+1,805만원` · `환차손익 +123만원 · 환율 미기록 2건 제외`) |
| KPI — 누적 수익률 | PASS (`+5.83%` · `CAGR +11.40% · 보유 1043일`) |
| KPI — 이달 배당·이자 | PASS (`+41만원` · `연 예상 494만원 · 시가배당 2.14%` · `미입력 3건 제외 · 월 평균 환산 기준`) |
| 자산 추이 차트 | PASS (스냅샷 6건, 총 자산 실선 + 투자원금 점선, 축 라벨 6개) |
| 스냅샷 라벨 | PASS (`마지막 저장 2026-08-10`) |
| 콘솔 에러 | 포트폴리오 관련 없음 |

### 미검증 / 리스크

- 실제 서버 연동(하네스 목으로 대체). 특히 `/api/portfolio/summary`의 환차손익은 보유 해외 종목마다 `stockPriceService.getPrice`를 호출해 **종목 수만큼 외부 호출**이 발생한다 — 실데이터에서 응답 시간 확인 필요
- 브라우저 페인이 숨겨진 상태에서는 캔버스 크기가 0으로 잡혀 차트가 좁게 그려졌다. `resize()` 호출 시 정상 복구되는 것을 확인했고, `responsive: true`의 ResizeObserver가 실제 브라우저에서는 자동 처리한다
- 마이그레이션 SQL 2건(`portfolio_snapshot`, `stock_purchase_history.fx_rate`)은 **실 DB 미적용**

## 다음 단계

review 단계. Phase 1~4 전체 diff에 대한 findings 정리.
