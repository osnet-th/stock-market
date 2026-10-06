---
title: "feat: 적금 자동납입 처리 방식 선택(알림 확인 / 자동 반영)"
type: feat
issue: 138
issue_url: https://github.com/osnet-th/stock-market/issues/138
status: active
date: 2026-10-06
approved: "2026-10-06 태형님 승인 (기능 plan + 추가 결정 a~d + DB 스키마 설계 cash_detail.deposit_mode VARCHAR(20) NULL + 단위 테스트 시나리오 D1~D7·M3~M4·S1~S7·B1~B3). '끝까지 한번에 진행해'로 단위별 확인 없이 구현을 이어가도록 지시함"
workflow_exception: "클라우드 세션에 compound-engineering(/ce:plan·/ce:work·/ce:review)이 없어 planning·briefing·review 게이트 절차를 수동 적용한다 (#113·#136과 동일)"
branch: claude/inspiring-wright-rljbsn
branch_exception: "클라우드 세션은 지정 브랜치에만 push할 수 있어 issue/138-{slug} 대신 세션 브랜치를 쓴다 (#136과 동일). validate-plan.sh의 branch 형식 검사 1건은 이 예외로 실패한다."
worktree: /home/user/stock-market-issue-138
worktree_note: "/home/user/stock-market을 가리키는 심볼릭 링크. checkpoint-guard.sh가 경로에서 이슈 번호를 읽기 때문에 둔다."
brainstorm: docs/brainstorms/2026-10-05-savings-auto-deposit-mode-brainstorm.md
test_plan_status: approved
schema_plan_status: approved
allowed_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/CashDetail.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PortfolioItem.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/enums/DepositMode.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/repository/PortfolioItemRepository.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/CashItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemJpaRepository.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemRepositoryImpl.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/mapper/PortfolioItemMapper.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/scheduler/PortfolioAutoDepositScheduler.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioAutoDepositBatchService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/CashDetailResponse.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/PortfolioController.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/CashItemAddRequest.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/CashItemUpdateRequest.java
  - src/main/resources/application.yml
  - src/main/resources/db/migration/cash_detail_deposit_mode_2026_10_06.sql
  - src/main/resources/static/js/components/portfolio.js
  - src/main/resources/static/partials/portfolio-add.html
  - src/main/resources/static/partials/portfolio-edit.html
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/CashDetailDepositModeTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/mapper/PortfolioItemMapperTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceAutoDepositTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioAutoDepositBatchServiceTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioServiceInstitutionTest.java
  - docs/plans/tests/2026-10-06-138-savings-auto-deposit-mode-test-plan.md
  - docs/plans/2026-10-06-001-feat-savings-auto-deposit-mode-plan.md
  - docs/brainstorms/2026-10-05-savings-auto-deposit-mode-brainstorm.md
  - .claude/issues/138/**
blocked_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/DepositHistory.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/FundDetail.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PensionDetail.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/repository/DepositHistoryRepository.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/DepositHistoryEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/DepositHistoryJpaRepository.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/DepositHistoryRepositoryImpl.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/FundItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PensionItemEntity.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/scheduler/PortfolioSnapshotScheduler.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioSnapshotBatchService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioSummaryService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioEvaluationService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioIncomeService.java
  - src/main/java/com/thlee/stock/market/stockmarket/notification/**
  - src/main/java/com/thlee/stock/market/stockmarket/chatbot/**
  - src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java
  - src/main/resources/application-dev.yml
  - src/main/resources/application-prod.yml
  - src/main/resources/static/js/api.js
  - src/main/resources/static/js/app.js
  - src/main/resources/static/index.html
  - src/main/resources/static/partials/portfolio-holdings.html
  - src/main/resources/static/partials/portfolio-deposit-financial.html
  - build.gradle
---

# 적금 자동납입 처리 방식 선택(알림 확인 / 자동 반영) (#138)

## 요약
- 현금성 자산(예금·적금·CMA)의 자동납입 설정에 "납입 처리" 방식을 둔다. 알림 확인(기본, 지금 동작)과 자동 반영 두 가지다.
- 자동 반영 항목은 매일 00:10(KST) 일배치가 납입일에 월 납입액을 납입 이력(메모 "자동 납입")에 기록하고 원금에 더한다. 이번 달 기록이 이미 있거나 시작일 전·만기일 당일 이후면 건너뛴다. 놓친 날은 소급하지 않는다.
- 알림 확인 방식에서 리마인더의 [납입]을 누르면 월 납입액과 오늘(현지 날짜)을 미리 채운 납입 창을 연다.
- 보유 자산 설명 줄에 "자동 납입"을 표시한다.
- 바뀌지 않는 것: 리마인더(당일·미납) 판정 규칙, 납입 이력 구조, 펀드·연금, 메일 알림

## 작업 리스트
- [x] U1 도메인·영속: 납입 처리 방식 값과 규칙, 컬럼·매핑, 운영 반영용 SQL → 체크포인트 CP1 (2026-10-06, D·M 작성 후 컴파일 실패 확인, 구현 후 통과)
- [x] U2 등록·수정 API: 현금성 등록·수정 요청과 응답에 납입 처리 방식 추가 (2026-10-06)
- [x] U3 자동 기록: 항목 1건 자동 기록(서비스), 대상 조회, 일배치·스케줄러 → 체크포인트 CP2 (2026-10-06, S·B 작성 후 컴파일 실패 확인, 구현 후 통과)
- [x] U4 화면: 납입 처리 선택, 설명 줄 표시, 리마인더 [납입] 미리 채움, 납입 창 기본 날짜 → 체크포인트 CP3 (2026-10-06, 브라우저 하네스 28건 통과)
- [x] 단위 테스트 — 승인된 시나리오만, 대상 단위 구현 전에 작성하고 실패를 확인한다 (D 20회·M 2건·S 9회·B 3건 통과, 전체 `./gradlew test` 40개 클래스 263건 통과)
- [ ] R1 리뷰 반영(태형님이 고른 이슈만) → 체크포인트 CP4
- [ ] 검증 (태형님 선택 방식)

## 배경 / 현재 상태
- **자동납입 설정:** `CashDetail`에 월 납입액·납입일이 있고(`CashDetail.java:17-18`), 테이블은 `cash_detail.monthly_deposit_amount`·`deposit_day`다(`CashItemEntity.java:32-36`). 처리 방식 값은 없다. 생성자는 5·7인자 두 가지다(`:20-42`).
- **도메인 쓰기 경로:** 등록은 `PortfolioItem.createWithCash`(`PortfolioItem.java:172-181`), 수정은 `updateCashDetail`(`:384-391`)을 지난다. DB 조회는 재구성 생성자를 쓰므로 두 메서드를 지나지 않는다.
- **등록·수정 API:** 컨트롤러가 요청 값을 풀어 `addCashItem`(`PortfolioService.java:242-268`)·`updateCashItem`(`:273-301`)에 넘긴다(`PortfolioController.java:184-198`, `:323-337`). 응답은 `CashDetailResponse.from`이 만든다.
- **납입 기록:** `addDeposit`이 납입 이력을 저장하고 `restoreAmount`로 원금을 늘린 뒤 이벤트를 남긴다(`PortfolioService.java:1110-1124`, `:1530-1541`). 항목 엔티티는 `@Version` 낙관적 잠금을 쓴다(`PortfolioItemEntity.java:59-62`).
- **리마인더 판정:** `getItems`가 현금성·펀드 항목의 `depositDueToday`·`depositOverdue`를 계산한다(`PortfolioService.java:357-394`). 납입일은 말일 보정(`:1251-1253`)하고, 이번 달 1일\~기준일 기록이 있으면 대상에서 뺀다(`:1255-1260`). 기준일은 서버 기본 시간대의 `LocalDate.now()`다(`:373`).
- **스케줄러 관례:** `@Scheduled(cron = "${...:기본값}", zone = "Asia/Seoul")` + `LoggingContext.forScheduler`(`PortfolioSnapshotScheduler.java:21-31`). 배치 서비스는 트랜잭션 없이 대상별 `try/catch`로 실패를 격리하고, 저장은 다른 빈의 `@Transactional` 메서드가 맡는다(`PortfolioSnapshotBatchService.java`). cron은 `application.yml`의 `scheduler.portfolio.*`에 둔다(`:172-175`).
- **KST 날짜:** 컨테이너에 TZ 설정이 없다. #113은 `Clock.system(Asia/Seoul)`을 주입한다(`PortfolioIncomeService.java:53`, `:63-73`).
- **대상 조회:** `PortfolioItemRepository`에는 처리 방식으로 고르는 조회가 없다. JPA 저장소는 JPQL 조회 예가 있다(`PortfolioItemJpaRepository.java` `findDistinctUserIdsByStatus`).
- **화면**
  - 등록·수정 모달의 현금성 칸 끝에 "자동납입 설정"(월 납입액·납입일)이 있다(`portfolio-add.html:314-328`, `portfolio-edit.html:253-267`). 현금성 선택 시 기본값은 `selectAssetType`이 채운다(`portfolio.js:1253-1256`).
  - 등록·수정 본문은 `portfolio.js:1420-1432`, `:2152-2165`, 수정 모달 값 채우기는 `:2015-2025`, 설명 줄은 `getItemSummary`의 CASH 분기(`:1127-1144`)다.
  - 리마인더 [납입]은 `openDepositFromReminder`(`:2273-2279`)가 `openDepositModal`을 연다. 금액은 비어 있고, 기본 날짜는 `toISOString()`(UTC)이라 한국 오전 9시 전에는 전날이다(`:2206`, `:2306`). 현지 날짜 함수 `_depositReminderToday`가 이미 있다.
  - 등록·수정 실패는 "자산 등록에 실패했습니다." 같은 고정 문구만 보인다.

## 요구사항 원장
| ID | 요구사항 | 출처 | 이번 범위 | 근거 / 담당 |
|---|---|---|---|---|
| REQ-1 | 현금성 자산(예금·적금·CMA)의 자동납입 설정에 처리 방식 선택(알림 받고 직접 납입 / 납입일에 자동 납입)을 추가한다 | 이슈 작업 내용 1 | 포함 | U1·U2·U4 |
| REQ-2 | 자동 납입 항목은 납입일 00:10(KST)에 월 납입액을 납입 이력(메모 "자동 납입")에 기록하고 원금에 더한다 | 이슈 작업 내용 2 | 포함 | U3, KTD3·KTD4 |
| REQ-3 | 알림 방식의 [납입]은 월 납입액과 오늘 날짜를 미리 채운 납입 창을 연다 | 이슈 작업 내용 3 | 포함 | U4, KTD6 |
| REQ-4 | 보유 자산 설명 줄에 "자동 납입"을 표시한다 | 이슈 작업 내용 4 | 포함 | U4 |
| REQ-5 | 자동납입을 설정할 때 처리 방식을 고를 수 있고, 기존 항목과 새 항목의 기본값은 알림 확인이다 | 이슈 완료 조건 1 | 포함 | U1·U2·U4, KTD1 |
| REQ-6 | 자동 납입 항목은 납입일에 한 번만 기록된다. 이번 달 기록이 이미 있거나 시작일 전·만기일 이후면 건너뛴다 | 이슈 완료 조건 2 | 포함 | U1·U3, KTD2·KTD4 |
| REQ-7 | 자동 기록이 빠진 달은 기존 당일·미납 알림이 그대로 뜬다 | 이슈 완료 조건 3 | 포함 | KTD5. 리마인더 판정을 바꾸지 않는다 |
| REQ-8 | 알림 확인 항목은 지금과 같이 동작한다 | 이슈 완료 조건 4 | 포함 | KTD1·KTD5 |
| REQ-9 | 메일 알림·알림 설정 | 이슈 범위 밖(#99) | 제외 | #99 범위 |
| REQ-10 | 은행 계좌·자동이체 연동 | 이슈 범위 밖 | 제외 | 이슈 범위 밖 |
| REQ-11 | 만기 지난 항목의 알림 판정 변경 | 이슈 범위 밖, brainstorm 확인 6 | 제외 | 기존 판정 규칙 유지. 필요하면 후속 이슈 |
| REQ-12 | 놓친 달 소급 기록 | 이슈 범위 밖, brainstorm 확인 2 | 제외 | 이중 계상 위험. 놓친 달은 기존 알림이 안내한다 |
| REQ-13 | 펀드·연금 자동 납입 | 이슈 범위 밖, brainstorm 확인 1 | 제외 | 대상은 현금성 자산만 |
| REQ-14 | 자동 반영이면 월 납입액(0 초과)과 납입일(1\~31일)이 꼭 있어야 하고, 없으면 저장을 거부한다 | brainstorm 권장 접근(저장) | 포함 | U1·U2·U4, KTD2 |
| REQ-15 | 자동 기록은 항목별로 실패를 격리하고, 같은 항목이 같은 달에 두 번 기록되지 않게 한다 | brainstorm 권장 접근(자동 반영) | 포함 | U3, KTD4 |
| REQ-16 | 자동 기록의 날짜는 KST로 계산한다 | brainstorm 권장 접근(자동 반영) | 포함 | U3, KTD3 |
| REQ-17 | 납입 이력 구조 변경 | brainstorm 제외 범위 | 제외 | 기존 납입 이력을 그대로 쓴다 |

## 핵심 기술 결정
**KTD1 — 처리 방식은 `CashDetail`의 값이고, 비어 있으면 알림 확인이다.**
- 도메인 enum `DepositMode { NOTIFY, AUTO }`(알림 확인 / 자동 반영)를 둔다. 문자열을 받는 `from`은 null·빈 값이면 NOTIFY, 모르는 값이면 `IllegalArgumentException`("납입 처리 방식이 올바르지 않습니다: …")을 던진다.
- `CashDetail`에 `depositMode`를 더한 8인자 생성자를 둔다. null이면 NOTIFY로 채운다. 기존 5·7인자 생성자는 NOTIFY로 위임해 그대로 둔다.
- 엔티티 `CashItemEntity`에 `deposit_mode` 문자열 컬럼을 두고 매퍼가 양방향으로 옮긴다. 기존 행(NULL)은 NOTIFY로 읽는다.

**KTD2 — 자동 반영 규칙은 `CashDetail`이 가진다.**
- `validateDepositMode()`: AUTO인데 월 납입액이 없거나 0 이하, 또는 납입일이 없거나 1\~31 밖이면 `IllegalArgumentException`("자동 납입은 월 납입액과 납입일(1~31일)을 입력해야 합니다.")을 던진다. API는 400으로 응답한다.
- 이 검사는 쓰기 경로(`createWithCash`, `updateCashDetail`)에서만 한다. DB 조회(재구성)는 검사하지 않는다.
- `isAutoDepositDueOn(date)`: AUTO이고 월 납입액·납입일이 있으며, 시작일 전이 아니고 만기일 전이며, 그 날이 이번 달 납입일(말일 보정)이면 true다. 날짜가 비어 있으면 그 조건은 보지 않는다.
- 말일 보정은 리마인더 판정(`PortfolioService.effectiveDepositDay`)과 같은 규칙(`min(납입일, 그달 일수)`)이다. 리마인더 쪽 코드는 바꾸지 않는다(유지보수 포인트).

**KTD3 — 일배치는 KST로 하루 한 번 돈다.**
- `PortfolioAutoDepositScheduler`(infrastructure/scheduler): `@Scheduled(cron = "${scheduler.portfolio.auto-deposit.cron:0 10 0 * * *}", zone = "Asia/Seoul")`, `LoggingContext.forScheduler("portfolio-auto-deposit")`. `application.yml`에 cron 한 줄을 더한다.
- `PortfolioAutoDepositBatchService`(application, 트랜잭션 없음): 오늘을 KST `Clock`으로 구한다(#113과 같은 생성자 주입). 대상 조회 → 오늘이 납입일인 항목만 → 항목마다 `try/catch`로 1건 기록을 호출한다. 기록한 수를 돌려주고 로그를 남긴다.
- 대상 조회는 `PortfolioItemRepository.findActiveAutoDepositCashItems()`(ACTIVE이고 처리 방식이 AUTO인 현금성 항목)다. JPA 저장소에 JPQL 조회 1개를 더한다.

**KTD4 — 1건 기록은 기존 납입 추가와 같은 서비스에서 한 트랜잭션으로 한다.**
- `PortfolioService.recordAutoDeposit(itemId, date)`(`@Transactional`): 항목을 다시 읽어 ACTIVE·현금성·`isAutoDepositDueOn(date)`을 확인하고, 이번 달 기록이 있으면(기존 `hasNoDepositThisMonth`) 건너뛴다. 기록하면 true다.
- 기록: 납입일 = date, 금액 = 월 납입액, 좌수 없음, 메모 "자동 납입". 원금 증가(`restoreAmount`)와 항목 저장, 이벤트(`PORTFOLIO_DEPOSIT_AUTO_ADDED`, 사용자 = 항목 소유자)는 `addDeposit`과 같다.
- 중복 방지: 트랜잭션 안에서 이번 달 기록을 다시 확인한다. 같은 항목을 동시에 저장하면 `@Version` 충돌로 한쪽이 롤백된다. 배치는 하루 한 번이라, 자동 기록을 지워도 같은 날 다시 생기지 않는다.

**KTD5 — 리마인더는 바꾸지 않는다.**
- 자동 기록이 되면 이번 달 기록이 생겨 당일·미납 대상에서 빠진다. 배치가 실패하거나 서버가 꺼져 있던 달은 지금처럼 당일·미납 알림이 뜬다.
- 리마인더 판정의 기준일은 지금처럼 서버 기본 시간대다. 자동 기록(KST 납입일)과 시간대가 달라도 잘못된 알림이 생기지 않음을 확인했다(배치 직후 한국 0\~9시에는 서버 날짜가 전날이라 당일·미납 조건이 모두 거짓이다).

**KTD6 — 화면**
- 등록·수정 모달의 자동납입 설정 아래에 "납입 처리" 선택 칸(알림 받고 직접 납입 / 납입일에 자동 납입)을 둔다. 라벨과 칸을 id로 잇는다. 자동 납입을 고르면 "납입일 0시 10분에 월 납입액을 납입 이력에 기록하고 원금에 더합니다." 안내를 보인다.
- 기본값: 현금성 선택 시 NOTIFY, 수정 모달은 `cashDetail.depositMode`(없으면 NOTIFY). 등록·수정 본문에 `depositMode`를 보낸다.
- 제출 전 확인: AUTO인데 월 납입액이 없거나 납입일이 1\~31 밖이면 서버와 같은 문구로 알리고 보내지 않는다. 실패 알림이 고정 문구라 서버 메시지가 보이지 않기 때문이다.
- 설명 줄: CASH 분기에서 월 납입액 뒤에 "자동 납입"을 붙인다.
- 리마인더 [납입]: `openDepositFromReminder`가 창을 연 뒤 금액에 항목의 월 납입액(현금성·펀드·연금 상세 중 해당 값)을 채운다. 표의 [납입] 버튼으로 열 때는 금액을 채우지 않는다.
- 납입 창 기본 날짜를 현지 날짜(`_depositReminderToday`)로 바꾼다. 리마인더와 표 버튼이 같은 창이라 두 경우 모두 바뀐다(`:2206`, `:2306`).

## API 계약
| 구분 | 변경 |
|---|---|
| 등록 `POST /api/portfolio/items/cash` | 요청에 `depositMode`(`NOTIFY` / `AUTO`, 선택) 추가. 없거나 빈 값이면 NOTIFY |
| 수정 `PUT /api/portfolio/items/cash/{itemId}` | 요청에 `depositMode` 추가. 메모·금융기관처럼 덮어쓴다. 없거나 빈 값이면 NOTIFY로 바뀐다 |
| 항목 응답(목록·등록·수정) | `cashDetail.depositMode` 추가. 현금성 항목은 항상 값이 있다 |
| 오류 | AUTO인데 월 납입액 없음·0 이하 또는 납입일 없음·1\~31 밖 → 400 `BAD_REQUEST`, "자동 납입은 월 납입액과 납입일(1~31일)을 입력해야 합니다." / 모르는 값 → 400 |

경로·인증과 다른 자산군·납입 이력·요약 API는 그대로다.

## DB 스키마 리뷰 (2026-10-06 승인)
| 항목 | 설계 |
|---|---|
| table | `cash_detail` (기존, 현금성 항목 자식 테이블). 목적: 자동납입 처리 방식 |
| column | `deposit_mode` `VARCHAR(20)`, NULL 허용, default 없음. 값 `NOTIFY` / `AUTO`. NULL은 NOTIFY로 읽는다 |
| PK·UK·FK·index·constraint | 추가하지 않는다. 배치 대상은 하루 한 번 ACTIVE·AUTO 현금성 항목만 읽고, 행 수가 적어 인덱스가 필요 없다 |
| Entity 매핑 | `CashItemEntity`에 `@Column(name = "deposit_mode", length = 20)` 문자열 필드. 연관관계 없음 |
| 반영 방식 | `ddl-auto: update`가 기동 시 컬럼을 추가한다. 운영 수동 적용·롤백 백업용으로 `cash_detail_deposit_mode_2026_10_06.sql`(`ADD COLUMN IF NOT EXISTS`)을 남긴다. 기존 행 채우기는 없다 |
| 기존 데이터 영향 | 기존 행은 NULL로 남아 알림 확인으로 동작한다. 앱이 현금성 항목을 저장하면(수정, 연결 현금 차감 등) 그때 `NOTIFY` 또는 `AUTO`가 기록된다 |
| 롤백 | 코드만 되돌려도 동작한다(남은 컬럼은 쓰지 않는다). 컬럼까지 지우려면 SQL 파일의 롤백 문(`DROP COLUMN IF EXISTS`)을 쓴다. 이때 처리 방식 설정은 사라지고, 이미 생긴 자동 납입 이력은 일반 납입 이력으로 남는다 |

## 구현 단위
### U1 도메인·영속 (KTD1·KTD2)
- `DepositMode` enum 신규, `CashDetail`에 값·생성자·`validateDepositMode`·`isAutoDepositDueOn`
- `PortfolioItem.createWithCash`·`updateCashDetail`에서 `validateDepositMode` 호출
- `CashItemEntity` 컬럼·생성자 인자, `PortfolioItemMapper` CASH 양방향
- 운영 반영용 SQL 파일

### U2 등록·수정 API (선행 U1)
- `CashItemAddRequest`·`CashItemUpdateRequest`에 `depositMode`
- 컨트롤러 → `addCashItem`·`updateCashItem` 인자 추가(`depositDay` 뒤), `DepositMode.from`으로 바꿔 `CashDetail`에 넣는다
- `CashDetailResponse`에 `depositMode`
- 시그니처가 바뀌어 기존 테스트 1개 파일(`PortfolioServiceInstitutionTest`)의 호출 4곳에 인자 하나(null)를 넣는다. 검증 내용은 바꾸지 않는다

### U3 자동 기록 (선행 U1, KTD3·KTD4)
- `PortfolioItemRepository.findActiveAutoDepositCashItems()` + 구현체 + JPQL 조회
- `PortfolioService.recordAutoDeposit(itemId, date)`
- `PortfolioAutoDepositBatchService`, `PortfolioAutoDepositScheduler`, `application.yml` cron

### U4 화면 (선행 U2, KTD6)
- 등록·수정 모달 "납입 처리" 선택 칸과 안내
- `portfolio.js`: 기본값, 수정 모달 값 채우기, 등록·수정 본문, 제출 전 확인, 설명 줄, 리마인더 [납입] 금액 채우기, 납입 창 기본 날짜

## 시스템 전반 영향
- 기동 시 `cash_detail`에 컬럼이 하나 추가된다.
- 매일 00:10(KST) 스케줄러가 하나 늘어난다(스케줄러 풀 10). 대상이 없으면 조회 1번으로 끝난다.
- 현금성 등록·수정 요청과 응답에 필드가 하나씩 늘어난다. 기존 클라이언트가 보내지 않으면 등록은 알림 확인, 수정은 알림 확인으로 바뀐다(화면은 늘 현재 값을 보낸다).
- 자동 기록은 원금·납입 이력을 바꾸므로 그날 스냅샷(15:40)과 요약에 반영된다. 챗봇 문맥은 바꾸지 않는다.

## 위험과 완화
| 위험 | 완화 |
|---|---|
| 서버가 00:10에 꺼져 있으면 그달 자동 기록이 빠진다 | 소급하지 않고(확인 2) 기존 당일·미납 알림이 안내한다(KTD5) |
| 같은 항목이 같은 달에 두 번 기록된다 | 트랜잭션 안 재확인 + `@Version` 충돌 시 롤백(KTD4). 배치는 하루 한 번 |
| 한 항목 실패가 다른 항목 기록을 막는다 | 배치는 트랜잭션 없이 항목별 `try/catch`, 기록은 항목별 트랜잭션(KTD3·KTD4) |
| 서버 기본 시간대(UTC)로 날짜가 하루 어긋난다 | 배치는 KST `Clock`을 쓴다. 리마인더와의 시간대 차이로 잘못된 알림이 생기지 않는다(KTD5) |
| 월 중간에 자동 반영으로 바꾸면 이미 지난 납입일은 기록되지 않는다 | 의도된 동작(소급 없음). 이번 달 기록이 없으면 기존 미납 알림이 뜬다 |
| 수정 요청에 `depositMode`가 빠지면 알림 확인으로 바뀐다 | 화면은 수정 시 현재 값을 늘 보낸다. API 계약에 적는다 |
| 말일 보정 규칙이 도메인과 리마인더 두 곳에 있다 | 같은 식을 쓰고, 유지보수 포인트로 남긴다. 리마인더 코드는 이번에 바꾸지 않는다 |

## 단위 테스트 계획
- 테스트 작성: 작성함 (2026-10-06 태형님 확인, brainstorm 확인 7)
- 테스트 계획 문서: [테스트 계획](./tests/2026-10-06-138-savings-auto-deposit-mode-test-plan.md)
- 사용자 승인: 테스트 시나리오 승인됨 (2026-10-06)
- 승인된 테스트 시나리오: 19건 (도메인 D1~D7, 매핑 M3~M4, 서비스 S1~S7, 배치 B1~B3)
- 검증 명령: `./gradlew test --tests "*CashDetailDepositModeTest" --tests "*PortfolioItemMapperTest" --tests "*PortfolioServiceAutoDepositTest" --tests "*PortfolioAutoDepositBatchServiceTest"`
- 작성 순서: 도메인·매핑 테스트는 U1 구현 전에, 서비스·배치 테스트는 U2·U3 구현 전에 작성하고 실패를 확인한다.

## 작업 진행 방식 (워크플로우 예외)
이 세션에는 compound-engineering(`/ce:work`·`/ce:review`)이 없어 같은 절차를 수동으로 적용한다(#113·#136과 동일).
- **구현:** 작업 리스트 순서대로 한 단위씩 진행한다.
  - 단위마다 변경 파일이 allowed_paths 안이고 blocked_paths 밖인지 확인한다.
  - 테스트 대상 단위는 테스트를 먼저 작성하고 실패를 확인한다.
  - 각 단위를 마치면 태형님께 다음 진행 여부를 확인한다.
  - 2026-10-06 태형님의 "끝까지 한번에 진행해" 지시로 단위별 확인 없이 이어서 진행하고, 체크포인트 결과는 마지막에 함께 보고한다. 경로 위반이나 범위 밖 변경(SCOPE_CREEP)이 나오면 즉시 멈추고 보고한다.
- **체크포인트:** CP마다 `checkpoint-guard.sh`로 경로를 검사하고 read-only 가드 리뷰를 수행한다.
- **리뷰:** 리뷰 게이트 문서 기준으로 finding 표를 만들고, 태형님이 고른 이슈만 수정한다.

## 체크포인트
- CP1 (U1 후): 처리 방식 기본값·검증·납입일 판정, 매핑 왕복, 기존 테스트 컴파일
- CP2 (U3 후): 등록·수정 API와 응답, 1건 기록(건너뛰기 조건 포함), 배치 실패 격리와 KST 날짜
- CP3 (U4 후): 저장소 밖 브라우저 하네스(실제 JS·partial)로 선택 칸·안내·제출 전 확인·설명 줄·리마인더 금액 채우기·기본 날짜·요청 본문을 확인한다
- CP4 (R1 후): 리뷰에서 고른 이슈만 반영됐는지 확인한다

### CP1 결과 (2026-10-06)
- 경로 검사 통과(변경 8개 파일 모두 plan 범위 안). 가드 리뷰 판정 CLEAN.
- 가드 참고(고치지 않음)
  - DB에 예상 밖 처리 방식 값이 직접 들어가면 매퍼의 `DepositMode.from`이 예외를 던져 그 사용자의 목록 조회가 실패한다. 같은 매퍼의 `CashSubType`·`TaxType` 변환도 같은 방식이고, 앱은 `name()`만 저장한다.
  - 처리 방식이 NULL인 기존 행은 앱이 처음 저장할 때 `NOTIFY`로 기록된다(DB 스키마 리뷰대로).

### CP2 결과 (2026-10-06)
- 경로 검사 통과(변경 14개 파일 모두 plan 범위 안). 가드 리뷰 판정 CLEAN.
  - 리마인더 판정, 납입 이력 도메인·엔티티·저장소, 펀드·연금은 바뀌지 않았다.
  - 배치는 다른 빈(프록시)으로 `recordAutoDeposit`을 불러 트랜잭션이 적용된다. JPQL(JOINED 하위 엔티티 + 상속 필드 status)은 올바르고, NULL 행이 빠지는 것은 NOTIFY 의미와 맞다.
- 가드 참고(고치지 않음)
  - 이번 달 기록 확인은 1일\~그 날짜만 본다(리마인더와 같은 규칙). 납입일 뒤 날짜로 미리 넣은 같은 달 기록이 있으면 자동 기록이 한 번 더 생길 수 있다.
  - `@Scheduled` 작업은 13개가 되어 풀 크기 10보다 많지만, 00:10 전후에 풀을 다 쓰는 작업이 없고 날짜는 실행 시점에 구한다.
  - JPQL은 단위 테스트 범위 밖이라 실제 실행은 verify 단계(bootRun)에서 처음 확인한다.

### CP3 결과 (2026-10-06)
- 경로 검사 통과(변경 3개 파일 모두 plan 범위 안). 가드 리뷰 판정 CLEAN.
  - 선택 칸은 두 모달의 현금성 템플릿 안에만 있고, `depositMode`는 현금성 등록·수정 본문에만 들어간다. 리마인더 판정과 매도 창 날짜는 바뀌지 않았다.
  - 제출 전 확인의 규칙·문구가 서버와 같다. 금액 미리 채우기는 리마인더 [납입]에서만 하고, 표의 [납입]은 채우지 않는다.
- 브라우저 하네스(저장소 밖, 실제 partial 4종 + 실제 `portfolio.js` + Alpine + Tailwind v3 CSS, 시간대 Asia/Seoul·고정 시각 2026-10-25T15:30Z) 28건 통과
  - 설명 줄 "자동 납입"(자동 반영만), 테이블 표시
  - 등록: 선택 칸·라벨 연결·선택지·위치·기본값·안내 표시, 제출 전 확인 3건(월 납입액 없음·32일·0일), 요청 본문(AUTO·NOTIFY), 다른 자산군은 칸·필드 없음
  - 수정: 현재 값 채우기(AUTO·기존 행 NOTIFY), 저장 본문, 바꾸기, 납입일 지우면 안내, 펀드는 칸·필드 없음
  - 납입 창: 기본 날짜가 현지(KST) 오늘(UTC는 전날인 시각), 리마인더 [납입] 금액 채우기(현금성·펀드), 월 납입액 없으면 비움, 팝업 클릭 흐름, 추가 본문과 추가 뒤 초기화
  - 375px에서 선택 칸이 화면 안, 등록 모달 패널 가로 넘침 없음, 콘솔·페이지 오류 없음
- 가드 참고(고치지 않음)
  - 금액 채우기는 납입 이력을 불러온 뒤에 한다. 불러오는 사이 사용자가 금액을 먼저 치면 덮어쓴다(가능성 낮음).
  - 상세를 고르는 삼항식이 `depositReminderMeta`와 중복된다. 기존 함수를 고치지 않은 보수적 선택이다.

## 리뷰 결과 (2026-10-06)
- **방식:** compound-engineering 리뷰 명령이 없어 리뷰 게이트 기준으로 수동 리뷰했다. 백엔드 정합성·아키텍처 리뷰와 요구사항·API 계약·화면 리뷰를 따로 수행하고 CP1~CP3 가드 참고를 합쳤다.
- **결과:** P1 없음. 의존 방향, domain 순수성, `@Transactional` 위치, self-invocation, Entity·도메인 모델 노출 위반이 없다. P2 3건, P3 3건.

| No | Severity | Issue | 관련 요구사항 | 위치 | 코드 맥락 | 영향 | 수정 방향 |
|---|---|---|---|---|---|---|---|
| 1 | P2 | 한국 0\~9시에 리마인더 [납입]으로 기록해도 알림이 사라지지 않는다 | REQ-3, REQ-8 (KTD5·KTD6) | `PortfolioService.java:381` ↔ `portfolio.js:2227` | 납입 창 기본 날짜는 현지(KST)로 바뀌었는데 리마인더 기준일은 서버 기본 시간대(TZ 설정 없음 → UTC)의 `LocalDate.now()`다. 0\~9시에는 새 기록이 "1일\~기준일" 밖이라 세지 않는다 | 09시 전까지 같은 항목이 다시 뜨고, 한 번 더 납입하면 이중 기록·원금 과다. 기본 날짜를 바꾸기 전에는 없던 동작 | 리마인더 기준일을 KST로 계산한다. 00:00\~00:10에는 자동 반영 항목도 잠깐 '오늘 납입일'로 보인다 |
| 2 | P2 | 설명 줄의 "자동 납입"이 흔한 화면 폭에서 말줄임으로 가려진다 | REQ-4 | `portfolio.js:1135` → `portfolio-holdings.html:248`, `:299` | 설명 줄은 한 줄 `truncate`이다. 만기일·만기 예상이 있는 적금은 하네스 측정상 1280·1440px에서 가려지고 1680px부터 보인다(실제 화면은 사이드바만큼 더 좁다) | 노트북·모바일에서 자동 납입 항목을 구분할 수 없다 | "자동 납입"을 유형 바로 뒤로 옮긴다("적금 · 자동 납입 · 3.5% · …") |
| 3 | P2 | 자동 기록 전에 열어 둔 화면에서 수정을 저장하면 원금이 옛 값으로 덮인다 | REQ-2 | `portfolio.js:1977`(`openEditModal`) → `PortfolioService.java:288`(`updateAmount`) | 수정 창은 화면 목록의 원금으로 채우고, 서버는 받은 원금을 그대로 저장한다 | 전날 열어 둔 탭에서 납입일에 메모만 고쳐도 원금이 월 납입액만큼 줄고 "자동 납입" 이력과 어긋난다 | 수정 창을 열 때 항목을 서버에서 다시 읽어 채운다. 창을 연 채 00:10을 넘기는 경우는 남는다(완전히 막으려면 version 비교·409, API 변경) |
| 4 | P3 | 리마인더 [납입] 금액 채우기가 납입 이력 조회 뒤에 들어간다 | REQ-3 | `portfolio.js:2299-2306` | 창을 먼저 띄우고 `await loadDepositHistories` 뒤에 금액을 넣는다 | 응답 전에는 금액이 비어 있고, 그 사이 입력한 값을 덮어쓴다 | 창을 띄우기 전에 금액을 넣는다(`openDepositModal`에 처음 금액 인자) |
| 5 | P3 | 현금성 월 납입액 칸의 안내가 "월 자동납입 금액"이다 | REQ-1 (문구 일관성) | `portfolio-add.html:319`, `portfolio-edit.html:258` | 새 "자동 납입"은 앱이 기록한다는 뜻인데 안내는 은행 자동이체 뜻 그대로다 | 알림 확인 상태에서도 자동으로 기록된다고 오해할 수 있다 | 현금성 칸만 "월 납입 금액"으로 바꾼다 |
| 6 | P3 | 배치 완료 로그의 '대상'이 자동 반영 항목 전체 수다 | 공통 구현 기준(로그) | `PortfolioAutoDepositBatchService.java:70` | `기록={}/대상={}`에 `targets.size()`를 쓴다 | "기록=2/대상=40"이 실패 38건처럼 읽힌다 | 오늘 납입일 항목 수를 따로 세어 남긴다 |

- **요구사항 확인 필요**
  - A. 개설한 달의 첫 회차: 시작일이 같은 달 납입일 이전이면 그 달 납입일에도 자동 기록된다(`CashDetail.java:90`). 등록 원금에 첫 회차가 들어 있으면 그 달에 한 번 더 더해진다.
  - B. 만기가 지난 항목도 자동 반영으로 저장된다. 기록은 생기지 않지만 설명 줄에 "자동 납입"이 보이고, 미납 알림은 계속 뜬다(REQ-11 제외 범위).
  - C. 안내 문구가 건너뛰는 경우(이번 달 기록 있음, 시작일 전, 만기일 이후)와 소급이 없다는 점을 알리지 않는다. 납입일이 지난 뒤 자동 반영으로 바꾸면 그달에는 "자동 납입"과 미납이 함께 보인다.
  - T. 수정 요청으로 AUTO가 저장되는지 확인하는 테스트가 없다(S3는 처리 방식이 없을 때 NOTIFY만 본다). 추가하려면 테스트 시나리오 승인이 필요하다.
- **선택:** 태형님 선택 대기

## 검증
검증 방식은 verify 단계에서 태형님이 고른다. 후보와 이 세션의 제약:
- `./gradlew test`: 임시 Postgres로 실행할 수 있다.
- `bootRun` + curl: 임시 Postgres로 기동해 컬럼 추가, AUTO 등록·400, 응답 필드를 확인한다. cron을 매분으로 덮어쓰고 납입일을 오늘로 둔 항목으로 자동 기록·건너뛰기를 확인할 수 있다.
- 운영 반영 후 확인(태형님): 적금을 자동 납입으로 바꾼 뒤 납입일 다음 날 납입 이력과 원금을 본다.

## 수정 범위
- **수정 가능:** 위 allowed_paths 범위
  - 도메인 `CashDetail`·`PortfolioItem`·`DepositMode`(신규), 항목 저장소 포트
  - 엔티티 `CashItemEntity`, JPA 저장소·구현체, 매퍼, 운영 반영용 SQL
  - `PortfolioService`, `PortfolioAutoDepositBatchService`(신규), `PortfolioAutoDepositScheduler`(신규), `CashDetailResponse`, 컨트롤러, 현금성 요청 DTO 2종, `application.yml`(cron 한 줄)
  - `portfolio.js`, 등록·수정 partial
  - 신규 단위 테스트(승인 후 확정)와 테스트 계획 문서, 매퍼 테스트, 시그니처 변경으로 인자만 넣는 기존 테스트 1종
- **수정 금지:** 위 blocked_paths 범위
  - 납입 이력 도메인·엔티티·저장소, 펀드·연금 상세와 엔티티
  - 스냅샷 스케줄러·배치, 요약·평가·배당 서비스, 알림(메일)·챗봇, 전역 예외 처리기
  - 프로필 설정 파일, `api.js`·`app.js`·`index.html`, 보유 자산·납입 창 partial, `build.gradle`

## 완료 정의
- REQ-1\~8과 REQ-14\~16을 충족하고, REQ-9\~13·17은 제외로 유지한다.
- 현금성 자산에서 처리 방식을 고를 수 있고, 자동 납입 항목은 납입일에 한 번 기록되며, 알림 확인 항목과 기존 데이터는 지금처럼 동작한다.
- 승인된 단위 테스트가 통과한다.
