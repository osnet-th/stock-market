---
title: "feat: 포트폴리오 배당 집계를 KSD 실지급일 기준으로 전환"
type: feat
issue: 113
issue_url: https://github.com/osnet-th/stock-market/issues/113
status: active
date: 2026-10-03
approved: "2026-10-03 태형님 승인 (기능 plan + stockevaluation domain/service 패키지·포트·어댑터 추가 + basis 값 ACTUAL_PAYMENT_DATE 추가 + 단위 테스트 시나리오 A1~A6·S1~S11). '끝까지 다 진행해'로 PR·병합까지 진행을 지시함"
workflow_exception: "클라우드 세션에 compound-engineering(/ce:plan·/ce:work·/ce:review)이 없어 planning·briefing·review 게이트 절차를 수동 적용한다 (#131·#132와 동일)"
branch: claude/inspiring-wright-rljbsn
branch_exception: "클라우드 세션은 지정 브랜치에만 push할 수 있어 issue/113-{slug} 대신 세션 브랜치를 쓴다 (2026-10-03 태형님 확인, brainstorm 확인 7). validate-plan.sh의 branch 형식 검사 1건은 이 예외로 실패한다."
worktree: /home/user/stock-market-issue-113
worktree_note: "/home/user/stock-market을 가리키는 심볼릭 링크. checkpoint-guard.sh가 경로에서 이슈 번호를 읽기 때문에 둔다."
brainstorm: docs/brainstorms/2026-10-03-portfolio-dividend-ksd-payment-date-brainstorm.md
test_plan_status: approved
schema_plan_status: none
allowed_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/domain/model/DividendSchedule.java
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/domain/service/DividendSchedulePort.java
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/infrastructure/kis/KisDividendScheduleAdapter.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioIncomeService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/PortfolioIncomeResponse.java
  - src/main/resources/static/js/components/portfolio.js
  - src/main/resources/static/partials/portfolio-holdings.html
  - src/test/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioIncomeServiceTest.java
  - src/test/java/com/thlee/stock/market/stockmarket/stockevaluation/infrastructure/kis/KisDividendScheduleAdapterTest.java
  - docs/plans/tests/2026-10-03-113-portfolio-dividend-ksd-payment-date-test-plan.md
  - docs/plans/2026-10-03-001-feat-portfolio-dividend-ksd-payment-date-plan.md
  - docs/brainstorms/2026-10-03-portfolio-dividend-ksd-payment-date-brainstorm.md
  - .claude/issues/113/**
blocked_paths:
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/infrastructure/kis/KisKsdScheduleClient.java
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/infrastructure/kis/dto/**
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/domain/model/KsdScheduleType.java
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/application/**
  - src/main/java/com/thlee/stock/market/stockmarket/stockevaluation/presentation/**
  - src/main/java/com/thlee/stock/market/stockmarket/stock/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/**
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioSummaryService.java
  - src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioEvaluationService.java
  - src/main/resources/application.yml
  - src/main/resources/db/**
  - src/main/resources/static/js/api.js
  - src/main/resources/static/index.html
  - src/main/resources/static/js/app.js
  - build.gradle
---

# 포트폴리오 배당 집계 KSD 실지급일 기준 전환 (#113)

## 요약
- 포트폴리오 상단 "이달 배당 · 이자"에서 국내 주식(개별 종목·ETF) 배당을 KSD 배당일정의 지급일 기준으로 바꾼다. 이달 금액은 지급일이 이번 달(KST)인 주당 현금배당 × 현재 보유 수량의 합이다.
- 국내 주식 연 예상은 이번 달을 포함한 최근 12개월 실지급 배당 × 현재 수량으로 바꾼다. 시가배당률도 이 연 예상으로 계산한다.
- 해외 주식 배당, KSD에 배당 기록이 없는 ETF, 예적금·채권 이자는 지금처럼 연 예상 ÷ 12로 이달 금액에 더한다.
- KSD 조회는 종목 단위로 12시간 캐시한다. 국내 종목 조회가 하나라도 실패하면 국내 배당 전체를 지금 방식으로 계산하고 기존 basis(`ESTIMATED_MONTHLY_AVERAGE`)를 돌려준다. 요약 API는 실패하지 않는다.
- 응답 필드는 그대로 두고 basis 값 `ACTUAL_PAYMENT_DATE`를 추가한다. KPI 카드 캡션은 basis에 따라 항상 기준을 보인다.
- 바뀌지 않는 것: DB, 요약 API 경로·필드, 이자 산출식, 연금 제외, 종목 평가 화면과 기존 KSD 조회 클라이언트

## 작업 리스트
- [x] U1 배당 일정 도메인 모델·조회 포트 (stockevaluation)
- [x] U2 KIS 배당 일정 어댑터·캐시 (테스트 A를 먼저 작성) → 체크포인트 CP1 (2026-10-03, A1~A6 작성 후 컴파일 실패 확인, 구현 후 6건 통과)
- [x] U3 배당·이자 집계 전환과 basis 추가 (테스트 S를 먼저 작성) → 체크포인트 CP2 (2026-10-03, S1~S11 작성 후 컴파일 실패 확인, 구현 후 11건 통과)
- [x] U4 KPI 캡션 → 체크포인트 CP3 (2026-10-03, 브라우저 하네스 5건 통과)
- [x] 단위 테스트 — 승인된 시나리오만, 대상 단위 구현 전에 작성하고 실패를 확인한다 (A1~A6 6건, S1~S11 11건 통과. 임시 Postgres로 전체 203건 통과)
- [x] R1 리뷰 반영 — No.2~7 (2026-10-03, 리뷰 결과 절) → 체크포인트 CP4
- [ ] 검증 (태형님 선택 방식)

## 배경 / 현재 상태
- **집계:** `PortfolioIncomeService.summarize`(`PortfolioIncomeService.java:40`)는 주식을 국내·해외 구분 없이 `dividendYield × 평가액`으로 계산하고(`:51-59`), 이달 금액을 (연 배당 + 연 이자) ÷ 12로 낸다(`:75`). basis는 `ESTIMATED_MONTHLY_AVERAGE` 하나다.
- **노출:** `PortfolioSummaryService.getSummary`가 항목·평가 결과를 넘겨 호출하고(`PortfolioSummaryService.java:130`), `GET /api/portfolio/summary`의 `income`으로 나간다. 다른 호출처는 없다.
- **화면:** `portfolio-holdings.html:42-54`. 기준 캡션은 미입력 항목이 있을 때만 보인다(`:52-54`).
- **KSD 조회:** `KisKsdScheduleClient.fetch`(`KisKsdScheduleClient.java:35`)는 종목 평가 일정 탭만 쓰고, 캐시 없이 첫 페이지만 받는다. 배당일정 응답에 배당금지급일(`divi_pay_dt`)과 주당 현금배당금(`per_sto_divi_amt`)이 있다(`KsdScheduleType.java:16-21`).
- **문서로 확인되지 않은 것:** 기간 필터가 기준일·지급일 중 무엇인지, 지급일·금액 문자열 형식, ETF 분배금 포함 여부. 이 환경에는 KIS 키가 없다.
- **선례:**
  - #121: 포트폴리오 application이 stock 도메인 포트(`MarketCalendarPort`)를 쓰고, 조회 실패 시 경고 로그 후 대체 동작을 한다.
  - #131: Caffeine 캐시를 직접 만들고, 테스트용 생성자로 `Clock`을 주입한다.

## 요구사항 원장
| ID | 요구사항 | 출처 | 이번 범위 | 근거 / 담당 |
|---|---|---|---|---|
| REQ-1 | 국내 주식은 KSD 배당일정의 배당금지급일·현금배당금을 조회해 보유 수량과 곱한 이달 지급 예정액을 산출한다 | 이슈 하고 싶은 것 1 | 포함 | U1~U3, KTD2·KTD5 |
| REQ-2 | KSD 조회 결과를 캐시해 화면 진입마다 외부 호출이 나가지 않게 한다 | 이슈 하고 싶은 것 2 | 포함 | U2, KTD4 |
| REQ-3 | 연 예상 기준을 정한다: 국내 주식은 최근 1년 실지급 배당 × 현재 수량 | 이슈 하고 싶은 것 3, brainstorm 확인 2 | 포함 | U3, KTD2·KTD5 |
| REQ-4 | 해외 주식 배당 처리를 정한다: 대응 소스가 없어 입력 배당률 방식(월 평균)을 유지한다 | 이슈 하고 싶은 것 4, brainstorm 확인 1 | 포함 | U3, KTD5. 현재 코드에 해외 배당 일정 연동이 없다(#110 확인 사실, Finnhub 연동 코드도 없음). 새 소스 도입은 하지 않는다 |
| REQ-5 | KSD 조회 실패 시 입력 배당률 방식으로 폴백하고 basis로 구분한다 | 이슈 하고 싶은 것 5 | 포함 | U3, KTD5 |
| REQ-6 | basis 코드를 확장하고 화면 캡션에 반영한다 | 이슈 하고 싶은 것 6 | 포함 | U3·U4, KTD5·KTD6 |
| REQ-7 | 포트폴리오 화면 레이아웃 변경 | 이슈 범위 밖 | 제외 | 이슈 범위 밖. KPI 카드 안 캡션 줄만 바꾼다(기준 줄 1개 추가, 배치 변경 없음) |
| REQ-8 | 배당 알림/스케줄러 | 이슈 범위 밖 | 제외 | 이슈 범위 밖 |
| REQ-9 | 이달 금액 = 국내 주식 지급일 기준 배당 + 해외 주식 배당·이자의 월 평균 | brainstorm 확인 1 | 포함 | U3, KTD5 |
| REQ-10 | 국내 종목 중 하나라도 조회에 실패하면 국내 배당 전체를 입력 배당률 방식으로 계산하고, 실패는 캐시하지 않는다 | brainstorm 확인 3 | 포함 | U2·U3, KTD4·KTD5 |
| REQ-11 | 배당 금액은 현재 보유 수량으로 계산한다 | brainstorm 확인 4 | 포함 | U3 |
| REQ-12 | 응답 필드는 그대로 두고 basis 값만 추가한다 | brainstorm 확인 5 | 포함 | U3, API 계약 |
| REQ-13 | KSD에 배당 기록이 없는 ETF는 입력 배당률 방식으로 계산한다 | brainstorm 확인 6 | 포함 | U3, KTD5 |
| REQ-14 | KSD 조회에 실패해도 요약 API는 실패하지 않는다 | brainstorm 목표 | 포함 | U3 |
| REQ-15 | "이달"과 최근 12개월은 KST 기준으로 계산한다 | brainstorm 현재 코드 확인(시간대) | 포함 | U3, KTD2 |
| REQ-16 | KPI 캡션이 미입력 항목 유무와 관계없이 항상 집계 기준을 보인다 | brainstorm 문제 정의 3 | 포함 | U4, KTD6 |
| REQ-17 | 기준일 시점 보유 수량 계산(매수·매도 이력 기반) | brainstorm 확인 4 대안 | 제외 | 태형님 결정(현재 수량 사용) |
| REQ-18 | 예적금·CMA·채권 이자 산출식 변경 | brainstorm 제외 범위 | 제외 | 이슈 대상은 배당이다. 산출식은 그대로 두고 월 평균으로 더한다 |

## 핵심 기술 결정
**KTD1 — 배당 일정은 stockevaluation 도메인 포트로 조회한다.**
- `stockevaluation.domain.service.DividendSchedulePort`(신규 패키지)와 모델 `DividendSchedule`을 두고, `KisDividendScheduleAdapter`가 기존 `KisKsdScheduleClient`로 구현한다.
- 포트폴리오 application은 포트만 의존한다(#121 `MarketCalendarPort` 선례). application의 직접 외부 호출 금지 규칙을 지킨다.
- `KisKsdScheduleClient`, `KsdScheduleType`, `StockEvaluationService`, 종목 평가 API는 바꾸지 않는다.

**KTD2 — 넓게 조회하고 지급일로 거른다(KST 기준).**
- 기준 달은 `YearMonth.now(clock)`이고, clock은 `Asia/Seoul`이다.
- 조회 기간: 기준 달 1일의 17개월 전 1일 ~ 기준 달 말일
- 이달: 지급일이 기준 달 1일 ~ 말일
- 연 예상: 지급일이 기준 달 1일의 11개월 전 1일 ~ 기준 달 말일(이번 달 포함 12개월)
- 기간 필터가 지급일이면 조회 기간이 연 예상 기간을 덮는다. 기준일이면 지급이 기준일보다 6개월까지 늦어도 덮는다(결산배당은 기준일 뒤 4개월 안팎).
- 지급일이 없는 일정은 어느 집계에도 넣지 않는다.

**KTD3 — 응답 변환은 행 단위로 관대하게 하고, 조회 실패와 구분한다.**
- 지급일: 숫자만 남겨 8자리면 `yyyyMMdd`로 읽는다. 그 외에는 지급일 없음으로 둔다.
- 현금배당금: 콤마·공백(유니코드 공백 포함)을 지우고 숫자로 읽는다. 비었거나 숫자가 아니거나 0 이하면 그 행을 뺀다(주식배당 등).
- 응답 목록이 null이면 빈 목록이다.
- 통신 실패와 응답 실패 코드(`KisApiException`)는 그대로 던진다. 변환에서 빠진 행은 실패가 아니다.

**KTD4 — 캐시는 어댑터 안 Caffeine 캐시로 둔다(#131 방식).**
- 키는 종목코드 + 조회 시작일 + 종료일, 값은 변환된 배당 일정 목록(빈 목록 포함)이다.
- 쓰기 후 12시간 만료, 최대 500개
- `cache.get(key, 로더)`를 쓴다. 로더가 예외를 던지면 캐시에 남지 않고 호출자에게 전달된다.
- 설정 파일은 바꾸지 않고 상수로 둔다.

**KTD5 — 집계 규칙**
- **국내 판정:** `StockDetail.market`을 `MarketType`으로 바꿔 `isDomestic()`으로 판정한다. 시장 값이 없거나 해석할 수 없으면 해외와 같이 계산한다.
  - 종목코드가 비어 있으면 KSD로 조회하지 않고 해외와 같이 계산한다. 빈 종목코드로 조회하면 출처가 전체 종목 일정을 돌려주기 때문이다(구현 중 보완, 2026-10-03).
- **조회:** 국내 주식 종목코드(중복 제거)마다 포트를 차례로 호출한다. 하나라도 예외가 나면 경고 로그를 남기고 남은 조회를 멈춘 뒤 폴백한다. 실패가 이어질 수 있어 경고 로그에는 예외 메시지만 남긴다(리뷰 No.3).
- **조회 성공 시 국내 주식:**
  - 이달 += Σ(이달 지급 현금배당 × 수량), 연 배당 += Σ(12개월 지급 현금배당 × 수량). 수량이 없으면 0이다.
  - KSD에 배당이 없는 개별 종목은 0이고 `excludedCount`에 세지 않는다.
  - KSD 배당 목록이 빈 ETF는 입력 배당률 방식으로 계산한다.
  - basis는 `ACTUAL_PAYMENT_DATE`다.
- **입력 배당률 방식(해외 주식, 빈 ETF, 폴백 시 국내 주식):** 연 배당 += 평가액 × 배당률 ÷ 100이고, 그 값을 12로 나눠 이달에 더한다. 배당률이 없거나 0 이하면 `excludedCount` +1(현행).
- **폴백:** 국내 주식 전부를 입력 배당률 방식으로 계산하고 basis는 `ESTIMATED_MONTHLY_AVERAGE`다.
- **이자:** 현행(원금 × 금리)과 같고, 12로 나눠 이달에 더한다.
- **결과:**
  - monthAmount = 이달 지급 배당 + (입력 배당률 방식 연 배당 + 연 이자) ÷ 12
  - yearEstimate = 연 배당 + 연 이자
  - dividendYield = 연 배당 × 100 ÷ (연 배당이 0보다 큰 주식 항목의 평가액 합)
  - 반올림은 현행처럼 소수 둘째 자리 HALF_UP. yearEstimate도 소수 둘째 자리로 맞춘다(리뷰 No.2)
  - 항목별 금액은 불변 값 객체로 더한다(리뷰 No.5)
- 연금·부동산 등은 현행대로 집계하지 않는다.

**KTD6 — 캡션**
- `portfolio.js`에 basis별 캡션을 돌려주는 헬퍼를 두고, KPI 카드에 항상 보이는 기준 줄을 추가한다.
  - `ACTUAL_PAYMENT_DATE`: "국내 배당 지급일 기준 · 그 외 배당·이자 월 평균"
  - 그 밖(`ESTIMATED_MONTHLY_AVERAGE`): "배당 일정 조회 실패 · 월 평균 환산 기준"
- 미입력 줄은 "배당률·금리 미입력 N건 제외"만 남긴다. 기준 문구는 위 기준 줄로 옮긴다.

## API 계약
`GET /api/portfolio/summary`의 경로·인증·필드는 그대로다. `income` 필드의 의미만 아래처럼 바뀐다.

| 필드 | 값 |
|---|---|
| `monthAmount` | 국내 이달 지급 배당 + (해외 주식·빈 ETF 배당 + 이자) ÷ 12. 시장 값이 없거나 해석할 수 없는 주식, 종목코드가 없는 주식은 해외 주식에 포함한다. 폴백이면 (연 배당 + 연 이자) ÷ 12 |
| `yearEstimate` | 국내 12개월 실지급 + 해외 주식·빈 ETF 입력 배당률 기준 + 연 이자(소수 둘째 자리). 폴백이면 현행 |
| `dividendYield` | 연 배당 ÷ 배당이 있는 주식 평가액 합 × 100. 대상이 없으면 null |
| `basis` | `ACTUAL_PAYMENT_DATE`(신규) / `ESTIMATED_MONTHLY_AVERAGE`(폴백) |
| `excludedCount` | 배당률·금리가 없어 빠진 항목 수. KSD로 계산한 국내 주식은 세지 않는다 |

## 구현 단위
### U1 배당 일정 도메인 모델·조회 포트
- `stockevaluation/domain/model/DividendSchedule.java`(record): `paymentDate`(null 가능), `cashPerShare`(0보다 커야 하며 생성 시 검사)
  - `isPaidBetween(from, to)`: 지급일이 있고 기간 안이면 true
  - `amountFor(quantity)`: 현금배당 × 수량
- `stockevaluation/domain/service/DividendSchedulePort.java`: `List<DividendSchedule> findCashDividends(String stockCode, LocalDate from, LocalDate to)`
  - 빈 목록은 "배당 일정 없음"이고, 조회 실패는 예외다(`MarketCalendarPort`와 같은 구분).

### U2 KIS 배당 일정 어댑터·캐시 (선행 U1)
- `stockevaluation/infrastructure/kis/KisDividendScheduleAdapter.java`: KTD3 변환, KTD4 캐시
- 승인된 테스트 A를 먼저 작성하고 실패를 확인한다.

### U3 배당·이자 집계 전환 (선행 U1, U2)
- `PortfolioIncomeService`
  - 포트와 Clock을 주입한다. `@Autowired` 공개 생성자는 `Clock.system(Asia/Seoul)`을 쓰고, 테스트용 패키지 생성자를 둔다.
  - KTD2·KTD5 규칙, `@Slf4j` 경고 로그
- `PortfolioIncomeResponse`: 필드는 그대로 두고 Javadoc에 basis 값 설명을 추가한다.
- `PortfolioSummaryService`의 호출부(`summarize(items, evaluation)`)는 그대로다.
- 승인된 테스트 S를 먼저 작성하고 실패를 확인한다.

### U4 KPI 캡션 (선행 U3)
- `portfolio.js`: 캡션 헬퍼 1개
- `portfolio-holdings.html:46-54`: 기준 줄 추가, 미입력 줄 문구 정리

## 시스템 전반 영향
- 요약 API가 캐시가 비었을 때 국내 보유 종목마다 KSD를 조회한다. 캐시는 사용자 간에 공유된다.
- 외부 호출은 기존 요약의 읽기 전용 트랜잭션 안에서 일어난다. 시세 조회와 같은 구조다.
- 요약 API 경로·필드와 DB는 바뀌지 않는다. basis 값을 쓰는 화면 코드는 KPI 카드뿐이고 이번에 함께 바꾼다.
- 종목 평가 일정 탭은 지금처럼 캐시 없이 조회한다.

## 위험과 완화
| 위험 | 완화 |
|---|---|
| 실응답 형식이 예상과 달라 지급일이 모두 비면 국내 배당이 조용히 0이 된다 | 변환을 관대하게 두고, 운영 반영 후 종목 평가 일정 탭 값과 KPI를 대조한다(검증 절) |
| 기간 필터 기준을 모른다 | KTD2 넓은 조회 + 지급일 필터로 어느 쪽이어도 결과가 같다 |
| ETF 분배금 포함 여부를 모른다 | KSD 배당 목록이 빈 ETF는 입력 배당률 방식(REQ-13) |
| 캐시가 비면 국내 종목 수만큼 순차 호출해 첫 요약이 느려진다 | 12시간 캐시를 사용자 간에 공유하고, 첫 실패에서 조회를 멈춘다 |
| 한 종목의 18개월 행이 첫 페이지를 넘으면 일부가 빠진다 | 분기배당도 6건 안팎이라 가능성이 낮다. 연속조회는 이번에 넣지 않는다 |
| 기준일 뒤에 사고판 경우 실제 지급액과 다르다 | 태형님 결정(REQ-11). 캡션에 기준을 보인다 |
| 신규 배당 공시가 최대 12시간 늦게 반영된다 | 배당 일정 특성상 수용한다 |
| KIS가 "조회 자료 없음"을 실패 코드로 주면 무배당 종목·분배금 없는 ETF 때문에 요청마다 폴백한다(CP1 가드 리뷰) | 종목 평가 일정 탭은 12종 일정의 빈 결과를 "해당 기간 일정이 없습니다"로 보여 주는 구조라 성공·빈 목록으로 온다고 본다. 운영 반영 후 확인 항목에 넣는다 |

## 단위 테스트 계획
- 테스트 작성: 작성함 (2026-10-03 태형님 확인, brainstorm 확인 7)
- 테스트 계획 문서: [테스트 계획](./tests/2026-10-03-113-portfolio-dividend-ksd-payment-date-test-plan.md)
- 사용자 승인: 테스트 시나리오 승인됨 (2026-10-03)
- 승인된 테스트 시나리오: 정상 7건, 예외·경계 10건 (어댑터 A1~A6, 집계 S1~S11)
- 검증 명령: `./gradlew test --tests "*KisDividendScheduleAdapterTest" --tests "*PortfolioIncomeServiceTest"`
- 작성 순서: 어댑터 테스트는 U1 후 U2 구현 전에, 집계 테스트는 U2 후 U3 구현 전에 작성하고 실패를 확인한다.

## 작업 진행 방식 (워크플로우 예외)
이 세션에는 compound-engineering(`/ce:work`·`/ce:review`)이 없어 같은 절차를 수동으로 적용한다(#131·#132와 동일).
- **구현:** 작업 리스트 순서대로 한 단위씩 진행한다.
  - 단위마다 변경 파일이 allowed_paths 안이고 blocked_paths 밖인지 확인한다.
  - 테스트 대상 단위는 테스트를 먼저 작성하고 실패를 확인한다.
  - 각 단위를 마치면 태형님께 다음 진행 여부를 확인한다.
- **체크포인트:** CP마다 `checkpoint-guard.sh`로 경로를 검사하고 read-only 가드 리뷰를 수행한다.
- **리뷰:** 리뷰 게이트 문서 기준으로 finding 표를 만들고, 태형님이 고른 이슈만 수정한다.

## 체크포인트
- CP1 (U2 후): 어댑터 변환·캐시, 테스트 A 통과
- CP2 (U3 후): 집계 규칙·폴백·basis, 테스트 S 통과
- CP3 (U4 후): 캡션. 저장소 밖 브라우저 하네스(실제 JS·partial 사용)로 basis 두 값과 미입력 건수에 따른 문구를 확인한다.

### CP1 결과 (2026-10-03)
- 경로 검사 통과(변경 4개 파일 모두 plan 범위 안). 가드 리뷰 판정 CLEAN.
- 가드 리뷰 참고 처리
  - 금액의 공백을 앞뒤만 지워 KTD3 문구("콤마·공백을 지우고")와 달랐다 → 모든 공백을 지우도록 맞췄다(CP2 커밋에 포함).
  - KIS가 "조회 자료 없음"을 실패 코드로 줄 가능성 → 위험과 완화, 운영 반영 후 확인 항목에 추가했다.
  - 빈 종목코드로 조회하면 전체 종목이 온다 → U3에서 조회 대상에서 뺀다(KTD5 보완).
  - 수량 null → U3에서 0으로 계산한다(S11).
  - 행 제외·지급일 해석 실패 로그가 없다 → plan 범위 밖이라 보류한다.

### CP2 결과 (2026-10-03)
- 경로 검사 통과(변경 5개 파일 모두 plan 범위 안). 가드 리뷰 판정 CLEAN. KST 기간·국내 판정·폴백·ETF 분기·결과식이 KTD2·KTD5·API 계약과 같고, `PortfolioSummaryService` 호출부는 그대로다.
- 절차 참고: 빈 종목코드 제외는 구현 중 보완이라 승인 기록과 승인된 테스트 시나리오에 없다. 태형님의 "끝까지 다 진행해" 지시에 따라 진행했고, 최종 보고에서 따로 알린다. 승인되지 않은 테스트는 추가하지 않는다.
- 가드 리뷰 참고(리뷰 단계에서 함께 판단): 계속 실패하는 종목의 요청마다 재조회·스택트레이스 경고 로그, 캐시가 빌 때의 순차 호출 지연(수용한 위험), `yearEstimate` 자릿수 미정규화, 국내 종목이 없을 때도 basis가 `ACTUAL_PAYMENT_DATE`(S10 승인 동작), 금액 공백 제거가 ASCII 공백만 처리

### CP3 결과 (2026-10-03)
- 경로 검사 통과(변경 3개 파일 모두 plan 범위 안). 가드 리뷰 판정 CLEAN. 캡션 문구가 KTD6와 같고, 헬퍼 이름이 다른 컴포넌트와 겹치지 않는다.
- 브라우저 하네스(저장소 밖, 실제 partial의 카드 마크업 + 실제 `portfolio.js`) 5건 통과
  - C1 `ACTUAL_PAYMENT_DATE`, 제외 0건 → 기준 줄만 보이고 미입력 줄은 숨는다.
  - C2 제외 2건 → 기준 줄과 "배당률·금리 미입력 2건 제외"가 함께 보인다.
  - C3 `ESTIMATED_MONTHLY_AVERAGE` → "배당 일정 조회 실패 · 월 평균 환산 기준"
  - C4 income 없음 → 캡션이 숨고 이달 금액은 "—"
  - C5 콘솔·페이지 오류 없음
- 가드 리뷰 참고: 모바일 2열 카드에서 캡션이 두 줄로 접힐 수 있다(가로 넘침 없음). 기존 미입력 캡션도 길이가 비슷해 수용한다.

## 리뷰 결과 (2026-10-03)
- **방식:** 이 세션에는 compound-engineering 리뷰 명령이 없어 리뷰 게이트 기준으로 수동 리뷰했다. 백엔드 정합성·아키텍처 리뷰와 요구사항·API 계약·화면 리뷰를 따로 수행하고, CP1~CP3 가드 리뷰 참고를 합쳤다.
- **결과:** P1 없음. 의존 방향, domain 순수성, `@Transactional` 위치, self-invocation 위반이 없다. REQ는 모두 충족이거나 제외 결정대로다.

| No | Severity | Issue | 관련 요구사항 | 위치 | 코드 맥락 | 영향 | 수정 방향 |
|---|---|---|---|---|---|---|---|
| 1 | P2 | 빈 종목코드 조회 제외를 구현 중 KTD5에 넣었지만 승인 기록이 없다 | REQ-1, plan 변경 재승인 기준 | `PortfolioIncomeService.domesticStockDetail` | 빈 코드면 해외 주식처럼 계산 | 절차 문제. 막는 쪽 보완이라 기능 위험은 없다 | 코드 유지, 최종 보고에서 확인 요청 |
| 2 | P3 | `yearEstimate` 자릿수가 고정되지 않는다 | REQ-12, KTD5 | `PortfolioIncomeService.toResponse` | 연 예상에 `setScale` 없음 | 응답 숫자 형식이 기존 `x.00`과 달라진다 | 소수 둘째 자리로 맞춤 |
| 3 | P3 | 폴백 경고 로그가 요청마다 스택트레이스를 남긴다 | 공통 로그 기준, REQ-14 | `PortfolioIncomeService.loadDomesticSchedules` | `log.warn(.., e)`, 실패는 캐시하지 않음 | 실패가 이어지면 로그가 쌓인다 | 예외 메시지만 남김 |
| 4 | P3 | KSD 기록이 있는 ETF 경로와 빈 종목코드 경로에 테스트가 없다 | REQ-13, KTD5 | `stockDividendOf`, `domesticStockDetail` | 두 분기를 검증하지 않음 | 회귀가 나도 못 잡는다 | 테스트 S12·S13 추가 |
| 5 | P3 | 내부 누적 클래스가 "record·inner static class는 DTO/VO 용도만" 규칙에 어긋난다 | 공통 컨벤션 | `PortfolioIncomeService.Totals` | 값이 바뀌는 누적기에 산식까지 있음 | 규칙 위반(기능 영향 없음) | 불변 값 객체(record)로 바꿈 |
| 6 | P3 | 문서 불일치: API 계약 표·응답 Javadoc의 월 평균 대상 누락, REQ-7 근거 문구 | REQ-7, REQ-12 | plan, `PortfolioIncomeResponse` Javadoc | 시장 값·종목코드 없는 주식 누락, "문구만" 표현 | 나중에 오해 소지 | 문구 보강 |
| 7 | P3 | 금액 공백 제거가 ASCII 공백만 처리한다 | KTD3 | `KisDividendScheduleAdapter.parseAmount` | `[,\s]` | 유니코드 공백이 섞이면 행이 빠진다 | 유니코드 공백 포함 |
| 8 | P3 | 캐시가 비면 외부 호출을 차례로 하고, 읽기 전용 트랜잭션 안에서 실행한다 | 위험과 완화(수용) | `loadDomesticSchedules` ← `PortfolioSummaryService.getSummary` | 국내 종목 수만큼 호출 | 첫 요약 지연, DB 커넥션 점유 | 보류. 요약 서비스(수정 금지 경로)를 바꿔야 한다 |
| 9 | P3 | 어댑터가 빈 종목코드를 거부하지 않는다 | KTD1, KTD5 | `KisDividendScheduleAdapter.findCashDividends` | 호출처 가드에만 의존 | 다른 호출처가 생기면 전체 종목을 조회할 수 있다 | 보류. 지금 호출처는 집계 서비스뿐이고 거기서 막는다 |

- **CP4 결과:** 경로 검사 통과(6개 파일), 가드 리뷰 CLEAN. 합계 값 객체 변경은 자릿수 고정 외에 동작이 같다. 참고에 따라 경고 로그를 예외 요약(`toString`)으로 바꾸고, 운영 확인 문구와 월 평균 대상 문구(해석할 수 없는 시장 값)를 보완했다.
- **선택:** 태형님의 "끝까지 다 진행해" 지시에 따라 권장안을 적용했다. No.2~7은 수정하고, No.1은 코드를 유지한 채 최종 보고에서 확인을 요청한다. No.8·9는 보류한다.
- **수정 결과 (R1):**
  - No.2: `yearEstimate`를 소수 둘째 자리로 맞췄다. S1이 자릿수까지 비교하도록 보강했다.
  - No.3: 경고 로그에 종목코드와 예외 요약(예외 종류와 메시지)만 남긴다. 스택트레이스는 남기지 않는다.
  - No.4: S12·S13을 추가했다(테스트 계획 리뷰 보강). 변이 3종이 모두 검출되는 것을 확인했다.
  - No.5: 누적 클래스를 불변 값 객체 `IncomeTotals`(합산 `plus`, 정적 생성)로 바꾸고, 응답 계산은 서비스의 `toResponse`로 옮겼다.
  - No.6: API 계약 표, 응답 Javadoc, REQ-7 근거를 고치고, REQ-4 근거에 해외 배당 연동이 없다는 사실을 적었다.
  - No.7: 공백 제거에 유니코드 공백(`\p{Z}`)을 넣었다.
  - 대상 테스트 19건 통과(A1~A6, S1~S13)
- **요구사항 확인 필요 (운영 확인 후 판단, 이번에는 바꾸지 않음):**
  - KIS가 "조회 자료 없음"을 실패 코드로 주는지(위험과 완화에 기록)
  - 응답 행 종목코드가 요청 종목과 같은지(`SHT_CD` 필터가 정확히 일치하는지)
  - 18개월 조회가 KIS 조회 기간 상한에 걸리는지. 걸리면 늘 폴백되고 캡션이 "배당 일정 조회 실패"로 보인다.
  - ETF를 개별주식으로 등록했고 KSD에 분배금이 없으면, 입력 배당률이 무시되고 0으로 보인다.
  - 국내 주식이 없어도 basis가 `ACTUAL_PAYMENT_DATE`라 캡션이 "국내 배당 지급일 기준"으로 보인다(S10 승인 동작, 유지).
  - 연 예상 기준이 바뀐 점이 캡션에 따로 드러나지 않는다(기준 줄이 이달·연 예상 모두에 해당한다고 보고 유지).

## 검증
검증 방식은 verify 단계에서 태형님이 고른다. 후보와 이 세션의 제약:
- `./gradlew compileJava`, `./gradlew test`: 이 세션에서 실행할 수 있다.
- `bootRun` + curl: 로컬 Postgres로 기동할 수 있지만 KIS 키가 없어 KSD 조회는 실패한다. 폴백 경로(basis `ESTIMATED_MONTHLY_AVERAGE`)와 요약 API 정상 응답만 확인할 수 있다.
- 운영 반영 후 확인(태형님)
  - 국내 배당주를 보유한 계정에서 KPI 이달 금액·캡션을 종목 평가 일정 탭의 지급일·현금배당금과 대조한다.
  - 무배당 종목을 종목 평가 일정 탭(배당일정)에서 조회해 오류가 아니라 "해당 기간 일정이 없습니다"로 나오는지 본다. 오류로 나오면 무배당 종목 보유 시 매번 폴백되므로 후속 조치가 필요하다.
  - KPI 캡션이 "배당 일정 조회 실패"로 보이면 원인을 확인한다. 통신 오류는 서버 경고 로그의 reason에 상세 메시지가 남지만, KIS 응답 실패 코드는 "DIVIDEND 조회 실패"로만 남아 조회 기간 상한·자료 없음을 구분할 수 없다. 이때는 종목 평가 일정 탭에서 같은 종목·기간을 조회해 본다.

## 수정 범위
- **수정 가능:** 위 allowed_paths 범위
  - stockevaluation 신규 3종(모델·포트·어댑터)
  - `PortfolioIncomeService`, `PortfolioIncomeResponse`(Javadoc)
  - `portfolio.js`, `portfolio-holdings.html`
  - 단위 테스트 2종과 테스트 계획 문서
- **수정 금지:** 위 blocked_paths 범위
  - 기존 KSD 클라이언트·응답 DTO·일정 종류, 종목 평가 application·presentation
  - stock 도메인, 포트폴리오 domain·infrastructure·presentation, `PortfolioSummaryService`·`PortfolioEvaluationService`
  - 설정 파일, DB 리소스, `api.js`·`index.html`·`app.js`·`build.gradle`

## 완료 정의
- REQ-1~6과 REQ-9~16을 충족하고, REQ-7·8·17·18은 제외로 유지한다.
- 국내 배당주가 있는 포트폴리오에서 이달 금액이 지급일 기준으로 계산되고, 캡션이 기준을 보인다.
- KSD 조회가 실패해도 요약 API가 정상 응답하고, basis로 폴백이 구분된다.
- 승인된 단위 테스트가 통과하고, 요약 API 경로·필드와 DB는 바뀌지 않는다.
