---
title: "feat: S-RIM 입력 자동 채움 · 기업 리포트 지배주주지분 노출 · 종목 평가 S-RIM 계산"
type: feat
issue: 124
issue_url: https://github.com/osnet-th/stock-market/issues/124
status: active
date: 2026-09-27
branch: issue/124-srim-autofill-owners-equity
worktree: /Users/thlee/Documents/personal/stock-market-issue-124
brainstorm: docs/brainstorms/2026-09-27-srim-autofill-owners-equity-brainstorm.md
test_plan_status: approved
schema_plan_status: none
allowed_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/companyreport/application/**
  - src/main/java/com/thlee/stock/market/stockmarket/stock/domain/model/StockQuantity.java
  - src/main/java/com/thlee/stock/market/stockmarket/stock/application/dto/StockQuantityResponse.java
  - src/main/java/com/thlee/stock/market/stockmarket/stock/application/StockFinancialService.java
  - src/main/java/com/thlee/stock/market/stockmarket/stock/infrastructure/stock/dart/DartFinancialAdapter.java
  - src/main/java/com/thlee/stock/market/stockmarket/stock/application/ValuationMetricService.java
  - src/main/java/com/thlee/stock/market/stockmarket/stock/application/ShareReportSelector.java
  - src/main/java/com/thlee/stock/market/stockmarket/stock/infrastructure/stock/sec/SecFinancialAdapter.java
  - src/test/java/com/thlee/stock/market/stockmarket/companyreport/application/**
  - src/test/java/com/thlee/stock/market/stockmarket/stock/application/**
  - src/test/java/com/thlee/stock/market/stockmarket/stock/infrastructure/stock/**
  - docs/plans/tests/2026-09-27-124-srim-autofill-owners-equity-test-plan.md
  - src/main/resources/static/js/components/company-report.js
  - src/main/resources/static/js/components/stock-eval.js
  - src/main/resources/static/partials/company-report.html
  - src/main/resources/static/partials/stock-eval.html
  - docs/plans/2026-09-27-001-feat-srim-autofill-owners-equity-plan.md
blocked_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/companyreport/domain/**
  - src/main/java/com/thlee/stock/market/stockmarket/companyreport/infrastructure/**
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/**
  - src/main/java/com/thlee/stock/market/stockmarket/chatbot/**
  - src/main/resources/db/**
---

# S-RIM 입력 자동 채움 · 지배주주지분 노출 · 종목 평가 S-RIM (#124)

> Notion Plan 페이지(2026-09-27)를 저장소 문서로 재구성하고 2026-09-28 보완 결정(미국 ROE·주가지표 반기 전환 범위 포함, 단위 테스트 작성)을 반영했다. 로컬 원본이 원격에 없어 재구성했으므로 `draft`로 두고 재승인을 받는다.

## 요약
기업 리포트가 이미 조회하는 전체재무제표에서 지배주주지분을 꺼내 화면에 드러내고, S-RIM 입력을 버튼 한 번으로 채운다. 주식총수 기준을 반기까지 넓히고, 종목 평가 화면에 저장하지 않는 S-RIM 계산 섹션을 붙인다. 새 외부 API는 없다. 예외는 주식총수 기준 보고서 선택으로, 기존 정기공시 목록 조회를 한 번 더 쓴다.

## 작업 리스트
- [ ] U1 지배주주지분·비지배지분·지배주주순이익 추출 + 스냅샷 노출 + schemaVersion 3
- [ ] U2 요약 표 행 추가 + ROE 기준 교체 + ROE 기준 전달 + 근거 산식·기준표 문구
- [ ] U2b 미국 리포트 ROE 기준 표기 + 기준 혼합 방지
- [ ] U3 결산기준일 어댑터 유실 복구
- [ ] U4 리포트 경로 주식총수 사업·반기 전환 + 폴백
- [ ] U4b 주가지표 서비스 주식총수 사업·반기 전환 + 챗봇 경로 회귀 확인
- [ ] U5 S-RIM 자동 채움 근거 스냅샷 노출
- [ ] U6 기업 리포트 자동 채움 버튼·라벨·출처
- [ ] U7 종목 평가 무저장 S-RIM 섹션
- [ ] 단위 테스트 (승인된 시나리오 기준, 구현 전 작성)
- [ ] 검증: 전후 수치 비교 (REQ-16)

## 요구사항 원장

| ID | 요구사항 | 출처 | 이번 범위 | 근거 / 담당 |
|---|---|---|---|---|
| REQ-1 | 재무제표 요약에 지배주주지분·비지배지분 행이 연도별로 표시된다 | 이슈 작업1·완료조건1 | 포함 | U1, U2 |
| REQ-2 | 재무지표 ROE를 지배주주 기준으로 교체한다. 행을 추가하지 않는다 | 이슈 작업1·완료조건2 | 포함 | U2 |
| REQ-3 | 계정 매칭은 재무제표 구분과 IFRS 표준 계정 ID 조합으로 한다 | 이슈 작업1 | 포함 | U1 |
| REQ-4 | 버튼 한 번으로 지배주주지분·전기말 지분·유통주식수·기준일이 채워진다 | 이슈 작업2·완료조건3 | 포함 | U5, U6 |
| REQ-5 | S-RIM 주식수는 자기주식이 차감된 합계 유통주식수를 쓴다 | 이슈 작업2·확정결정 | 포함 | U5 |
| REQ-6 | 주식수 라벨을 유통주식수로 바꾸고 값의 출처(행 구분·보고서 종류·기준일)를 표시한다 | 이슈 작업2·완료조건3 | 포함 | U6 |
| REQ-7 | 주식총수 조회를 사업·반기 중 최신 기준으로 전환한다. 분기는 제외한다 | 이슈 작업3·완료조건5 | 포함 | U4 |
| REQ-8 | DART 결산기준일이 도메인 모델까지 전달된다 | 이슈 작업3 | 포함 | U3 |
| REQ-9 | 종목 평가에서 저장 없이 S-RIM 3시나리오를 계산한다 | 이슈 작업4·완료조건6 | 포함 | U7 |
| REQ-10 | EPS·BPS·시가총액 산출 주식수는 보통주 기준으로 유지된다 | 이슈 확정결정·완료조건4 | 포함 | U4 (기존 조회 함수 불변) |
| REQ-11 | 기존 저장 리포트는 재무 새로고침 전까지 변하지 않는다 | 이슈 확정결정 | 포함 | U2 (ROE 기준 미표기 = 전체 기준), U6 (v2 버튼 비활성) |
| REQ-12 | 스냅샷 schemaVersion을 2에서 3으로 올린다 | 이슈 확정결정 | 포함 | U1 |
| REQ-13 | 미국 리포트 ROE는 지배주주 태그 기준을 유지하고 사용 기준을 표기하며 기준 혼합을 막는다 | 2026-09-28 범위 확대 (REQ-22와 한 쌍) | 포함 | U2b |
| REQ-14 | 반기 전환은 리포트 조립 경로와 주가지표 서비스 모두에 적용한다 | 2026-09-28 범위 확대 (REQ-21과 한 쌍) | 포함 | U4, U4b |
| REQ-15 | 종목 평가 S-RIM 입력도 버튼 한 번으로 채워진다 | 이슈 작업4 | 포함 | U7 |
| REQ-16 | 주식총수를 쓰는 파생 지표와 ROE의 변화가 확인된다 | 이슈 완료조건7 | 포함 | 검증 단계 전후 비교 |
| REQ-17 | 자동 채움은 누를 때만 동작하고 직접 입력한 칸을 덮어쓰지 않는다 | 이슈 작업2 | 포함 | U6, U7 |
| REQ-18 | ROE 근거 산식과 기준 표기는 스냅샷의 ROE 기준을 따른다 | 2026-09-28 보완 | 포함 | U2 |
| REQ-19 | 수익성 제안 등급 변화는 의도된 변화로 두고 기준표 문구에 지배주주 기준을 표기한다 | 2026-09-28 보완 | 포함 | U2 |
| REQ-20 | 종목 평가 S-RIM에 기준 주가 비교를 포함한다 | 2026-09-28 보완 | 포함 | U7 |
| REQ-21 | 주가지표 서비스의 반기 전환 (EPS·BPS·PER·PBR 반기 주식수) | 이슈 작업3 · 2026-09-28 포함 결정 | 포함 | U4b, 챗봇 경로 회귀 확인 |
| REQ-22 | 미국 리포트 ROE 지배주주 기준 | 이슈 작업1 해석 · 2026-09-28 포함 결정 | 포함 | U2b |
| REQ-23 | S-RIM 계산식·시나리오 변경 | 이슈 범위 제외 | 제외 | — |
| REQ-24 | 종목 평가 S-RIM 결과 저장 | 이슈 범위 제외 | 제외 | — |
| REQ-25 | 우선주 시가총액 차감 방식 | 이슈 범위 제외 | 제외 | 주식수 합산 방식 고정 |
| REQ-26 | 요구수익률 자동 산출·회사채 금리 연동 | 이슈 범위 제외 | 제외 | — |
| REQ-27 | ROE 추정 자동화 | 이슈 범위 제외 | 제외 | — |
| REQ-28 | 매매 대응 밴드 표시 | 이슈 범위 제외 | 제외 | — |
| REQ-29 | 컨센서스 API 연동 | 이슈 범위 제외 | 제외 | — |

## 핵심 기술 결정
- **KTD1 — 지배주주지분은 이미 조회 중인 전체재무제표에서 꺼낸다.** 전기말 지분도 같은 행의 전기 값을 쓴다.
- **KTD2 — 계정은 재무제표 구분 + IFRS 계정 ID 단독 매칭.** 추출 헬퍼의 이름 폴백을 비운다. 지배주주순이익은 손익·포괄손익 양쪽을 탐색한다.
- **KTD2b — ROE는 기존 행을 교체한다.** 분자·분모를 함께 바꾼다(기준 혼합 금지). ROA는 전체 기준 유지.
- **KTD2c — ROE 산출 기준을 스냅샷에 싣는다.** 값: 지배주주/전체. 지배주주 계정이 없는 종목(개별재무제표 등)은 전체 기준으로 폴백하고 그 사실을 기준 값으로 드러낸다. 값이 없는 v2 스냅샷은 전체 기준으로 본다.
- **KTD3 — S-RIM 주식수 조회를 기존 유통주식수 조회와 분리한다.** 기존(보통주 우선)은 그대로 둔다.
- **KTD4 — 주식총수 보고서 후보를 사업·반기로 제한한다.** (사업연도, 기간 종료월) 쌍으로 비교한다. 공시 조회 실패 또는 후보 없음이면 기준연도 사업보고서로 폴백한다.
- **KTD5 — 종목 평가 계산은 기존 S-RIM 계산 API를 재사용한다.**
- **KTD6 — 미국 경로와 주가지표 서비스도 이번 범위에 포함한다.** (2026-09-28 태형님 확정, 9/27 범위 최소 결정 번복) 주가지표 서비스는 U4의 보고서 선택 로직을 재사용하고, 호출처(기업 리포트·챗봇 컨텍스트) 회귀를 확인한다. 미국은 SEC 조회가 이미 지배주주 태그(`NetIncomeLoss`, `StockholdersEquity`)를 우선하므로 사용 태그에 따라 ROE 기준 값을 채우고 한쪽만 폴백되면 전체 기준으로 맞춘다.
- **KTD7 — 종목 평가 자동 채움은 기존 리포트 미리보기 API를 쓴다.** 버튼 클릭 시에만 호출하고 로딩·실패를 표시한다.
- **KTD8 — S-RIM 화면 요소는 공유하지 않고 복제한다.** 후속 S-RIM 작업은 두 화면을 함께 고친다.

## 구현 단위

### U1 지배주주 계정 추출·노출 (선행 없음)
- `SnapshotFinancialExtractor`: BS `ifrs-full_EquityAttributableToOwnersOfParent`, `ifrs-full_NoncontrollingInterests`, IS/CIS `ifrs-full_ProfitLossAttributableToOwnersOfParent` 시리즈 추출 (ID 단독).
- `ReportSnapshot.CURRENT_SCHEMA_VERSION` 3.

### U2 요약 표·ROE (선행 U1)
- 재무상태표 요약에 지배주주지분·비지배지분 행, 손익계산서 요약에 지배주주순이익 행 추가.
- ROE = 지배주주순이익 ÷ 지배주주지분. 한쪽이라도 없으면 기존 전체 기준으로 폴백하고 ROE 기준 값에 반영.
- `company-report.js`: 근거 산식(현재 `'ROE = 당기순이익 ÷ 자본총계'`)과 원천 행 목록을 ROE 기준 값으로 분기. 기준표 E 조건·판정 note에 지배주주 기준 표기.
- 수익성 제안 등급 계산 로직과 임계값은 바꾸지 않는다.

### U2b 미국 리포트 ROE (선행 U2)
- SEC 순이익·자본 조회에서 어떤 태그가 쓰였는지 연도별로 전달한다 (지배주주 태그 / 비지배 포함 폴백 태그).
- 두 값 모두 지배주주 태그면 ROE 기준 값 = 지배주주. 한쪽이라도 폴백 태그면 분자·분모를 모두 전체 기준 태그로 맞추거나, 불가하면 ROE를 비우지 않고 전체 기준으로 표기한다.
- 태그 의미는 구현 전 실제 종목 1~2개로 실측 확인한다.

### U3 결산기준일 복구 (선행 없음)
- `DartStockTotqyItem.stlm_dt`를 `StockQuantity` → `StockQuantityResponse`까지 전달.

### U4 주식총수 사업·반기 전환 (선행 U3)
- 보고서 선택은 순수 클래스 `ShareReportSelector`(stock/application, 신규)로 분리해 U4b와 공유한다.
- `KrReportSnapshotAssembler`: 정기공시 목록으로 사업·반기 중 최신 (연도, 보고서 코드) 쌍을 고른다. 공시 조회는 호출 스레드에서 먼저 수행하고 결과만 병렬 태스크에 넘긴다.
- 실패·후보 없음 → (기준연도, 사업보고서) 폴백.
- 기존 보통주 우선 유통주식수 계산 함수는 수정하지 않는다.

### U4b 주가지표 서비스 반기 전환 (선행 U4)
- `ValuationMetricService`의 사업보고서 고정 주식총수 조회를 U4의 사업·반기 선택 로직으로 바꾼다. 실패·후보 없음이면 사업보고서 폴백.
- EPS·BPS 분자(연간 순이익·자본)는 기존 기준을 유지하고 주식수만 최신 기준이 된다. 주식수 기준일을 경고·근거에 남긴다.
- 호출처 회귀 확인: 기업 리포트 주가지표, 챗봇 컨텍스트. 챗봇 코드는 수정하지 않는다.

### U5 자동 채움 근거 노출 (선행 U1, U3, U4)
- 스냅샷에 S-RIM 근거를 추가: 지배주주지분 값·기준일(ISO), 연도별 기말 지배주주지분, 합계 유통주식수·주식수 기준일·보고서 종류·행 구분.
- 지배주주지분 기준 시점: 최신 정기보고서 컬럼(분기 포함) 우선, 없으면 기준연도 연간 값. **분기 컬럼 값은 구현 착수 전 실측으로 확인한다.**
- 합계 행이 없으면 주식수를 비우고 자동 채움에서 제외한다.

### U6 기업 리포트 자동 채움 (선행 U5)
- "재무 데이터 가져오기" 버튼. 자동 채움한 칸을 추적해 직접 고친 칸은 보존하고, 자동으로 채웠던 칸만 갱신한다.
- 금액은 현재 선택된 금액 단위로 환산해 넣는다.
- 연도 행: ROE 계산 모드 행에만 전기말 지분을 채운다(Y년 행 ← Y−1년 말). 값 없는 연도는 비움. 행 생성 없음. 예상 지분·예상 순이익은 채우지 않는다.
- 라벨 "총 주식수" → "유통주식수" (입력 폼, 상세 조회 두 곳).
- 출처(행 구분·보고서 종류·기준일)는 입력 화면에만 표시하고 저장하지 않는다.
- 스냅샷에 근거가 없으면(v2) 버튼 비활성 + "재무 새로고침 후 사용 가능" 표시.

### U7 종목 평가 S-RIM (선행 U5, U6)
- DART 탭 하위에 S-RIM 탭 추가. 입력·결과 요소는 U6을 복제한다.
- "재무 데이터 가져오기" → 리포트 미리보기 API 호출(클릭 시에만), 로딩·실패 표시.
- 기준 주가도 같은 응답의 주가지표로 채워 비교를 표시한다.
- 계산은 기존 S-RIM 계산 API. 결과를 저장하지 않는다.

## 시스템 전반 영향
- **ROE 변화(의도)** — 지배주주 기준으로 바뀐다. 수익성 제안 등급은 ROE 경계값(0/5/10/15%) 근처 종목에서 바뀔 수 있다. 이미 저장된 등급은 적용 버튼 없이는 바뀌지 않는다.
- **반기 전환으로 바뀌는 값** — 시가총액·PSR·PCR·EV/EBITDA·자기주식수·EPS·BPS·PER·PBR. 챗봇 컨텍스트의 주가지표도 함께 바뀐다.
- **기준 혼재** — 반기 주식수 ÷ 연간 이익·매출(EPS·PSR 등), 주당 지표 보통주 vs S-RIM 합계. 출처·기준일 표시로 구분한다.
- **미국 ROE** — 지배주주 태그 기준을 명시하고, 폴백 태그 종목은 전체 기준으로 표기한다.

## 위험과 완화
| 위험 | 완화 |
|---|---|
| 기존 유통주식수 조회를 합계로 바꿔 주당 지표 왜곡 | S-RIM용 조회 분리, 기존 함수 불변 |
| 계정명 매칭으로 자본과 순이익이 섞임 | 계정 ID 단독 + 재무제표 구분 |
| ROE 분자·분모 기준 혼합 | 둘 다 있을 때만 지배주주 기준, 아니면 전체 폴백 |
| 월값 단독 비교로 더 낡은 연도 조회 | (사업연도, 종료월) 쌍 비교 |
| 공시 조회를 병렬 태스크 안에서 호출해 풀 고갈 | 호출 스레드에서 먼저 수행 |
| 반기 선택 실패로 주식수 결손 | 사업보고서 폴백 |
| v2·미국 리포트에 틀린 산식 표시 | ROE 기준 값으로 분기 |
| 자동 채움이 사용자 입력을 덮어씀 | 직접 고친 칸 보존 |
| 단위 불일치로 금액이 틀리게 들어감 | 현재 금액 단위로 환산 |
| 주가지표 서비스 변경이 챗봇 응답에 영향 | 호출처 회귀 확인, 챗봇 코드 불변 |
| 미국 SEC 태그 의미 오해 | 구현 전 실측 확인 |
| 종목 평가 자동 채움 지연 | 클릭 시에만 호출, 로딩·실패 표시 |
| 화면 요소 복제로 두 화면이 어긋남 | 후속 작업은 두 화면을 함께 고친다 |

## 열린 질문
- 분기 컬럼의 지배주주지분 값 품질 — U5 착수 전 실측.
- 반기보고서가 없는 신규 상장 종목의 폴백 실동작.
- 합계 행이 없는 종목의 실제 존재 여부.
- 종목 평가 자동 채움 체감 지연 — 구현 단계 실측.

## 단위 테스트 계획
- 테스트 작성: 작성함
- 테스트 계획 문서: [테스트 계획](./tests/2026-09-27-124-srim-autofill-owners-equity-test-plan.md)
- 사용자 승인: 테스트 시나리오 승인됨 (2026-09-28)
- 승인된 테스트 시나리오: 정상 17건, 예외 6건
- 검증 명령: `./gradlew test --tests "*SnapshotFinancialExtractorTest" --tests "*ShareReportSelectorTest" --tests "*KrReportSnapshotAssemblerTest" --tests "*ValuationMetricServiceTest" --tests "*UsSnapshotFinancialExtractorTest" --tests "*DartFinancialAdapterTest"`

## 완료 정의
- REQ-1~22 충족, REQ-23~29는 제외로 유지.
- 국내 리포트에서 지배주주지분·비지배지분·지배주주순이익 행과 지배주주 기준 ROE가 보인다.
- S-RIM 자동 채움 버튼이 공통 칸과 ROE 계산 모드 연도 행을 채우고 출처를 표시한다.
- 반기보고서가 있는 종목이 반기 기준 주식수와 기준일을 쓴다.
- 종목 평가에서 S-RIM 3시나리오와 기준 주가 비교가 표시되고 저장되지 않는다.
- 주당 지표가 보통주 기준을 유지하고, 파생 지표·ROE 전후 변화가 기록된다.
