---
title: "feat: 연결 현금 자산 기준 주식 금융기관 적용"
type: feat
issue: 142
issue_url: https://github.com/osnet-th/stock-market/issues/142
status: active
date: 2026-10-06
approved: "2026-10-06 태형님 승인('권장대로 끝까지 한번에 진행해'): plan과 세부 사항(보조 문구, 다시 연결 때 기존 값 유지). 단위 테스트는 작성하지 않는다. 리뷰 수정 선택·GAP 결정·검증 방식도 권장안대로 진행하도록 지시함. PR 생성·병합은 별도 지시를 받는다"
workflow_exception: "클라우드 세션에 compound-engineering(/ce:plan·/ce:work·/ce:review)이 없어 planning·briefing·review 게이트 절차를 수동 적용한다 (#113·#136·#138·#140과 동일)"
branch: claude/inspiring-wright-rljbsn
branch_exception: "클라우드 세션은 지정 브랜치에만 push할 수 있어 issue/142-{slug} 대신 세션 브랜치를 쓴다 (#136·#138·#140과 동일). validate-plan.sh의 branch 형식 검사 1건은 이 예외로 실패한다."
worktree: /home/user/stock-market-issue-142
worktree_note: "/home/user/stock-market을 가리키는 심볼릭 링크. checkpoint-guard.sh가 경로에서 이슈 번호를 읽기 때문에 둔다."
brainstorm: docs/brainstorms/2026-10-06-stock-institution-linked-cash-brainstorm.md
test_plan_status: none
schema_plan_status: none
allowed_paths:
  - src/main/resources/static/js/components/portfolio.js
  - src/main/resources/static/partials/portfolio-add.html
  - src/main/resources/static/partials/portfolio-edit.html
  - src/main/resources/static/partials/portfolio-holdings.html
  - docs/plans/2026-10-06-003-feat-stock-institution-linked-cash-plan.md
  - docs/brainstorms/2026-10-06-stock-institution-linked-cash-brainstorm.md
  - .claude/issues/142/**
blocked_paths:
  - src/main/java/**
  - src/test/**
  - src/main/resources/application.yml
  - src/main/resources/application-dev.yml
  - src/main/resources/application-prod.yml
  - src/main/resources/db/migration/**
  - src/main/resources/static/js/api.js
  - src/main/resources/static/js/app.js
  - src/main/resources/static/js/components/home.js
  - src/main/resources/static/index.html
  - src/main/resources/static/partials/portfolio-deposit-financial.html
  - build.gradle
---

# 연결 현금 자산 기준 주식 금융기관 적용 (#142)

## 요약
- 연결 현금 자산에 금융기관이 있으면 주식의 금융기관은 그 값을 따른다. 보유 자산 표와 금융기관별 합계가 이 값을 쓴다.
- 이 경우 주식 등록·수정 창은 입력 칸 대신 "연결 계좌 기준: {금융기관}" 안내를 보여 주식에서 바꿀 수 없다.
- 화면에서만 계산하고 막는다. 서버 저장·검증·응답과 주식에 저장된 자기 값은 그대로다.
- 바뀌지 않는 것: 연결하지 않은 주식과 다른 자산군의 입력, 연결 현금 자산 구조(매수 금액 차감, 삭제 제한), 서버·DB·API

## 작업 리스트
- [ ] U1 적용 금융기관 계산과 표시: 계산 함수 2개, 금융기관별 합계와 보유 자산 표의 주식 행 → 체크포인트 CP1
- [ ] U2 등록·수정 창: 연결 현금 자산에 금융기관이 있으면 입력 칸 대신 안내, 저장 값 처리 → 체크포인트 CP2
- [ ] 리뷰 (태형님이 고른 이슈만 반영)
- [ ] 검증 (태형님 선택 방식)

## 배경 / 현재 상태
- **저장:** 금융기관은 `portfolio_item.institution` 하나를 모든 자산군이 같이 쓴다. 서버는 저장하고 응답에 담기만 한다(`PortfolioItem.java:261-268`). 챗봇·알림·스냅샷 등 서버의 다른 곳에서는 쓰지 않는다.
- **연결 현금 자산:** 주식 등록·수정 창에서 고른다(`portfolio-add.html:129-139`, `portfolio-edit.html:67-78`).
  - 주식 1건은 현금 자산 최대 1건에 연결된다. 목록 응답의 주식 항목에 `linkedCashItemId`가 온다(`PortfolioService.java:349-362`).
  - 연결된 현금 자산은 삭제할 수 없다(`validateCashDeletion`).
- **합계:** `getInstitutionTotals`가 항목의 `institution`만 보고 묶는다. 비어 있으면 "미지정"이다(`portfolio.js:868-894`).
- **표시:** 주식은 현금성 외 그룹 행에 나온다(현금성만 하위 그룹, `portfolio.js:838-852`). 이 행이 항목명 옆에 `item.institution`과 "연결 · {현금 자산 이름}" 배지를 보인다(`portfolio-holdings.html:285-295`).
- **입력 칸:** 등록·수정 창의 "금융기관" 칸은 자산군과 관계없이 같은 칸 하나다(`portfolio-add.html:398-408`, `portfolio-edit.html:308-317`).
  - 수정 창은 항목을 다시 읽어 `institution`과 `cashItemId`를 채운다(`portfolio.js:1990-1996`, `:2012`).
  - 저장은 폼 값을 그대로 보낸다(등록 `portfolio.js:1384`, 수정 `:2141`).
- **문제:** 주식을 키움 CMA에 연결해도 금융기관을 주식마다 다시 입력해야 하고, 비워 두면 합계에서 "미지정"으로 빠진다. #136에서 "주식도 자기 칸에 입력"으로 정한 결과다.

## 요구사항 원장
| ID | 요구사항 | 출처 | 이번 범위 | 근거 / 담당 |
|---|---|---|---|---|
| REQ-1 | 주식에 연결한 현금 자산에 금융기관이 있으면, 주식의 금융기관은 그 값을 따른다 | 이슈 작업 내용 1 | 포함 | U1, KTD1 |
| REQ-2 | 이 경우 주식 등록·수정 창에서는 금융기관을 바꿀 수 없게 한다 | 이슈 작업 내용 2 | 포함 | U2, KTD3 |
| REQ-3 | 보유 자산 표의 금융기관 표시와 금융기관별 합계가 이 규칙을 따른다 | 이슈 작업 내용 3 | 포함 | U1, KTD2 |
| REQ-4 | 연결 현금 자산에 금융기관이 있는 주식은 따로 입력하지 않아도 그 금융기관으로 보이고 합계에 잡힌다 | 이슈 완료 조건 1 | 포함 | U1, KTD1·KTD2 |
| REQ-5 | 이 경우 주식 등록·수정 창에서 금융기관을 바꿀 수 없다 | 이슈 완료 조건 2 | 포함 | U2, KTD3 |
| REQ-6 | 현금 자산의 금융기관을 바꾸면 연결된 주식도 함께 바뀐다 | 이슈 완료 조건 3 | 포함 | U1, KTD1(저장 없이 매번 계산) |
| REQ-7 | 연결하지 않은 주식과 다른 자산군은 지금처럼 자기 칸에 입력한다 | 이슈 완료 조건 4 | 포함 | U1·U2, KTD1·KTD3 |
| REQ-8 | 연결 현금 자산에 금융기관이 없으면 주식 자기 값을 쓰고, 그것도 없으면 "미지정"이다 | brainstorm 확인 1 | 포함 | U1·U2, KTD1·KTD3 |
| REQ-9 | 주식에 저장된 자기 값은 지우지 않고, 연결을 끊거나 현금 자산의 금융기관을 비우면 다시 쓴다 | brainstorm 확인 3 | 포함 | U2, KTD3 |
| REQ-10 | 보유 자산 표의 "연결 · {현금 자산 이름}" 배지는 그대로 둔다 | brainstorm 확인 4 | 포함 | U1(배지 코드 변경 없음) |
| REQ-11 | 서버에서도 막기(API 동작 변경) | brainstorm 확인 2 대안 | 제외 | 태형님 결정: 화면에서만 막는다(KTD4) |
| REQ-12 | 다른 자산군의 금융기관 입력 방식 변경 | 이슈 범위 밖 | 제외 | 계좌를 연결하는 구조가 없다 |
| REQ-13 | 연결 현금 자산 구조(매수 금액 차감, 삭제 제한) 변경 | 이슈 범위 밖 | 제외 | 이슈 범위 밖 |

## 핵심 기술 결정
**KTD1 — 적용 금융기관은 화면에서 계산한다(`portfolio.js`).**
- `getCashInstitution(cashItemId)`: 목록에서 그 id의 현금 자산을 찾아 금융기관을 돌려준다. id가 없거나, 현금 자산이 없거나, 금융기관이 비어 있으면 null이다. 폼 값(문자열 id)과 응답 값(숫자 id)을 모두 받는다.
- `getItemInstitution(item)`: 주식이고 `getCashInstitution(item.linkedCashItemId)`가 있으면 그 값, 아니면 항목에 저장된 값(없으면 빈 문자열)이다.
- 저장하지 않고 매번 계산하므로 현금 자산의 금융기관을 바꾸면 연결된 주식도 바로 바뀐다.

**KTD2 — 합계와 표가 같은 계산을 쓴다.**
- `getInstitutionTotals`는 `item.institution` 대신 `getItemInstitution(item)`으로 묶는다. 정렬·비중·"미지정" 규칙은 그대로다.
- 보유 자산 표의 현금성 외 그룹 행(`portfolio-holdings.html:290-291`)은 `getItemInstitution(item)`을 보인다. 현금성 하위 그룹 행(`:243-244`)에는 현금 자산만 나와 그대로 둔다.
- 합계 카드 안내 문구 조건(`:128`)과 자동 완성 목록(`getInstitutionNames`)은 저장 값 기준 그대로 둔다. 적용 값은 모두 어떤 항목(주식 자신 또는 연결 현금 자산)의 저장 값이라, 안내 문구 표시 여부가 같고 자동 완성 목록에도 이미 들어 있다.

**KTD3 — 등록·수정 창은 연결 현금 자산을 따르는 동안 입력을 받지 않는다.**
- 주식이고 `getCashInstitution(폼의 cashItemId)`가 있으면, 금융기관 입력 칸 대신 아래 안내를 보인다. 연결 현금 자산을 바꾸면 바로 다시 판단한다.
  - "연결 계좌 기준: {금융기관}"
  - 보조 문구 "연결한 현금 자산의 금융기관을 따릅니다. 바꾸려면 현금 자산을 수정하세요."
- 그 외(주식이 아님, 연결 안 함, 연결 현금 자산에 금융기관 없음)는 지금처럼 입력 칸을 보인다.
- 이 상태로 저장하면 금융기관 값은 등록 때 null, 수정 때 기존 저장 값(`editingItem.institution`)을 보낸다. 주식에서는 값이 바뀌지 않고, 저장된 자기 값도 지워지지 않는다(연결을 끊었다 다시 연결하며 입력한 값도 저장되지 않는다).

**KTD4 — 서버·API는 그대로다(화면에서만 막는다).**
- 저장·검증·응답이 바뀌지 않는다. API로 다른 값을 보내면 저장은 되지만, 연결 현금 자산을 따르는 동안 화면에 쓰이지 않는다.

## API 계약
- 경로·요청·응답 필드와 의미는 그대로다. 응답의 `institution`은 항목에 저장된 값이다.

## DB 스키마 (변경 없음)
`schema_plan_status: none`. 테이블·컬럼·Entity를 건드리지 않는다.

## 구현 단위
### U1 계산과 표시 (KTD1·KTD2)
- `portfolio.js`: `getCashInstitution`·`getItemInstitution` 추가, `getInstitutionTotals`의 묶음 기준 교체
- `portfolio-holdings.html`: 현금성 외 그룹 행의 금융기관 표시

### U2 등록·수정 창 (KTD3)
- `portfolio-add.html`·`portfolio-edit.html`: 금융기관 칸 자리에 안내와 입력 칸 전환
- `portfolio.js`: 주식 등록·수정 저장의 금융기관 값

## 시스템 전반 영향
- 연결된 주식 중 주식 값이 비었거나 연결 현금 자산과 다르던 항목은 표시와 합계가 연결 현금 자산 기준으로 바뀐다(의도).
- 홈·챗봇·메일·스냅샷은 금융기관을 쓰지 않아 영향이 없다.

## 위험과 완화
| 위험 | 완화 |
|---|---|
| 연결된 주식은 화면 값과 응답의 `institution`이 다를 수 있다 | 금융기관을 쓰는 곳이 화면뿐이다. 나중에 서버에서 쓰면 같은 규칙을 서버에도 둔다 |
| 연결을 끊으면 예전에 입력해 둔 주식 값이 다시 보인다 | 결정 사항(확인 3)이다. 끊은 뒤에는 입력 칸이 보여 바로 고칠 수 있다 |
| 목록에 없는 현금 자산에 연결된 경우 | `getCashInstitution`이 null을 돌려 주식 자기 값을 쓴다(`getLinkedCashName`과 같은 처리) |

## 단위 테스트 계획
- 테스트 작성: 작성하지 않음 (2026-10-06 태형님 확인, brainstorm 확인 5). 바뀌는 곳이 화면 JS·partial뿐이고 Java 코드는 바뀌지 않는다.
- 대신 체크포인트마다 저장소 밖 브라우저 하네스(실제 partial·`portfolio.js`)로 확인하고, 전체 `./gradlew test`로 회귀를 본다.

## 작업 진행 방식 (워크플로우 예외)
이 세션에는 compound-engineering(`/ce:work`·`/ce:review`)이 없어 같은 절차를 수동으로 적용한다(#113·#136·#138·#140과 동일).
- **구현:** 작업 리스트 순서대로 진행한다. 단위마다 변경 파일이 allowed_paths 안이고 blocked_paths 밖인지 확인한다.
  - 2026-10-06 태형님의 "권장대로 끝까지 한번에 진행해" 지시로 단위별 확인 없이 이어서 진행한다. 리뷰 수정 선택·GAP 결정·검증 방식은 권장안대로 하고 결과를 마지막에 함께 보고한다. 경로 위반이나 범위 밖 변경(SCOPE_CREEP)이 나오면 즉시 멈추고 보고한다. PR 생성·병합은 별도 지시를 받는다.
- **체크포인트:** CP마다 `checkpoint-guard.sh`로 경로를 검사하고 read-only 가드 리뷰를 수행한다.
- **리뷰:** 리뷰 게이트 문서 기준으로 finding 표를 만들고, 태형님이 고른 이슈만 수정한다.

## 체크포인트
- CP1 (U1 후): 하네스로 확인한다.
  - 적용 금융기관: 연결 현금 자산에 금융기관 있음(주식 값과 다름)·없음, 미연결(값 있음·없음), 주식 외 자산
  - 합계: 연결된 주식이 현금 자산의 금융기관으로 묶이고, 현금 자산의 금융기관을 바꾸면 따라 옮겨 간다. "미지정"은 마지막이다.
  - 표: 연결된 주식 행에 연결 현금 자산의 금융기관과 "연결 · …" 배지가 보인다.
- CP2 (U2 후): 하네스로 확인한다.
  - 등록 창: 금융기관 있는 현금 자산 연결 시 안내, 금융기관 없는 현금 자산·연결 안 함·주식 외 자산은 입력 칸
  - 저장 본문: 등록은 null, 수정은 기존 저장 값. 연결을 끊고 입력하면 그 값을 보낸다. 끊었다 다시 연결하면 기존 값을 보낸다.
  - #138·#140 화면 하네스 회귀, 콘솔 오류 없음

## 검증
검증 방식은 verify 단계에서 태형님이 고른다. 후보:
- `./gradlew test`: 임시 Postgres로 실행할 수 있다(Java 변경 없음, 회귀 확인).
- `bootRun` + 브라우저 확인: 임시 Postgres로 기동하고 API로 현금 자산·주식을 만든 뒤, 실제 화면에서 표·합계·등록·수정 창을 확인한다.
- 운영 반영 후 확인(태형님): 연결된 주식이 현금 자산의 금융기관으로 보이고, 수정 창에서 바꿀 수 없는지 본다.

## 수정 범위
- **수정 가능:** 위 allowed_paths 범위(`portfolio.js`, 등록·수정·보유 자산 partial, 이 plan과 brainstorm, 이슈 작업 파일)
- **수정 금지:** 위 blocked_paths 범위
  - 서버 코드 전체와 테스트, 설정 파일, SQL
  - `api.js`·`app.js`·`home.js`·`index.html`, 납입 창 partial, `build.gradle`

## 완료 정의
- REQ-1\~10을 충족하고, REQ-11\~13은 제외로 유지한다.
- 연결 현금 자산에 금융기관이 있는 주식은 따로 입력하지 않아도 그 금융기관으로 표와 합계에 보이고, 등록·수정 창에서 바꿀 수 없다.
- 연결하지 않은 주식과 다른 자산군, 서버 동작은 지금과 같다.
