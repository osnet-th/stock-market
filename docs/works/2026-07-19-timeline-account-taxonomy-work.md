# 재무 타임라인 계정 taxonomy 불일치 Work 기록

plan: docs/plans/2026-07-19-001-fix-timeline-account-taxonomy-plan.md
gate: docs/gates/2026-07-19-timeline-account-taxonomy-gates.md
issue: https://github.com/osnet-th/stock-market/issues/85

## 구현 요약 (조립기만 수정 — FinancialTimelineAssembler)

### Phase 1 — Bug A: IFRS id 접두사 정규화 (영업CF 2017·2018)
- `canonicalAccountId(id)` 신설: 구 `ifrs_X` → 신 `ifrs-full_X`(표준형)로 정규화. `ifrs-full_`·`dart_`·`-표준계정코드 미사용-`·기타는 그대로.
- `detailRowKey`: account_id를 canonical로 키잉 → 구/신 접두사 세부행이 **한 행으로 병합**(BS/IS/CF 전체). 병합 행 id가 표준 `ifrs-full_`이라 트리 빌더 앵커·BS 앵커·추출기 매칭이 그대로 동작.
- `findCashFlowAmount`(FCF): `canonicalAccountId(item.accountId())` 비교 → 2017·2018 영업CF·유형/무형취득 인식(FCF도 복구).
- **설계 정정(plan 대비)**: 정규화 방향을 `ifrs-full_`(표준형)로 잡아 다운스트림(FinancialDetailTreeBuilder 앵커, SnapshotFinancialExtractor 상수 모두 `ifrs-full_`)이 유지됨 → **추출기·트리빌더 수정 불필요**. canonical은 조립기 private 메서드(단일 소스).

### Phase 2 — Bug B: 당기순이익 총계 보강 (2018~2022)
- `detailNetIncomeTotal(data)`: 전체재무제표 IS/CIS에서 이름에 "당기순이익" 포함 + "지배"·"비지배"·"계속영업"·"중단영업" 제외인 총계를 연도별 수집(id 무관, 무표준코드 연도 포함).
- `backfillNetIncome(rows, data)`: 요약(주요계정) 당기순이익 행의 **결측 연도만** `putIfAbsent`로 보강(기존 값 보존 = 회귀 최소). 요약에 당기순이익 행 자체가 없으면 상세 총계로 신설.
- `toSummaryRows`를 mutable 리스트로 바꿔 보강 적용.

## 변경 파일
- src/main/java/com/thlee/stock/market/stockmarket/stock/application/FinancialTimelineAssembler.java (유일)

## 영향/하위호환
- 정규화 방향이 표준형이라 기존 `ifrs-full_` 매칭 전부 유지. 구 `ifrs_` 행은 이제 표준 앵커에 정상 편입(개선).
- 당기순이익은 결측 연도만 채워 기존 표시값 불변(회귀 없음 설계).
- 공유 타임라인 → 종목 평가·재무 타임라인도 동일 개선.

## 리뷰 반영 (ce:review, 2026-07-19 태형님 결정 "F1 하드닝 + F4 nit")
- **F1 (MED, 당기순이익 총계 매칭 하드닝)**: `netIncomeTotalOf` 신설 — 표준 id `ifrs-full_ProfitLoss`(접두사 정규화) 우선 매칭, 없을 때만(무표준코드 연도) 이름 폴백. 이름 폴백에 **`주당` 제외** 추가 → "주당당기순이익"(EPS) 오매칭 방지. `isTotalNetIncome` → `netIncomeTotalOf` + `isTotalNetIncomeName`으로 분리.
- **F4 (nit)**: `detailNetIncomeTotal` → `detailNetIncomeByYear`(반환이 연도별 Map), 지역변수 `total`→`byYear`, "IS"/"CIS"/총계 id를 상수화(`INCOME_STMT_DIV`·`COMPREHENSIVE_INCOME_DIV`·`NET_INCOME_ID`), "연결 총계" 주석 완화.
- F2·F3·F5(LOW): 실질 위험 없음/설계 의도 부합으로 유지(리뷰 문서 기록).

## 범위 확장 (validation 중, 태형님 지적 "영업이익 없는데? 매출액도 같이" 2026-07-19)
- 현대차 검증 중 **영업이익 2018·2019 "—"** 발견 — 당기순이익과 동일 계열(주요계정 결측, 값은 전체재무제표에 존재). brainstorm Open Question #4(매출액·영업이익 등) 실현.
- 보강 로직을 **일반화**: `backfillNetIncome` → `backfillSummaryAmounts`(매출액·영업이익·당기순이익 3종). 계정별 `(요약 이름 prefix, 표준 id, 이름 매처)` 구성 — `backfillSummaryRow`/`detailTotalByYear`/`incomeStatementTotal` 공통화.
- 표준 id: 매출액 `ifrs-full_Revenue`·영업이익 `ifrs-full_ProfitLossFromOperatingActivities`·당기순이익 `ifrs-full_ProfitLoss`. 무표준코드 연도는 이름 폴백(매출액 "매출액"·영업이익 "영업이익", 둘 다 "률" 제외).
- 전부 `putIfAbsent` 결측 보강 → 기존 값 불변(회귀 안전 유지).

## 번들 추가 (무관 · 태형님 요청 "#85 worktree에 포함", 2026-07-19) — "조 2자리 버림" 통일
성격이 #85(타임라인)과 다른 프론트 표시 수정. **커밋은 별도로 분리** 예정.
- 공용 헬퍼 **`Format.truncTo2`** 신설(문자열 절단 `toFixed(6)`→2자리, 부동소수 오차 방지).
- **종목평가 억↔조 토글**: `stock-eval.js` `fmtAmountByUnit` — 조 변환 1자리 반올림 → 2자리 버림. `Format.truncTo2` 사용.
- **기업리포트**: `Format.compactNumber(value, trunc)`에 옵션 추가(기본 false=기존 1자리 반올림 유지 → **차트 축·재무 화면 불변**), `company-report.js` `crAmt`가 `trunc=true`로 조 2자리 버림.
- 검증(로직): 현대차 자본금 1,488,993,000,000 → **1.48조**(이전 1.5조), 45.206조→45.2, 1,862.54조(콤마), 종목평가 45.28→45.28. 앱 재기동 후 새 JS 서빙 확인.
- 범위: compactNumber 기본 동작 무변경(차트/재무 축 영향 없음), 기업리포트 crAmt만 버림 적용.

## work 검증
- `./gradlew compileJava` → exit 0 (리뷰 반영·범위 확장 후 재컴파일 포함). stock-eval.js는 정적 리소스(컴파일 무관).

## 미검증 (validation 단계 대상)
- 앱(wt-85) 기동 후 현대차 005380: 영업CF·당기순이익·FCF 2017~2026 전부 채워짐.
- 회귀: 삼성전자 등 기존 정상 종목 스냅샷 값 불변, 종목 평가 재무 타임라인 세부계정 트리 중복/누락 없음.
