---
title: "feat: 만기 지난 현금성 항목의 납입 알림·자동 납입 표시 정리"
type: feat
issue: 140
issue_url: https://github.com/osnet-th/stock-market/issues/140
status: draft
date: 2026-10-06
workflow_exception: "클라우드 세션에 compound-engineering(/ce:plan·/ce:work·/ce:review)이 없어 planning·briefing·review 게이트 절차를 수동 적용한다 (#113·#136·#138과 동일)"
branch: claude/inspiring-wright-rljbsn
branch_exception: "클라우드 세션은 지정 브랜치에만 push할 수 있어 issue/140-{slug} 대신 세션 브랜치를 쓴다 (#136·#138과 동일). validate-plan.sh의 branch 형식 검사 1건은 이 예외로 실패한다."
worktree: /home/user/stock-market-issue-140
worktree_note: "/home/user/stock-market을 가리키는 심볼릭 링크. checkpoint-guard.sh가 경로에서 이슈 번호를 읽기 때문에 둔다."
brainstorm: docs/brainstorms/2026-10-06-expired-cash-deposit-reminder-brainstorm.md
test_plan_status: pending
schema_plan_status: none
allowed_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/CashDetail.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java
  - src/main/resources/static/js/components/portfolio.js
  - src/main/resources/static/partials/portfolio-add.html
  - src/main/resources/static/partials/portfolio-edit.html
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/CashDetailMaturityTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceDepositReminderTest.java
  - docs/plans/tests/2026-10-06-140-expired-cash-deposit-reminder-test-plan.md
  - docs/plans/2026-10-06-002-feat-expired-cash-deposit-reminder-plan.md
  - docs/brainstorms/2026-10-06-expired-cash-deposit-reminder-brainstorm.md
  - .claude/issues/140/**
blocked_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PortfolioItem.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/FundDetail.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PensionDetail.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/DepositHistory.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/repository/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioAutoDepositBatchService.java
  - src/main/java/com/thlee/stock/market/stockmarket/notification/**
  - src/main/java/com/thlee/stock/market/stockmarket/chatbot/**
  - src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java
  - src/main/resources/application.yml
  - src/main/resources/application-dev.yml
  - src/main/resources/application-prod.yml
  - src/main/resources/db/migration/**
  - src/main/resources/static/js/api.js
  - src/main/resources/static/js/app.js
  - src/main/resources/static/js/components/home.js
  - src/main/resources/static/index.html
  - src/main/resources/static/partials/portfolio-holdings.html
  - src/main/resources/static/partials/portfolio-deposit-financial.html
  - build.gradle
---

# 만기 지난 현금성 항목의 납입 알림·자동 납입 표시 정리 (#140)

## 요약
- 만기일 당일부터 현금성 항목(만기일이 있는 예금·적금)을 납입일 당일·미납 판정에서 뺀다. 알림 팝업, 설명 줄 "⚠ 미납", 표의 "미납" 배지, 납입 창 안내가 함께 정리된다.
- 만기 판단은 도메인 `CashDetail` 한 곳에 두고, #138 자동 납입 판정의 만기 조건도 같은 판단을 쓴다.
- 만기가 지난 항목을 자동 납입으로 저장하는 것은 막지 않고, 등록·수정 모달에 안내를 보인다.
- 만기된 항목의 설명 줄에는 "자동 납입"을 붙이지 않는다.
- 바뀌지 않는 것: DB·Entity·API 계약, 만기 전 항목의 알림·자동 납입, 펀드·연금, 만기일이 없는 항목, 표의 D-day·"만기 경과", 홈 만기 임박 알림

## 작업 리스트
- [ ] U1 도메인: 만기 판단(`isMaturedOn`)과 자동 납입 판정의 만기 조건 정리
- [ ] U2 서비스: 당일·미납 판정에서 만기된 현금성 항목 제외 → 체크포인트 CP1
- [ ] U3 화면: 설명 줄 "자동 납입" 조건, 등록·수정 모달의 만기 안내 → 체크포인트 CP2
- [ ] 단위 테스트 — 승인된 시나리오만, 대상 단위 구현 전에 작성하고 실패를 확인한다
- [ ] 리뷰 (태형님이 고른 이슈만 반영)
- [ ] 검증 (태형님 선택 방식)

## 배경 / 현재 상태
- **알림 판정:** `PortfolioService.getItems`가 현금성·펀드 항목의 `depositOverdue`·`depositDueToday`를 계산한다(`PortfolioService.java:393-396`). 기준일은 KST다(`:384`).
  - `isDepositOverdue`(`:1246-1260`)와 `isDepositDueToday`(`:1266-1277`)는 납입일(`resolveDepositDay`, `:1279-1290`)과 이번 달 기록만 본다. 시작일·만기일은 보지 않는다.
  - 두 함수의 기존 단위 테스트는 없다.
- **판정 결과 사용처(화면만):** 리마인더 팝업(`portfolio.js:2282`), 설명 줄 "⚠ 미납"(`:1114`, `:1125`, `:1137`), 표 "미납" 배지(`portfolio-holdings.html:245`, `:296`), 납입 창 안내(`portfolio-deposit-financial.html:16`), 리마인더 목록 배지(`:164-165`). 서버의 다른 곳에서는 쓰지 않는다.
- **자동 납입:** `CashDetail.isAutoDepositDueOn`은 만기일 당일부터 false다(`CashDetail.java:95-97`). 저장 검사(`validateDepositMode`, `:69-79`)와 설명 줄 "자동 납입"(`portfolio.js:1133`)은 만기를 보지 않는다.
- **등록·수정 모달 안내:** 자동 납입을 고르면 "…만기일부터는 건너뜁니다." 안내가 보인다(`portfolio-add.html:337-339`, `portfolio-edit.html:276-278`).
- **만기 표시 관례:** 표의 "현재가·만기" 열은 브라우저 현지 날짜로 D-day를 계산하고(`getDaysUntil`, `portfolio.js:896-903`), 만기일 다음 날부터 "만기 경과"를 보인다(`:934-951`).
- **만기일 없는 항목:** CMA는 만기일을 저장하지 않는다(`portfolio.js:1434`, `:2181`). 펀드 상세에는 만기일이 없다(`FundDetail.java:10-13`).

## 요구사항 원장
| ID | 요구사항 | 출처 | 이번 범위 | 근거 / 담당 |
|---|---|---|---|---|
| REQ-1 | 만기일 당일부터 현금성 자산(예금·적금·CMA)을 납입일 당일·미납 알림 대상에서 뺀다 | 이슈 작업 내용 1 | 포함 | U1·U2, KTD1·KTD2. CMA는 만기일이 없어 해당 없음 |
| REQ-2 | 만기가 지난 항목을 "납입일에 자동 납입"으로 저장할 때의 처리(막기 또는 안내)를 정한다 | 이슈 작업 내용 2 | 포함 | U3, KTD3. 태형님 결정 2A(안내만) |
| REQ-3 | 만기가 지난 자동 납입 항목의 설명 줄 "자동 납입" 표시를 정리한다 | 이슈 작업 내용 3 | 포함 | U3, KTD4. 태형님 결정 3A(빼기) |
| REQ-4 | 만기가 지난 현금성 항목에는 당일·미납 알림이 뜨지 않는다 | 이슈 완료 조건 1 | 포함 | U2, KTD2 |
| REQ-5 | 만기가 지난 항목의 자동 납입 저장·표시가 정한 정책대로 동작한다 | 이슈 완료 조건 2 | 포함 | U3, KTD3·KTD4 |
| REQ-6 | 만기 전 항목의 알림과 자동 납입은 지금과 같다 | 이슈 완료 조건 3 | 포함 | KTD1·KTD2. 만기 전·만기일 없음·펀드 판정을 테스트로 확인 |
| REQ-7 | 만기 해지·재예치 처리 | 이슈 범위 밖 | 제외 | 이슈 범위 밖 |
| REQ-8 | 만기 지난 항목의 자동 반영 저장을 서버에서 막기(400) | brainstorm 후보 2B | 제외 | 태형님 결정 2A(안내만). 자동 기록은 어차피 생기지 않는다 |
| REQ-9 | 표의 D-day·"만기 경과" 표시 규칙, 홈 만기 임박 알림 변경 | brainstorm 제외 범위 | 제외 | 기존 표시를 유지한다 |
| REQ-10 | 펀드·연금·만기일 없는 현금성 항목의 알림 변경 | brainstorm 결정 사항 | 제외 | 만기일이 없어 판정 대상이 아니다 |

## 핵심 기술 결정
**KTD1 — 만기 판단은 `CashDetail`이 가진다.**
- `isMaturedOn(LocalDate date)`: 만기일이 있고 date가 만기일 당일 이후면 true다. 만기일이 없으면 false다.
- `isAutoDepositDueOn`의 만기 조건(`:95-97`)을 이 메서드로 바꾼다. 동작은 같고, 기존 테스트(D6·D7)로 확인한다.

**KTD2 — 당일·미납 판정에서 만기된 현금성 항목을 뺀다.**
- `PortfolioService.isDepositOverdue`·`isDepositDueToday`는 현금성 항목이고 상세의 `isMaturedOn(기준일)`이 true면 false를 돌려준다. 나머지 규칙(말일 보정, 이번 달 기록)과 기준일(`getItems`의 KST 날짜)은 그대로다.
- 펀드·연금, 만기일 없는 현금성 항목은 지금과 같다.
- 판정 결과를 쓰는 화면 5곳은 코드 변경 없이 함께 바뀐다.

**KTD3 — 만기 지난 항목의 자동 납입은 막지 않고 안내한다.**
- `portfolio.js`에 `isMaturityReached(maturityDate)`를 둔다. `getDaysUntil`이 0 이하면 true, 값이 없거나 날짜가 아니면 false다(현지 날짜, 표의 D-day와 같은 계산).
- 등록·수정 모달의 자동 납입 안내 아래에, 자동 납입이고 `isMaturityReached(만기일)`이면 "만기된 항목이라 자동 납입이 기록되지 않습니다." 안내를 보인다.
- 저장 요청과 서버 검증은 그대로다.

**KTD4 — 만기된 항목의 설명 줄에는 "자동 납입"을 붙이지 않는다.**
- `getItemSummary` CASH 분기에서 자동 납입이고 `isMaturityReached(만기일)`이 false일 때만 "자동 납입"을 붙인다.

## API 계약
- 경로·요청·응답 필드는 그대로다.
- 목록 응답의 `depositOverdue`·`depositDueToday`가 만기된 현금성 항목에서 false로 바뀐다(값 의미 변경).

## DB 스키마 (변경 없음)
`schema_plan_status: none`. 테이블·컬럼·Entity를 건드리지 않는다.

## 구현 단위
### U1 도메인 (KTD1)
- `CashDetail.isMaturedOn` 추가, `isAutoDepositDueOn`의 만기 조건을 이 메서드로 바꾼다.

### U2 서비스 (선행 U1, KTD2)
- `PortfolioService`의 두 판정 함수 앞에서 만기된 현금성 항목이면 false를 돌려준다(공통 private 함수 1개).

### U3 화면 (KTD3·KTD4)
- `portfolio.js`: `isMaturityReached` 추가, `getItemSummary` CASH 분기 조건
- `portfolio-add.html`·`portfolio-edit.html`: 자동 납입 안내 아래 만기 안내 한 줄

## 시스템 전반 영향
- 만기된 항목은 리마인더 팝업, 미납 표시, 납입 창 안내에서 빠진다.
- 만기일 당일부터는 그달 납입을 놓쳤어도 미납으로 보이지 않는다(의도). 직접 납입은 그대로 할 수 있다.
- 자동 납입 배치와 #138 동작은 그대로다.

## 위험과 완화
| 위험 | 완화 |
|---|---|
| 만기일을 실수로 과거로 입력한 항목은 알림이 사라진다 | 표의 "만기 경과"로 보이고, 자동 납입이면 등록·수정 창 안내로도 알 수 있다 |
| 서버 판정(KST)과 화면 표시(현지 날짜)의 날짜 기준이 다르다 | 한국에서는 같다. 해외에서는 만기일 전후 하루 어긋날 수 있다(기존 D-day 표시와 같은 성질) |
| 자동 납입 판정의 만기 조건을 바꾸며 동작이 달라진다 | 같은 식으로 옮기고 기존 D6·D7 테스트로 확인한다 |

## 단위 테스트 계획
- 테스트 작성: 작성함 (2026-10-06 태형님 확인, brainstorm 확인 4)
- 사용자 승인: 테스트 시나리오 승인 대기. plan 승인 요청 때 대화로 Given/When/Then·Mock 대상·제외 범위·검증 명령을 제시한다.
- 승인 후 테스트 계획 문서에 승인된 시나리오만 적고 `test_plan_status`를 `approved`로 바꾼다.

## 작업 진행 방식 (워크플로우 예외)
이 세션에는 compound-engineering(`/ce:work`·`/ce:review`)이 없어 같은 절차를 수동으로 적용한다(#113·#136·#138과 동일).
- **구현:** 작업 리스트 순서대로 진행한다.
  - 단위마다 변경 파일이 allowed_paths 안이고 blocked_paths 밖인지 확인한다.
  - 테스트 대상 단위는 테스트를 먼저 작성하고 실패를 확인한다.
- **체크포인트:** CP마다 `checkpoint-guard.sh`로 경로를 검사하고 read-only 가드 리뷰를 수행한다.
- **리뷰:** 리뷰 게이트 문서 기준으로 finding 표를 만들고, 태형님이 고른 이슈만 수정한다.

## 체크포인트
- CP1 (U2 후): 만기 판단, 당일·미납 판정(만기 전·당일·후, 만기일 없음, 펀드), 자동 납입 판정 동작 유지
- CP2 (U3 후): 저장소 밖 브라우저 하네스(실제 JS·partial)로 설명 줄과 모달 안내를 확인한다

## 검증
검증 방식은 verify 단계에서 태형님이 고른다. 후보:
- `./gradlew test`: 임시 Postgres로 실행할 수 있다.
- `bootRun` + curl: 임시 Postgres로 기동해 만기 전·당일·후 항목의 `depositOverdue`·`depositDueToday`를 확인한다.
- 운영 반영 후 확인(태형님): 만기가 지난 적금에 납입 알림과 미납 표시가 사라졌는지 본다.

## 수정 범위
- **수정 가능:** 위 allowed_paths 범위
  - 도메인 `CashDetail`, `PortfolioService`
  - `portfolio.js`, 등록·수정 partial
  - 신규 단위 테스트 2종(승인 후 확정)과 테스트 계획 문서
- **수정 금지:** 위 blocked_paths 범위
  - `PortfolioItem`, 펀드·연금 상세, 납입 이력, 저장소·영속·API 계층, 응답 DTO, 자동 납입 배치
  - 알림(메일)·챗봇, 전역 예외 처리기, 설정 파일, SQL
  - `api.js`·`app.js`·`home.js`·`index.html`, 보유 자산·납입 창 partial, `build.gradle`

## 완료 정의
- REQ-1\~6을 충족하고, REQ-7\~10은 제외로 유지한다.
- 만기된 현금성 항목에는 당일·미납 알림과 미납 표시가 없고, 자동 납입 저장·표시가 정한 정책대로 동작한다. 만기 전 항목과 다른 자산군은 지금과 같다.
- 승인된 단위 테스트가 통과한다.
