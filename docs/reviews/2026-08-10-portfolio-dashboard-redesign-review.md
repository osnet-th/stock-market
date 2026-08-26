# 포트폴리오 화면 대시보드형 재설계 Review (#110)

gate: docs/gates/2026-08-10-portfolio-dashboard-redesign-gates.md
plan: docs/plans/2026-08-10-001-feat-portfolio-dashboard-redesign-plan.md
work: docs/works/2026-08-10-portfolio-dashboard-redesign-work.md

대상: Phase 1~4 전체 diff (54 파일, +3,300 / −884)

---

## Findings

### High

**H1. 초기 확장 섹션 키 불일치 — 주식 그룹만 접힌 채 시작** *(수정 완료)*

`portfolio.js:168` `loadPortfolio()`가 `expandedSections`를 `item.assetType`(`'STOCK'`)로 채우는데, 테이블 그룹 키는 `getHoldingGroups()`의 `group.key`(`'STOCK_KR'` / `'STOCK_OVERSEAS'`)다. 주식만 키가 어긋나 최초 진입 시 **국내·해외 주식 그룹은 접히고 나머지 자산군은 펼쳐진** 상태가 됐다.

- 조치: 초기 섹션을 `getHoldingGroups()` 기준으로 생성
- 재검증: 그룹 10종(`STOCK_KR`·`STOCK_OVERSEAS`·`BOND`·`REAL_ESTATE`·`FUND`·`OTHER`·`CRYPTO`·`GOLD`·`PENSION`·`CASH`) 모두 `true` 확인

**H2. 분석 탭 이탈 시 재무 패널 Chart 인스턴스 누수** *(수정 완료)*

`portfolio.js` `setActiveTab()`이 `portfolio.selectedStockItem = null`로만 패널을 닫아, `financialChartInstance`·`_secChartInstance`와 타임라인/공시 상태가 그대로 남았다. 탭을 오갈 때마다 Chart 인스턴스가 쌓인다.

- 조치: `closeStockDetail()` 호출로 교체 (차트 destroy + 상태 초기화 경로 재사용)
- 재검증: 패널 오픈 → 탭 전환 후 `financialChartInstance === null`, `selectedFinancialMenu === null` 확인

> work 단계에서 이미 잡아 고친 것 3건은 기록만 남긴다 — `renderTrendChart` 이름 충돌(salary.js가 덮어씀), 리밸런싱 총 이동 금액 중복 계상(레벨 혼합), 프론트 `getEvalAmount()`의 PENSION 분기 누락.

### Medium — 태형님 지시(2026-08-10 "다 고쳐줘")로 M1~M3 수정 완료

**M1. 보유 화면 1회 로드에 서버측 전체 평가가 2회 중복 실행** *(수정 완료)*

`loadPortfolio()`가 `loadSummary()`와 `loadIncomeSummary()`를 각각 호출하고, `PortfolioSummaryService`·`PortfolioIncomeService`가 **각자** `portfolioEvaluationService.evaluatePortfolios(List.of(userId))`를 부른다. 여기에 프론트의 `loadStockPrices()` 벌크 호출까지 더하면 한 번의 화면 진입에 평가 2회 + 시세 벌크 1회다.

- 완화 요인: 시세는 서버 캐시(TTL)로 보호된다
- **조치**: `/income/summary` 엔드포인트를 없애고 `/summary` 응답에 `income` 을 중첩. `PortfolioIncomeService.summarize(items, evaluation)` 로 바꿔 요약이 이미 구한 평가 결과를 재사용한다. 프론트도 호출 1회로 축소 → **평가 2회 → 1회**
- 참고: `/income/summary` 는 같은 PR 에서 추가돼 외부 사용처가 없으므로 제거해도 호환성 문제가 없다

**M2. `PortfolioSummaryService` 매수 이력 조회 N+1** *(수정 완료)*

`resolveHoldingDays()`와 `calculateFxProfit()`이 각각 항목마다 `purchaseHistoryRepository.findByPortfolioItemId(item.getId())`를 호출했다. 주식 N종이면 최대 2N 쿼리다.

- **조치**: 포트에 `findByPortfolioItemIdIn(List<Long>)` 추가(JPA 파생 쿼리 + Impl). `loadPurchaseHistories()` 가 한 번에 조회해 `itemId` 로 그룹핑하고 두 계산이 그 맵을 공유한다 → **최대 2N 쿼리 → 1회**

**M3. 환차손익이 해외 종목마다 시세 API를 개별 호출** *(수정 완료)*

`fetchCurrentFxRate()`가 종목당 `stockPriceService.getPrice(...)`를 불렀다. 환율은 종목이 아니라 통화 단위 값이다.

- **조치**: `fetchFxRatesByCurrency()` 가 통화별 대표 종목 하나만 조회해 같은 통화의 나머지 항목에 재사용한다 → **해외 종목 N개 → 통화 종류 수(현실적으로 1~2회)**

**M4. 배당 집계가 계획과 다른 산출 방식** *(후속 이슈로 분리 — #113)*

plan 은 "국내 주식 배당은 KSD 배당 일정 × 보유 수량"이었으나 구현은 `StockDetail.dividendYield × 평가액`이고, 이달 금액은 연 예상의 1/12(월 평균 환산)이다. 화면 캡션과 `basis = ESTIMATED_MONTHLY_AVERAGE`로 기준을 노출하고는 있으나 "이달 배당·이자"라는 라벨과 실제 의미가 완전히 일치하지는 않는다.

- 태형님 결정(2026-08-10): KSD 전환은 외부 API 캐싱·폴백 설계가 필요해 **별도 이슈로 분리**
- 후속: [#113 포트폴리오 배당 집계를 KSD 실지급일 기준으로 전환](https://github.com/osnet-th/stock-market/issues/113)
- 이번 범위에서는 현행 방식을 유지하되 `basis` 와 화면 캡션으로 기준을 명시한 상태로 둔다

### Low — L1~L4 수정 완료, L5는 의도

**L1. `투자원금` 캡션이 환차손익으로 대체됨** *(수정 완료)*
총 평가손익 카드의 보조 줄이 `summary` 로드 전에는 `투자원금 …`, 로드 후에는 `환차손익 …`으로 바뀌었다. `fxProfit`은 항상 non-null(0 기본)이라 **원화 자산만 가진 사용자는 늘 "환차손익 +0원"만 보고 투자원금은 보지 못했다.**

- 조치: 투자원금을 항상 표시하고, 환차손익은 `hasFxProfitInfo()`(`fxProfit != 0 || fxUnknownCount > 0`)일 때만 아랫줄에 덧붙인다

**L2. 매수 이력 수정에서 환율을 지울 수 없음** *(수정 완료)*
`StockPurchaseHistory.update()`가 `fxRate != null`일 때만 반영해 잘못 입력한 환율을 되돌릴 수 없었다.

- 조치: null 을 넘기면 기록을 지우도록 변경(양수 검증은 non-null 일 때만)

**L3. 키워드 버튼 노출 범위** *(수정 완료)*
`canKeyword()`가 기존 규칙(`assetType !== 'CASH' && subType !== 'ETF'`)을 그대로 옮긴 것이라 **신설된 연금**에도 `키워드` 버튼이 떴다.

- 조치: `PENSION` 을 `CASH` 와 함께 제외. 채권·부동산·금은 기존 동작이라 이번 범위에서 건드리지 않았다

**L4. `getHoldingGroups()` 반복 호출** *(수정 완료)*
템플릿의 `x-for`, 배지, `expandAllSections()`, 초기 섹션 구성에서 각각 호출되고 내부에서 그룹마다 `reduce`를 돌았다.

- 조치: `_holdingGroupsCache` 로 메모이제이션하고, `items`·`stockPrices` 가 바뀌는 지점(`loadPortfolio`·`loadStockPrices`)에서만 `invalidateHoldingGroups()` 로 무효화

**L5. 연금은 배당·이자 집계에서 제외** *(의도 — 수정 안 함)*
`PortfolioIncomeService.resolveInterestRate()`가 PENSION에 null을 돌려 집계·`excludedCount` 양쪽에서 빠진다. 연금 수익은 평가액에 이미 반영되므로 이중 계상을 막는다는 점에서 타당하나, 의도임을 문서에 남긴다.

---

---

## 2차 리뷰 — 수정 코드 자체에 대한 재검토

M/L 수정 diff 를 다시 훑어 **수정이 만든 새 결함 2건**을 찾았다. 둘 다 수정 완료.

**R3. L2 수정이 매수 이력의 환율을 지워버림** *(수정 완료)*

L2 에서 `StockPurchaseHistory.update()` 가 `fxRate` 를 항상 반영하도록 바꿨는데, **매수 이력 수정 폼에 환율 입력란이 없다.** `submitEditHistory()` 가 `fxRate` 를 보내지 않으므로 서버에서 `null` 로 덮어써져, 메모만 고쳐도 기록된 환율이 사라진다 → 환차손익에서 해당 이력이 통째로 빠진다.

- 조치: `editHistoryForm` 에 `fxRate` 추가, `startEditHistory()` 가 기존 값을 채우고 `submitEditHistory()` 가 그대로 돌려보낸다. 폼에는 **해외 주식일 때만** 입력란을 노출하고(`priceCurrency !== 'KRW'`), 비우면 기록 삭제임을 placeholder 로 안내
- 재검증: 기존 환율 `1310` 로드 확인, 미기록 이력은 빈 값 확인

**R4. M3 수정이 통화 전체를 날릴 수 있음** *(수정 완료)*

M3 에서 통화별 **대표 종목 1개**만 조회하도록 바꿨는데, 하필 그 종목 시세 조회가 실패하면 **같은 통화의 모든 항목이 환차손익에서 빠진다.** 수정 전에는 항목마다 각자 조회해서 하나 실패해도 나머지는 살았다.

- 조치: 통화별로 종목 목록을 들고, 성공할 때까지 다음 종목으로 넘어간다. 정상 경로에서는 여전히 통화당 1회 호출

**추가 점검 — L4 캐시 무효화 누락**

`loadPortfolio()` 의 **catch 경로**에서 `items = []` 로 비우면서 캐시를 무효화하지 않아, 로드 실패 후에도 옛 그룹이 화면에 남을 수 있었다 → `invalidateHoldingGroups()` 추가. `items`/`stockPrices` 를 쓰는 지점 5곳 전수 확인 완료.

---

## 수정 후 재검증 (2026-08-10)

| 항목 | 결과 |
|---|---|
| `./gradlew compileJava` · `compileTestJava` | PASS |
| `./gradlew test --tests "*Portfolio*"` | PASS (59 tests, 0 failures) |
| 컴포넌트 메서드 이름 중복 재스캔 | PASS (포트폴리오 관련 0건) |
| M1 — `/summary` 단일 호출로 income 수신 | PASS (`portfolio.income` 채워짐, `/income/summary` 호출 없음) |
| H1 — 초기 확장 섹션 | PASS (그룹 10종 모두 `true`) |
| H2 — 탭 이탈 시 패널 정리 | PASS (`financialChartInstance`·`selectedFinancialMenu` null) |
| L1 — 투자원금 상시 노출 | PASS (`투자원금 3억 932만원` + `환차손익 +123만원 · 환율 미기록 2건 제외`) |
| L1 — 원화만 보유 시 환차줄 숨김 | PASS (`hasFxProfitInfo() === false`) |
| L3 — 연금 키워드 버튼 제외 | PASS (연금 `false`, 주식 `true`) |
| L4 — 그룹 캐시 동작·무효화 | PASS (동일 참조 반환, 시세 재조회 후 재계산) |
| KPI 4장 · 자산 추이 차트 | PASS (기존 값 그대로 유지) |
| R3 — 매수 이력 환율 왕복 | PASS (기존 `1310` 로드, 미기록 이력은 빈 값) |
| R4 — 통화별 환율 폴백 | PASS (컴파일 통과, 통화별 종목 순회 로직 반영) |
| L4 — 로드 실패 경로 캐시 무효화 | PASS (`items`/`stockPrices` 변경 지점 5곳 전수 확인) |

---

## Open Questions / Assumptions

1. ~~**배당 집계 방식**(M4)~~ — **해소**: KSD 실지급일 기준 전환을 후속 이슈 [#113](https://github.com/osnet-th/stock-market/issues/113) 로 분리 (2026-08-10 태형님 결정)
2. **마이그레이션 3건 실 DB 적용 시점** — `pension_detail`, `portfolio_snapshot`, `stock_purchase_history.fx_rate`. 현재 미적용
3. **스냅샷 자동 적재** — 지금은 수동 저장만. 사용자가 안 누르면 추이가 비어 있다. 스케줄러 도입 여부
4. **실서버 통합 검증** — 전 구간이 목 하네스 기반이다. 특히 M2·M3의 응답 시간은 실데이터에서만 확인 가능
5. **가정** — `investedAmountKrw`는 해당 매수 건의 원화 환산 총액이다(`calculateDeductionAmount` 용법 기준). 이 가정 위에서 매수 환율을 역산한다

---

## Change Summary

- **Phase 1** 포트폴리오 화면을 4탭(보유 자산/매도 이력/목표 배분/분석)으로 재구성. `portfolio.html`을 셸로 축소하고 탭 partial 4종 신설. 행 액션에 **수정** 추가 + 자산군별 노출 규칙 복원. 미사용 헬퍼 8종 제거
- **Phase 2** 뉴스 열람을 키워드 메뉴로 이관. 상태 7종·메서드 9종(176줄) 제거, 키워드 등록 전용 모달 신설
- **Phase 3** 연금 자산군(`AssetType.PENSION`) 신설. 도메인·Entity·DTO·마이그레이션 + API 2종, 안전자산 버킷 편입, 원금/평가액 분리
- **Phase 4** 집계 4종 추가 — 자산 추이 스냅샷(테이블·API 2종·라인차트), 배당·이자, CAGR·보유일수, 환차손익(`fx_rate` 컬럼)
- 검증: `compileJava`·`compileTestJava` PASS, `test --tests "*Portfolio*"` 59 tests 0 failures, 하네스 실측 Phase별 PASS
