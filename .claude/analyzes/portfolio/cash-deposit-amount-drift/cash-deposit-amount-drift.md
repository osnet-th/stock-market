# 현금성 자산 추가 납입 시 investedAmount-이력 불일치 유실 (Issue #30)

## 현상

이미 납입 이력이 1건 이상 존재하는 CASH 항목에서 추가 납입을 수행하면, 항목의 `investedAmount`가 **납입 이력 합계로 덮어씌워지면서 이력에 반영되지 않은 초기/보유 금액이 유실**됨.

Issue #30 재현 상태:
- DB: `investedAmount = 1,500,000원`, `DepositHistory = [3,000원]` (합 = 3,000원, 불일치 상태)
- 사용자 액션: `addDeposit(40,000원)`
- 결과: `investedAmount = 43,000원` (기대: `1,540,000원`)
- 유실: `1,500,000원`

## 기존 수정과의 차이

선행 분석 `cash-deposit-overwrites-initial-amount` 및 설계 `cash-deposit-preserve-initial-amount` 는 **이력이 비어 있을 때(`histories.isEmpty()`)의 첫 납입 유실**만 해결했음. Issue #30 은 **이미 이력 1건 이상이 있지만 `investedAmount`와 이력 합계가 불일치한 상태**에서 발생하는 별개 엣지 케이스로, 현재 보정 가드(`existing.isEmpty()`)를 통과하지 못해 수정되지 않음.

## 근본 원인

`PortfolioItem.investedAmount`는 **두 가지 방식으로 쓰기 가능**하여 drift 가능성 있음:

1. **이력 기반 재계산 경로** — `addDeposit/updateDeposit/deleteDeposit` → `recalculateFromDepositHistories(histories)` 가 `investedAmount = Σ amount` 로 덮어씀. 이력을 truth source 로 취급.
2. **항목 수정 경로** — `updateCashItem` / `updateFundItem` 내 `item.updateAmount(investedAmount)` 가 납입 이력과 무관하게 `investedAmount` 를 사용자 입력값으로 직접 덮어씀.

경로 2 로 `investedAmount` 만 변경되면 이력과 불일치가 발생하고, 이후 경로 1 이 호출되면 **불일치분이 조용히 유실**됨.

### 불일치 발생 가능 시나리오

- **A**: 보정 로직 배포 이전에 이력 1건이 저장된 레거시 데이터가 남은 경우.
- **B**: 항목 수정(`updateCashItem`)으로 `investedAmount` 만 재지정한 경우. 납입 이력은 변경되지 않아 합계와 어긋남.
- **C**: 자동 생성된 "기존 납입분" 이력을 사용자가 수동 삭제한 경우. 이력은 남은 납입분만 남고, `investedAmount` 는 삭제 당시 재계산된 값으로 고정됨.

## 코드 위치

- `src/main/java/.../portfolio/application/PortfolioService.java:599-625` — `addDeposit`. `histories.isEmpty()` 일 때만 보정 수행(606-614). **이미 이력이 있으나 합계 ≠ investedAmount 인 경우 보정 누락.** 620번 줄 `recalculateFromDepositHistories(histories)` 에서 기존 `investedAmount` 유실.
- `src/main/java/.../portfolio/domain/model/PortfolioItem.java:303-313` — `recalculateFromDepositHistories`. 이력 합계를 `investedAmount` 에 그대로 할당.
- `src/main/java/.../portfolio/application/PortfolioService.java:211-219` — `updateCashItem`. `item.updateAmount(investedAmount)` 로 이력과 독립적 쓰기.
- `src/main/java/.../portfolio/application/PortfolioService.java:452-…` — `updateFundItem`. 동일 패턴 추정, 확인 필요.
- `src/main/java/.../portfolio/domain/model/PortfolioItem.java:163-169` — `updateAmount`. 납입 이력 미고려.

## 영향 범위

- **자산 유형**: CASH, FUND (동일 재계산 경로 사용).
- **사용자 임팩트**: 추가 납입 1회로 잔액이 수백만 원 단위까지 유실 가능. 금액 기반 기능 전반(잔액 표시, 만기 예상액 `calculateExpectedMaturityAmount`, 수익률, 포트폴리오 요약/도넛 차트 비중 등) 왜곡.
- **Deposit 수정/삭제(`updateDeposit`, `deleteDeposit`)**: 이력 1건 이상을 전제로 `recalculateInvestedAmountFromDeposits` 호출. 불일치 상태에서 실행되면 동일하게 유실됨.
- **복구 가능성**: 유실된 이전 `investedAmount` 는 이벤트 발행(`PORTFOLIO_DEPOSIT_ADDED`) 전 스냅샷이 없으므로 이벤트 로그에서도 복원 불가. 사용자 직접 보정 필요.

## 해결 방향 (구현 전 승인 필요)

우선 설계 단계에서 선택해야 할 트레이드오프:

1. **`addDeposit` 보정 조건 확장** — `existing.isEmpty()` → `existing 합계 ≠ item.investedAmount` 로 가드 완화. 불일치분을 "기존 납입분" 이력으로 승격시킨 뒤 재계산. 장점: 기존 패턴 연장, 최소 변경. 단점: 수동 삭제로 의도적으로 줄인 잔액까지 자동 복원될 가능성.
2. **`updateCashItem` / `updateFundItem` 에서 investedAmount 직접 수정을 이력 조정으로 전환** — 금액 변경 시 차이만큼 "조정 이력" 을 추가/삭감. 장점: 이력이 유일한 truth source. 단점: 변경 범위가 넓고 기존 UI/validation 영향.
3. **`recalculateFromDepositHistories` 가 불일치를 감지하면 예외/로그로 가드** — 근본 fix 는 아니지만 회귀 방지.

다음 단계는 위 옵션 중 선택 및 설계 문서 작성.

## 참고

- 선행 분석: `.claude/analyzes/portfolio/cash-deposit-overwrites-initial-amount/cash-deposit-overwrites-initial-amount.md`
- 선행 설계: `.claude/designs/portfolio/cash-deposit-preserve-initial-amount/cash-deposit-preserve-initial-amount.md`
- 관련 이슈: GitHub #30