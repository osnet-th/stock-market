---
title: "feat: 기업 리포트 S-RIM 항상 표시·주가 추이 아래 배치 및 종목 변경 시 초기화"
type: feat
issue: 132
issue_url: https://github.com/osnet-th/stock-market/issues/132
status: draft
date: 2026-09-29
branch: claude/inspiring-wright-rljbsn
branch_exception: "클라우드 세션은 지정 브랜치에만 push할 수 있어 issue/132-{slug} 대신 세션 브랜치를 쓴다 (2026-09-29 태형님 확인, #131과 동일). validate-plan.sh의 branch 형식 검사 1건은 이 예외로 실패한다."
worktree: /home/user/stock-market-issue-132
worktree_note: "/home/user/stock-market을 가리키는 심볼릭 링크. checkpoint-guard.sh가 경로에서 이슈 번호를 읽기 때문에 둔다."
workflow_exception: "클라우드 세션에 compound-engineering(/ce:plan·/ce:work·/ce:review)이 없어 planning·briefing·review 게이트 절차를 수동 적용한다 (#131과 동일, 2026-09-29 태형님 확인)"
brainstorm: docs/brainstorms/2026-09-29-company-report-stock-change-srim-reset-brainstorm.md
test_plan_status: none
schema_plan_status: none
allowed_paths:
  - src/main/resources/static/js/components/company-report.js
  - src/main/resources/static/partials/company-report.html
  - docs/plans/2026-09-29-002-feat-company-report-srim-stock-change-reset-plan.md
  - docs/brainstorms/2026-09-29-company-report-stock-change-srim-reset-brainstorm.md
  - .claude/issues/132/**
blocked_paths:
  - src/main/java/**
  - src/main/resources/application.yml
  - src/main/resources/application-dev.yml
  - src/main/resources/application-prod.yml
  - src/main/resources/db/**
  - src/main/resources/static/js/components/stock-eval.js
  - src/main/resources/static/partials/stock-eval.html
  - src/main/resources/static/js/api.js
  - src/main/resources/static/index.html
  - src/main/resources/static/js/app.js
  - build.gradle
---

# 기업 리포트 S-RIM 항상 표시·배치 변경 및 종목 변경 시 초기화 (#132)

## 요약
- 새 리포트 작성 중 1단계에서 다른 종목을 고르면 S-RIM 입력과 채권 기준수익률 선택 상태를 비운다. 같은 종목을 다시 고르거나 수정·재개 모드일 때는 그대로 둔다.
- S-RIM을 체크로 켜는 선택 섹션이 아니라 항상 보이는 섹션으로 바꾸고, 작성 5단계에서 주가 추이 바로 아래로 옮긴다(태형님 추가 요청). 저장은 S-RIM 입력이 있을 때만 한다.
- #131에서 넣은 USD 전환 경고는 더 이상 뜰 수 없으므로 제거한다.
- 바뀌지 않는 것: 서버, 저장 구조, S-RIM 계산식, 상세 화면, 종목 평가 화면

## 작업 리스트
- [ ] U1 종목 변경 시 S-RIM 초기화
- [ ] U2 S-RIM 항상 표시, 입력이 있을 때만 저장
- [ ] U3 S-RIM 섹션을 주가 추이 아래로 이동
- [ ] U4 #131 USD 전환 경고 제거
- [ ] 브라우저 하네스 확인 → 체크포인트 CP1
- [ ] 검증 (태형님 선택 방식)

## 배경 / 현재 상태
- **종목 선택:** `companyReportSelectStock`(`company-report.js:633`)은 S-RIM을 건드리지 않는다.
- **종목을 바꿀 수 있는 때:** 새 리포트 작성 중 첫 임시저장 전뿐이다.
  - 수정·재개 모드에서는 1단계 이동이 막혀 있다(`companyReportGoStep`, `:788`).
  - 첫 임시저장 뒤에는 `mode`가 `edit`로 바뀐다(`_crPersist`, `:1002`).
- **S-RIM 섹션:** 체크(`srim.enabled`)로 켜는 선택 섹션이고, 5단계 맨 아래에 있다(`company-report.html:721~897`). 체크했을 때만 저장 요청에 S-RIM을 담는다(`_crBuildBody`, `:1153~1154`).
- **서버 검사:** `SrimValidator`는 S-RIM이 오면 작성 완료 때 필수 항목을 검사한다. 임시저장은 빈칸을 허용한다.
- **종목 평가:** 종목을 고를 때마다 이미 S-RIM을 초기화한다(`stock-eval.js:119`).

## 요구사항 원장
| ID | 요구사항 | 출처 | 이번 범위 | 근거 / 담당 |
|---|---|---|---|---|
| REQ-1 | 새 리포트 작성 중 다른 종목을 고르면 S-RIM 입력(지배주주지분·유통주식수·기준일·요구수익률·연도 행)을 초기화한다 | 이슈 작업 1·2 | 포함 | U1 |
| REQ-2 | 종목이 바뀌면 채권 기준수익률 선택 상태와 적용 근거를 초기화한다 | 이슈 작업 2 | 포함 | U1 |
| REQ-3 | 원화 종목에서 적용한 채권 기준수익률이 외화(USD) 종목 S-RIM에 남지 않는다 | 이슈 작업 3 | 포함 | U1, U4 |
| REQ-4 | 다른 종목으로 바꾸면 S-RIM 단계에 이전 종목의 입력값과 적용 근거가 남지 않는다 | 이슈 완료조건 1 | 포함 | U1 |
| REQ-5 | 같은 종목을 다시 고르면 기존 입력을 유지한다 | 이슈 완료조건 2 | 포함 | U1 종목 코드 비교 |
| REQ-6 | 수정·재개 모드에서는 기존 입력을 유지한다 | 이슈 완료조건 2 | 포함 | 수정·재개 흐름은 바꾸지 않는다(1단계 잠금 유지) |
| REQ-7 | 저장된 리포트의 S-RIM 데이터와 계산식은 바뀌지 않는다 | 이슈 완료조건 3 | 포함 | 서버·저장 구조는 바꾸지 않는다. 불러오기 경로 유지 |
| REQ-8 | S-RIM을 선택(체크)이 아니라 항상 보이게 한다 | 2026-09-29 태형님 추가 요청 | 포함 | U2 |
| REQ-9 | S-RIM은 입력했을 때만 저장한다. 비어 있으면 S-RIM 없이 저장·작성 완료할 수 있다 | 2026-09-29 태형님 결정 | 포함 | U2 |
| REQ-10 | S-RIM 섹션을 작성 5단계의 주가 추이(월봉) 바로 아래, 주가지표 위에 둔다 | 2026-09-29 태형님 추가 요청 | 포함 | U3 |
| REQ-11 | 초기화는 알림 없이 한다 | 2026-09-29 태형님 결정 | 포함 | U1 |
| REQ-12 | #131의 USD 전환 경고를 제거한다 | 2026-09-29 태형님 결정 | 포함 | U4 |
| REQ-13 | S-RIM 외 기업 리포트 입력(수기 입력·등급·파라미터)의 종목 변경 초기화 | brainstorm 제외 범위 | 제외 | 이슈 범위 밖 |
| REQ-14 | 상세 화면 섹션 순서 변경 | 2026-09-29 태형님 결정 | 제외 | 작성 화면만 옮긴다 |
| REQ-15 | 종목 평가 화면 변경 | brainstorm 제외 범위 | 제외 | 이미 종목 전환 시 초기화한다 |

## 핵심 기술 결정
**KTD1 — 초기화 지점은 종목 선택 한 곳이다.**
- `companyReportSelectStock`에서 새 종목 코드가 현재 `selected.stockCode`와 다를 때만 초기화한다. 같은 코드면 아무것도 하지 않는다.
- 이 함수가 종목을 바꾸는 유일한 경로다. 수정·재개 모드는 1단계가 잠겨 있다.
- 첫 선택(`selected` 없음)도 "다름"으로 보지만, 그때 S-RIM은 새 작성 시작(`_crResetForm`)으로 이미 비어 있어 결과가 같다.

**KTD2 — S-RIM 전용 초기화 헬퍼를 둔다: `_crSrimResetForStockChange()`**
- 계산 세대 번호(`_crSrimGeneration`)를 올려 진행 중인 S-RIM 계산 결과를 버린다.
- `srim`을 `_crSrimEmpty()`로 바꾼다. `rate`도 새 객체가 되므로 진행 중이던 금리 조회 응답은 화면에 나오지 않는다.
- `_crResetForm()`은 수기 입력·등급·파라미터까지 지우므로 쓰지 않는다.

**KTD3 — 포함 체크를 없애고, 입력 여부로 저장을 정한다.**
- `srim.enabled` 필드와 체크박스를 없앤다. 대상은 초기값, `_crSrimEmpty()`, `_crSrimPopulate`, partial이다.
- 저장 요청:
  - S-RIM 입력이 있으면 `srim: _crSrimInput()`, `clearSrim: false`
  - 없으면 `srim: null`, `clearSrim: true`
  - 요청 형식과 서버는 그대로다.
- 입력 판정 `_crSrimHasInput()`: 아래 중 하나라도 해당하면 입력으로 본다.
  - 지배주주지분·자본 기준일·유통주식수·주식수 기준일·요구수익률·비교 기준 주가·주가 기준일 중 공백이 아닌 칸이 있다.
  - ROE 연도 행이 하나 이상 있다. 행 추가는 S-RIM을 쓰겠다는 동작이고, 판정을 단순하게 유지하기 위해서다.
  - 금액 단위 선택이나 금리 조회·선택만으로는 입력으로 보지 않는다. 금리를 적용하면 요구수익률이 채워져 입력이 된다.
- 저장된 S-RIM이 있는 리포트를 수정하다 S-RIM 칸을 모두 비우면 S-RIM이 삭제된다. 지금의 체크 해제와 같은 결과다.

**KTD4 — S-RIM 섹션을 5단계 주가 추이 블록 바로 뒤로 옮긴다.**
- `<section>` 블록(현재 `company-report.html:721~897`)을 통째로 주가 추이 블록(`:516~525`) 뒤, 주가지표 앞으로 옮긴다.
- 섹션 안의 내용과 이벤트(`@input`/`@change` → `crSrimChanged()`)는 그대로 둔다.
- 체크박스 라벨 "S-RIM 적정주가 (선택)"은 제목 "S-RIM 적정주가"로 바꾼다.

**KTD5 — #131 USD 전환 경고를 제거한다.**
- `crSrimRateAppliedNote()`를 없앤다. 근거 표시(`company-report.html:749`)는 `srimRateAppliedNote(companyReport.srim)`와 초록 글씨로 되돌린다.
- 금리 조회·적용은 지금처럼 원화 종목만 가능하다(`crSrimRateEnabled`).

## 구현 단위
### U1 종목 변경 시 S-RIM 초기화
- `companyReportSelectStock(stock)`: `selected`를 바꾸기 전에 종목 코드가 다르면 `_crSrimResetForStockChange()`를 부른다.
- `_crSrimResetForStockChange()`를 추가한다(KTD2).

### U2 S-RIM 항상 표시, 입력이 있을 때만 저장
- `company-report.js`
  - 초기값(`:4`)과 `_crSrimEmpty()`(`:113`)에서 `enabled`를 뺀다.
  - `_crSrimPopulate`(`:475`)의 `s.enabled = true`를 뺀다.
  - `_crSrimHasInput()`을 추가하고, `_crBuildBody`(`:1153~1154`)가 이 판정으로 `srim`·`clearSrim`을 정하게 한다.
- `company-report.html`
  - 체크박스 라벨(`:723~724`)을 제목으로 바꾼다.
  - 표시 조건 `x-show="companyReport.srim.enabled"`(`:726`)를 없앤다.

### U3 S-RIM 섹션을 주가 추이 아래로 이동
- KTD4대로 블록을 옮긴다.

### U4 #131 USD 전환 경고 제거
- KTD5대로 헬퍼와 partial을 되돌린다.

## 단위 테스트 계획
- 테스트 작성: 작성하지 않는다(`test_plan_status: none`).
- 사유: 프론트엔드(JS·partial)만 바뀌고, 저장소에 JS 단위 테스트 도구가 없으며, 서버 변경이 없다.
- 대신 저장소 밖 브라우저 하네스로 확인한다. 실제 `company-report.js`와 partial을 쓰고 API만 가짜 응답으로 바꾼다(2026-09-29 태형님 확인).

## 검증
- **하네스 확인 항목 (CP1):**
  1. 원화 종목 A에서 S-RIM 입력·금리 적용·계산 뒤 종목 B(원화·USD 각각)를 고르면, 입력·연도 행·금리 상태·적용 근거·결과가 모두 비워진다.
  2. 같은 종목을 다시 고르면 S-RIM이 그대로 남는다.
  3. S-RIM 섹션이 체크 없이 보이고, 5단계에서 주가 추이 다음·주가지표 앞에 있다.
  4. 저장 요청 본문: S-RIM이 비어 있으면 `srim: null`, `clearSrim: true`다. 입력이 있으면 `srim`이 담기고 `clearSrim: false`다.
  5. 수정·재개로 불러오면 저장된 S-RIM이 채워지고, 1단계 이동은 여전히 막혀 있다.
  6. 원화 금리 적용 근거는 초록 글씨로 보이고, USD 전환 경고 문구는 없다.
- **verify 단계:** 검증 방식은 태형님이 고른다. 후보는 정적/문서 검증, bootRun 후 실제 화면 확인 등이다.

## 체크포인트
- **CP1 (U1~U4 후):** `scripts/checkpoint-guard.sh /home/user/stock-market-issue-132`로 경로를 검사하고, read-only 가드 리뷰와 하네스 결과를 브리핑한다.

## 위험과 완화
| 위험 | 완화 |
|---|---|
| S-RIM을 쓰지 않으려 했는데 재무 데이터 가져오기나 연도 추가로 입력이 생겨, 작성 완료 때 필수 검사에 걸림 | 서버 오류 문구로 빠진 항목이 보인다. 칸을 비우면 S-RIM 없이 저장된다 |
| 수정 중 S-RIM 칸을 모두 비우면 저장된 S-RIM이 삭제됨 | 지금의 체크 해제와 같은 동작이고, 칸을 직접 비워야만 생긴다 |
| 나중에 종목을 바꾸는 경로가 새로 생기면 초기화가 빠짐 | 초기화 헬퍼를 분리해 두고 유지보수 포인트로 기록한다 |

## 수정 범위
- **수정 가능:** allowed_paths (`company-report.js`, `company-report.html`, 이 plan·brainstorm, 이슈 폴더)
- **수정 금지:** blocked_paths (서버, 설정, 종목 평가 화면, `api.js`, `index.html`·`app.js`, `build.gradle`)

## 완료 정의
- REQ-1~12를 충족하고, REQ-13~15는 제외로 유지한다.
- 하네스 확인 항목 1~6이 통과한다.
- 서버·저장 구조·계산식·상세 화면·종목 평가 화면은 바뀌지 않는다.
