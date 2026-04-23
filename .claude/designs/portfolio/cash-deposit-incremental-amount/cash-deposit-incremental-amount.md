# 현금성 자산 납입 금액을 delta 기반으로 전환

## 배경

- 분석 문서: `.claude/analyzes/portfolio/cash-deposit-amount-drift/cash-deposit-amount-drift.md`
- 관련 이슈: GitHub #30
- 선행 설계 `cash-deposit-preserve-initial-amount` 는 "첫 납입" 엣지 케이스에만 보정을 추가했지만, `updateCashItem` 경유 등으로 `investedAmount` ≠ 이력 합계인 drift 상태에서는 여전히 추가 납입 시 `investedAmount` 유실이 발생.

## 핵심 결정

**납입 이력을 append-only 거래 로그로 취급하고, `investedAmount` 는 delta 로 증감한다.**

- `addDeposit` / `updateDeposit` / `deleteDeposit` 는 `PortfolioItem.recalculateFromDepositHistories` 호출을 제거하고, 이미 존재하는 도메인 메서드 `restoreAmount(delta)` / `deductAmount(delta)` 로 `investedAmount` 를 직접 증감한다.
- 이력 합계는 더 이상 `investedAmount` 의 재계산 소스가 아니다. 이력은 감사/표시용, `investedAmount` 는 독립 관리되는 잔액이다.
- 선행 설계의 "기존 납입분 자동 생성" 보정 로직(`addDeposit` 내 `histories.isEmpty()` 블록)은 **더 이상 필요 없으므로 제거**.

## 대안 (채택하지 않음)

1. **`addDeposit` 보정 조건을 `합계 ≠ investedAmount` 로 확장** — 음수 차액(수수료·해지 손실로 잔액 감액) 처리 규칙이 복잡, 사용자가 명시적으로 낮춰둔 금액을 덮어쓸 위험. 기각.
2. **`updateCashItem` 를 이력 조정 API 로 전환** — 범위가 넓고 UI/validation 변경 수반. 현재 범위 초과. 기각.
3. **재계산 유지 + drift 감지 경고** — 근본 fix 아님. 기각.

## 구현 계획

### 변경 파일

- `src/main/java/.../portfolio/application/PortfolioService.java`
- `src/main/java/.../portfolio/domain/model/PortfolioItem.java` (메서드 1개 제거)

### 변경 내용

#### 1) `addDeposit` (`PortfolioService.java:599-625`)

변경 전:
```java
List<DepositHistory> histories = new ArrayList<>(depositHistoryRepository.findByPortfolioItemId(itemId));
if (histories.isEmpty()) {
    BigDecimal existingAmount = item.getInvestedAmount();
    if (existingAmount != null && existingAmount.compareTo(BigDecimal.ZERO) > 0) {
        DepositHistory initial = DepositHistory.create(
                itemId, resolveInitialDepositDate(item), existingAmount, null, "기존 납입분");
        histories.add(depositHistoryRepository.save(initial));
    }
}
DepositHistory history = DepositHistory.create(itemId, depositDate, amount, units, memo);
DepositHistory saved = depositHistoryRepository.save(history);
histories.add(saved);
item.recalculateFromDepositHistories(histories);
portfolioItemRepository.save(item);
```

변경 후:
```java
DepositHistory saved = depositHistoryRepository.save(
        DepositHistory.create(itemId, depositDate, amount, units, memo));
item.restoreAmount(amount);
portfolioItemRepository.save(item);
```

- `findByPortfolioItemId` 조회 및 "기존 납입분" 보정 로직 전량 제거.
- `validateDepositTarget(item)` 호출은 유지(CASH/FUND 외 차단).

#### 2) `updateDeposit` (`PortfolioService.java:640-660`)

변경 전:
```java
history.update(depositDate, amount, units, memo);
DepositHistory saved = depositHistoryRepository.save(history);
recalculateInvestedAmountFromDeposits(item, itemId);
portfolioItemRepository.save(item);
```

변경 후:
```java
BigDecimal oldAmount = history.getAmount();
history.update(depositDate, amount, units, memo);
DepositHistory saved = depositHistoryRepository.save(history);

BigDecimal delta = amount.subtract(oldAmount);
if (delta.signum() > 0) {
    item.restoreAmount(delta);
} else if (delta.signum() < 0) {
    item.deductAmount(delta.negate());
}
portfolioItemRepository.save(item);
```

#### 3) `deleteDeposit` (`PortfolioService.java:665-681`)

변경 전:
```java
depositHistoryRepository.delete(history);
recalculateInvestedAmountFromDeposits(item, itemId);
portfolioItemRepository.save(item);
```

변경 후:
```java
BigDecimal removed = history.getAmount();
depositHistoryRepository.delete(history);
item.deductAmount(removed);
portfolioItemRepository.save(item);
```

#### 4) 미사용 코드 제거

- `PortfolioService.recalculateInvestedAmountFromDeposits` (`PortfolioService.java:744-750`)
- `PortfolioItem.recalculateFromDepositHistories` (`PortfolioItem.java:303-313`)
- `PortfolioService.resolveInitialDepositDate` (선행 설계에서 도입, 더 이상 사용 없음) — 제거 전 참조 재확인.

## 주의사항

- **`deductAmount` 잔액 부족 예외** (`PortfolioItem.java:178-181`): 사용자가 `updateCashItem` 으로 `investedAmount` 를 이력 합계보다 낮게 조정한 상태에서 `deleteDeposit` / `updateDeposit(감액)` 이 호출되면 예외 발생. 이는 "현재 잔액보다 큰 납입을 되돌릴 수 없다"는 도메인 규칙으로 받아들이고, 별도 완화 처리는 하지 않음. 필요 시 후속 설계에서 다룬다.
- **데이터 마이그레이션 없음**: 기존 drift 상태 데이터(`investedAmount` ≠ 이력 합계)는 현재 값 그대로 유지된다. 이번 변경은 **앞으로의 유실만 차단**한다. 과거 유실된 금액 복구는 수동 보정 영역.
- **이벤트 페이로드**: `publishDepositEvent` 시그니처 변경 없음. 기존 이벤트 구독자 영향 없음.
- **validateDepositTarget** 유지: CASH/FUND 외 타입은 여전히 차단.
- **FUND 적용성**: `restoreAmount`/`deductAmount` 는 assetType 제약 없이 `investedAmount` 를 증감한다. CASH 전용 주석은 있으나 로직상 FUND 에도 동일하게 적용 가능. CASH/FUND 모두 본 설계로 커버됨.

## 테스트 계획

명시적 요청이 없으므로 자동 테스트 코드는 작성하지 않음. 수동 검증 시나리오:

1. **이슈 #30 재현**: DB `investedAmount=1,500,000`, 이력 `[3,000]` 상태에서 4만원 납입 → `investedAmount=1,540,000`, 이력 `[3,000, 40,000]`.
2. **첫 납입**: 신규 CASH 항목(초기 100만원) 등록 → 10만원 납입 → `investedAmount=1,100,000`, 이력 `[100,000]` 1건(※ "기존 납입분" 자동 생성은 더 이상 없음).
3. **납입 수정**: 10만원 → 5만원 수정 → `investedAmount` 5만원 감액.
4. **납입 삭제**: 3천원 이력 삭제 → `investedAmount` 3천원 감액.
5. **updateCashItem 후 납입**: 항목 수정으로 `investedAmount` 를 임의 값으로 변경 후 추가 납입 → 사용자 수정값 + 신규 납입 금액으로 보존.
6. **잔액 부족 엣지**: `investedAmount` 가 이력 금액보다 작은 drift 상태에서 해당 이력 삭제 시 잔액 부족 예외 메시지 확인.

## 작업 리스트

- [x] `addDeposit` 을 `restoreAmount(amount)` delta 방식으로 전환하고 "기존 납입분" 보정 로직 제거
- [x] `updateDeposit` 을 old/new delta 방식으로 전환 (`recalculateInvestedAmountFromDeposits` 호출 제거)
- [x] `deleteDeposit` 을 `deductAmount(history.amount)` 방식으로 전환 (`recalculateInvestedAmountFromDeposits` 호출 제거)
- [x] `PortfolioService.recalculateInvestedAmountFromDeposits` 제거
- [x] `PortfolioItem.recalculateFromDepositHistories` 제거
- [x] `resolveInitialDepositDate` 참조 재확인 후 미사용이면 제거
- [ ] 수동 시나리오 6종 검증 후 커밋