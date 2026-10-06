# 만기 지난 현금성 항목 납입 알림 단위 테스트 계획 (#140)

2026-10-06 태형님 승인("권장대로 끝까지 한번에 진행해"). 승인된 시나리오만 작성한다.
- 승인: 7건(E1\~E2, R1\~R5)

## 테스트 대상
- 대상 클래스/메서드
  - `CashDetail.isMaturedOn`
  - `PortfolioService.isDepositOverdue`·`isDepositDueToday`
- 대상 유스케이스: 만기된 현금성 항목을 납입일 당일·미납 판정에서 뺀다

## 테스트 범위
- 포함: 도메인 만기 판단, 서비스 당일·미납 판정(만기 전·당일·후, 만기일 없음, 펀드)
- 제외
  - `getItems`의 KST 기준일 연결과 응답 값: `bootRun` + curl로 확인한다.
  - 화면(설명 줄, 등록·수정 모달 안내): 브라우저 하네스로 확인한다.
  - 자동 납입 판정 동작 유지: 기존 `CashDetailDepositModeTest`(D6·D7)로 확인한다.

## Mock 대상
- `PortfolioService`의 의존성(저장소·평가·환율·키워드·이벤트 로거)은 빈 Mock이다. 두 판정 함수는 저장소를 쓰지 않는다.
- 호출 여부 검증(verify)은 쓰지 않는다.

## 테스트 케이스
공통 고정값: 납입일 25일, 월 납입액 300,000원, 납입 기록 없음.

### 도메인 — `CashDetailMaturityTest`
| Case | Given | When | Then |
|---|---|---|---|
| E1 만기일 앞뒤 | 만기일 2026-10-25인 적금 | `isMaturedOn` 2026-10-24 / 10-25 / 10-26 | false / true / true |
| E2 만기일 없음 | 만기일 없는 적금, CMA | `isMaturedOn` 2026-10-25 | false |

### 서비스 — `PortfolioServiceDepositReminderTest`
| Case | Given | When | Then |
|---|---|---|---|
| R1 만기 전 | 만기일 2027-09-01인 적금 | 기준일 10-25 `isDepositDueToday` / 10-26 `isDepositOverdue` | true / true |
| R2 만기일 당일 | 만기일 10-25인 적금 / 만기일 10-26인 적금 | 기준일 10-25 `isDepositDueToday` / 기준일 10-26 `isDepositOverdue` | false / false |
| R3 만기 지남 | 만기일 2026-09-30인 적금 | 기준일 10-25 `isDepositDueToday` / 10-26 `isDepositOverdue` | false / false |
| R4 만기일 없음 | 만기일 없는 적금 | 기준일 10-25 `isDepositDueToday` / 10-26 `isDepositOverdue` | true / true |
| R5 펀드 | 납입일 25일 펀드 | 기준일 10-26 `isDepositOverdue` | true |

## 검증 명령
- `./gradlew test --tests "*CashDetailMaturityTest" --tests "*PortfolioServiceDepositReminderTest" --tests "*CashDetailDepositModeTest"`
