# 기업 리포트 입력 개선 Work 기록

plan: docs/plans/2026-07-19-001-feat-company-report-input-improvements-plan.md
gate: docs/gates/2026-07-19-company-report-input-improvements-gates.md
issue: https://github.com/osnet-th/stock-market/issues/84

## 구현 요약 (Phase 1~3)

### Phase 1 — 연혁: 여러 줄 + 년/년-월
- `ReportManual.java`: 연혁 전용 `requireYearOrMonth`(YYYY 또는 YYYY.MM, 월 1~12) 추가, `validateListFields`의 연혁 검증을 `requireYear` → `requireYearOrMonth`로 교체. 급변 항목·예상 매출은 `requireYear`(4자리) 유지.
- `company-report.html`(작성): 연혁 content `<input>` → `<textarea rows=2 resize-y>`, year 입력 마스크 `YYYY`/`YYYY.MM` 허용(maxlength 7, 숫자+`.` 1개), 행 정렬 `items-start`.
- `company-report.html`(조회): 연혁 content `td`에 `whitespace-pre-wrap`(줄바꿈 보존), year `td`에 `align-top`.

### Phase 2 — 매입처 품목 칸 (Gate 1 = A: 공유 필드)
- `ReportManual.java`: `PartnerItem(name, share, note)` → `PartnerItem(name, item, share, note)`, 판매처·매입처 `requireFieldsWithin`에 `item` 포함(람다 변수 `item`→`row`로 정리).
- `company-report.js`: `_crNewRow` customers/suppliers 템플릿에 `item:''` 추가.
- `company-report.html`(작성): 판매처=제품·매입처=원자재 품목 `<input>` 추가(폭 조정 name flex-1/item w-24/share w-16/note w-24).
- `company-report.html`(조회): 판매처(제품)·매입처(원자재) 표에 품목 열 추가.

### Phase 3 — 주가지표 공식형 재계산 (Gate 3, 파생지표 전체)
- `ReportManual.java`: `MetricInputs`에 `netIncome`·`equity` 추가(순서: price, shares, netIncome, equity, revenue, operatingCf, eps, bps), `validateUnitAndMetricInputs`의 `requireAmounts`에 두 필드 포함. `CURRENT_SCHEMA_VERSION` 1 → 2.
- `company-report.js`:
  - 초기 상태·`_crResetForm` metricInputs에 `netIncome`·`equity` 추가(eps·bps는 폴백용 유지).
  - `_crBuildManual` 직렬화에 netIncome·equity 추가 + `schemaVersion: 2`.
  - `crMetricBase`: EPS=분자(당기순이익)÷유통주식수, BPS=자본총계÷유통주식수. 우선순위 = 명시 입력 → 구버전 직접입력(eps/bps) → 자동(스냅샷 당기순이익/자본총계)÷유통주식수 → 스냅샷 eps/bps. `netIncome`·`equity`(원)도 반환.
  - `_crAutoEquity` 신설(스냅샷 `priceMetrics.breakdowns.bps` 자본총계 term → 없으면 BPS×스냅샷 유통주식수, 유통주식수 편집과 무관하게 고정).
  - `crFormulaText` 신설(EPS·BPS 공식에 실제 대입값 표시, 구버전 직접입력은 "직접 입력값").
- `company-report.html`(계산기): EPS·BPS 입력 칸 → 당기순이익·자본총계(amountUnit) 입력으로 교체, "내 계산" 표에 EPS·BPS 행(공식+대입값+산출값) 추가, 안내문구 갱신.
- 조회 "내 계산" 블록(PER/PBR 등)은 `crCalcMetrics`가 새 EPS·BPS를 파생하므로 HTML 변경 없이 자동 반영.

## 변경 파일
- src/main/java/com/thlee/stock/market/stockmarket/companyreport/domain/model/ReportManual.java
- src/main/resources/static/js/components/company-report.js
- src/main/resources/static/partials/company-report.html

## 하위호환
- `ReportManualJsonConverter`: `FAIL_ON_UNKNOWN_PROPERTIES=false` + Java record → v1 JSON(신규 필드 없음)은 null로 매핑, 신규 JSON도 관대 처리. 데이터 마이그레이션 없음.
- v1 리포트: PartnerItem.item=null → 조회 빈 칸. MetricInputs의 legacy eps/bps는 폴백 우선순위로 보존.

## 리뷰 반영 (ce:review, 2026-07-19 태형님 결정)
- **F1 (공식 유지 + 안내문구)**: 계산기 안내문구에 "자동=보고 기준값이라 내 계산과 다를 수 있음" 추가(작성 intro + 조회 "내 계산" 라벨). 산식은 당기순이익·자본총계÷유통주식수 공식 유지.
- **적자(음수) 지원**: 백엔드 `requireSignedAmounts`(음수 허용) 신설 → netIncome·equity에 적용(price·shares·revenue·operatingCf·eps·bps는 기존 비음수 유지). 프론트 `crSignedAmountInput`(선행 마이너스 1개 허용) 신설 → 당기순이익·자본총계 입력에 연결.
- **F2·F3·F7 (헬퍼 추출·drift 해소)**: `_crPerShare(inputAmt, legacyPerShare, autoAmount, snapMetric, unit, shares)` 신설로 eps/bps 4단 폴백 사다리 통합. 반환에 `source`(input/legacy/auto/snapshot) 포함 → `crFormulaText`가 우선순위를 재복제하지 않고 source로 표시 결정(shares 미상 시 값 없는 공식만 표기).
- **F4 (kind guard)**: `crFormulaText`가 eps/bps를 source·label로 명시 분기(암묵적 else 제거).
- **F5 (월 정렬 정합)**: `crHistoryAsc`를 문자열 localeCompare → `연*100+월` 숫자 정렬로 교체(2024.3 vs 2024.12 순서 정상).
- **F6**: 폴백 존재로 유지(리뷰 기록).

## work 검증
- `./gradlew compileJava` → exit 0 (리뷰 반영 후 재컴파일 포함, 무관한 realestate 모듈 deprecation Note만 출력).
- JS: 런타임 미설치로 자동 구문검사 불가 → 변경 구간 육안 확인(브라우저 실동작은 validation 단계에서 수행 예정).

## 미검증 (validation 단계 대상)
- 앱/DB 기동 후 Chrome 실동작: 연혁 여러 줄·년월 저장/조회·정렬, 매입처 품목 저장/조회, 주가지표 유통주식수 변경 시 EPS·PER·PBR 연쇄 재계산, **적자(음수) 당기순이익 입력 및 음수 EPS**, v1 리포트 폴백, 자동 vs 내계산 divergence 안내문구.
