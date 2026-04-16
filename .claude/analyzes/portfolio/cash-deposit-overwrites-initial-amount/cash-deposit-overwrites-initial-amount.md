# 현금성 자산 납입 추가 시 초기 investedAmount 유실 문제

## 현상

현금성 자산(예금/적금/CMA) 항목에 납입을 추가하면 항목 등록 시 입력한 초기 `investedAmount`가 사라지고, 추가 납입 금액만 잔액으로 남음.

- 예: 초기 등록 100만원 → 10만원 추가 납입 → 잔액 **10만원** (기대값: **110만원**)

## 원인

`PortfolioService.addDeposit()` 은 납입 저장 후 `recalculateInvestedAmountFromDeposits()`를 호출하여 **납입 이력 합계로 investedAmount를 덮어씀**.

- `PortfolioService.java:698-704`
  ```
  List<DepositHistory> histories = findByPortfolioItemId(itemId);
  item.recalculateFromDepositHistories(histories);
  ```
- `PortfolioItem.java:303-313` — 이력 합계를 investedAmount에 그대로 할당.

그러나 현금성 자산 **최초 등록 시점**(`addCashItem`, `PortfolioService.java:172-196`)에는
- `investedAmount`만 설정되고
- `DepositHistory`는 **생성되지 않음**.

따라서 사용자가 첫 납입을 추가하는 순간, 이력에는 방금 넣은 한 건만 존재하고 초기 등록 금액은 이력에 반영되지 않아 유실됨.

## 참고: 주식 쪽은 동일 패턴을 회피하고 있음

`addStockPurchase` (`PortfolioService.java:367-373`) 는 이력이 비어 있으면 현재 보유분(`quantity`, `avgBuyPrice`, "기존 보유분" 메모)으로 **초기 이력을 자동 생성**한 뒤 재계산함. 현금성 자산의 납입 흐름에는 이 보정 로직이 없음.

## 영향 범위

- 현금성 자산(CASH) 첫 납입 추가 시 초기 금액 유실
- 펀드(FUND)도 동일 경로(`addCashItem`이 아닌 `addFundItem` + 최초 `addDeposit`)에서 같은 문제가 발생할 가능성 있음 — `addFundItem`도 납입 이력을 만들지 않음 (`PortfolioService.java:148-167`).
- `updateDeposit`, `deleteDeposit` 도 동일 재계산 경로를 타지만, 이 시점에는 이미 최소 1건의 이력이 있으므로 신규 버그 유발은 아님 (단, 초기 금액이 이력으로 들어오지 않았다면 동일 유실이 누적됨).

## 코드 위치

- `src/main/java/.../portfolio/application/PortfolioService.java:578-591` — `addDeposit`
- `src/main/java/.../portfolio/application/PortfolioService.java:698-704` — `recalculateInvestedAmountFromDeposits`
- `src/main/java/.../portfolio/domain/model/PortfolioItem.java:303-313` — `recalculateFromDepositHistories`
- `src/main/java/.../portfolio/application/PortfolioService.java:172-196` — `addCashItem` (초기 이력 미생성)
- `src/main/java/.../portfolio/application/PortfolioService.java:148-167` — `addFundItem` (초기 이력 미생성)
- 비교 참고: `src/main/java/.../portfolio/application/PortfolioService.java:367-373` — `addStockPurchase` 초기 이력 자동 생성 패턴