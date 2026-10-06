# 적금 자동납입 처리 방식 단위 테스트 계획 (#138)

2026-10-06 태형님 승인("끝까지 한번에 진행해"). 승인된 시나리오만 작성한다.
- 처음 승인: 19건(D1~D7, M3~M4, S1~S7, B1~B3)
- 리뷰 선택 T로 추가 승인("권장대로 해", 2026-10-06): D8, S8. 시작한 달을 건너뛰는 규칙(리뷰 A) 때문에 D7의 "시작일 10-25 → true" 1건을 "시작일 09-25 → true"로 바꾼다.

## 테스트 대상
- 대상 클래스/메서드
  - `DepositMode.from`, `CashDetail`(생성자 기본값, `validateDepositMode`, `isAutoDepositDueOn` — 시작한 달 건너뛰기 포함)
  - `PortfolioItemMapper` CASH 변환
  - `PortfolioService.addCashItem`·`updateCashItem`·`recordAutoDeposit`
  - `PortfolioAutoDepositBatchService.recordDueAutoDeposits`
- 대상 유스케이스: 현금성 자산의 납입 처리 방식 등록·수정, 납입일 자동 기록, 일배치 실패 격리와 KST 날짜

## 테스트 범위
- 포함: 도메인 규칙, 매핑 왕복, 서비스 등록·수정·1건 기록, 배치 대상 선별·격리·KST
- 제외: 스케줄러 cron 연결, JPQL 조회(JPA 테스트 제외), 컨트롤러, 화면(CP3 브라우저 하네스로 확인)

## Mock 대상
- Repository: `PortfolioItemRepository`, `DepositHistoryRepository` — 저장 요청을 목록에 모아 상태로 확인한다. 새 항목 저장은 id를 붙인 사본을 돌려준다.
- 기타: `PortfolioService`의 나머지 의존성(매수·매도 이력 저장소, 현금 연결 저장소, 평가·환율·키워드, 이벤트 로거)은 빈 Mock이다. 배치 테스트는 `PortfolioService` Mock의 응답값만 쓴다.
- 시계: 배치는 고정 `Clock`(UTC 순간 + Asia/Seoul)을 생성자로 넣는다.
- 호출 여부 검증(verify)은 쓰지 않는다.

## 테스트 케이스
공통 고정값: 월 납입액 300,000원, 납입일 25일, 원금 1,000,000원, 기준일 2026-10-25.

### 도메인 — `CashDetailDepositModeTest`
| Case | Given | When | Then |
|---|---|---|---|
| D1 처리 방식이 없으면 알림 확인 | 5인자·7인자 생성자, 8인자에 처리 방식 null | `getDepositMode()` | 모두 NOTIFY |
| D2 문자열 변환 | null, "", "  ", "NOTIFY", "AUTO", "WRONG" | `DepositMode.from` | NOTIFY·NOTIFY·NOTIFY·NOTIFY·AUTO, "WRONG"은 `IllegalArgumentException`("납입 처리 방식이 올바르지 않습니다: WRONG") |
| D3 검증 통과 | AUTO + 300,000원 + 25일 / NOTIFY + 월 납입액·납입일 없음 | `validateDepositMode()` | 예외 없음 |
| D4 검증 거부 | AUTO에 (월 납입액 없음, 25일), (0원, 25일), (300,000원, 납입일 없음), (300,000원, 0일), (300,000원, 32일) | `validateDepositMode()` | 모두 `IllegalArgumentException`("자동 납입은 월 납입액과 납입일(1~31일)을 입력해야 합니다.") |
| D5 납입일 판정 | AUTO 25일 / NOTIFY 25일 | `isAutoDepositDueOn` 10-25, 10-24 | AUTO: 10-25 true, 10-24 false / NOTIFY: 10-25 false |
| D6 말일 보정 | AUTO 31일 | `isAutoDepositDueOn` 2026-02-28, 2026-02-27, 2026-04-30 | true, false, true |
| D7 기간 | AUTO 25일, 시작일·만기일 조합 | `isAutoDepositDueOn(10-25)` | 시작일 09-25 → true, 시작일 11-01 → false, 만기일 10-25 → false, 만기일 10-20 → false, 만기일 10-26 → true, 날짜 없음 → true |
| D8 시작한 달 건너뛰기 | AUTO 25일, 시작일 10-05 / 시작일 10-25 | `isAutoDepositDueOn` 10-25, 11-25 | 시작일 10-05: 10-25 false, 11-25 true / 시작일 10-25: 10-25 false |

### 매핑 — `PortfolioItemMapperTest`(기존 파일에 추가)
| Case | Given | When | Then |
|---|---|---|---|
| M3 처리 방식 왕복 | AUTO 현금성 항목 | 도메인 → 엔티티 → 도메인 | 엔티티 값 "AUTO", 복원 값 AUTO |
| M4 기존 행 | 처리 방식 컬럼이 null인 `CashItemEntity` | `toDomain` | NOTIFY |

### 서비스 — `PortfolioServiceAutoDepositTest`
| Case | Given | When | Then |
|---|---|---|---|
| S1 등록 시 처리 방식 | 적금 등록 요청: AUTO + 300,000원 + 25일 / 처리 방식 없음 | `addCashItem` | 응답 `cashDetail.depositMode` "AUTO" / "NOTIFY" |
| S2 등록 검증 | AUTO인데 월 납입액 없음 | `addCashItem` | `IllegalArgumentException`(D4 문구), 저장된 항목 없음 |
| S3 수정 시 덮어쓰기 | 기존 AUTO 적금 | `updateCashItem`에 처리 방식 없음 | 응답 "NOTIFY" |
| S4 자동 기록 | ACTIVE AUTO 적금(원금 1,000,000원), 이번 달 기록 없음 | `recordAutoDeposit(id, 10-25)` | true, 저장된 납입 이력 1건(10-25, 300,000원, 좌수 없음, 메모 "자동 납입"), 저장된 항목 원금 1,300,000원 |
| S5 이번 달 기록 있음 | 같은 항목, 10-03 납입 이력 | `recordAutoDeposit(id, 10-25)` | false, 저장된 납입 이력·항목 없음, 원금 1,000,000원 |
| S6 지난달 기록만 있음 | 같은 항목, 09-25 납입 이력 | `recordAutoDeposit(id, 10-25)` | true, 저장된 납입 이력 1건 |
| S7 건너뛰기 | NOTIFY 적금(10-25) / AUTO 적금(10-24) / CLOSED AUTO 적금(10-25) | `recordAutoDeposit` | 모두 false, 저장된 납입 이력 없음 |
| S8 수정 시 자동 반영 저장 | 기존 NOTIFY 적금 | `updateCashItem`에 AUTO + 300,000원 + 25일 | 응답 "AUTO" |

### 배치 — `PortfolioAutoDepositBatchServiceTest`
| Case | Given | When | Then |
|---|---|---|---|
| B1 KST 날짜 | 시계 2026-10-25T15:10Z(KST 10-26 00:10), 대상 A(26일)·B(25일), `recordAutoDeposit(A, 10-26)`만 true | `recordDueAutoDeposits()` | 1 |
| B2 실패 격리 | 같은 시계, 대상 A·B(26일), A는 예외, B는 true | `recordDueAutoDeposits()` | 1 |
| B3 대상 없음 | 대상 없음 | `recordDueAutoDeposits()` | 0 |

## 검증 명령
- `./gradlew test --tests "*CashDetailDepositModeTest" --tests "*PortfolioItemMapperTest" --tests "*PortfolioServiceAutoDepositTest" --tests "*PortfolioAutoDepositBatchServiceTest"`
