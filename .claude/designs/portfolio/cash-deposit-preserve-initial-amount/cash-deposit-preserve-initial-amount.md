# 현금성 자산 첫 납입 시 초기 금액 보존

## 배경

분석 문서: `.claude/analyzes/portfolio/cash-deposit-overwrites-initial-amount/cash-deposit-overwrites-initial-amount.md`

현금성 자산 최초 납입 추가 시, 재계산 로직이 납입 이력 합계로 `investedAmount`를 덮어써 초기 등록 금액이 유실됨.

## 핵심 결정

**주식의 `addStockPurchase` 패턴을 차용**: `addDeposit` 진입 시 납입 이력이 비어 있고 `investedAmount > 0` 이면, 현재 `investedAmount`를 **"기존 납입분"** 메모의 초기 `DepositHistory`로 자동 생성한 뒤 신규 납입을 저장하고 재계산한다.

- 초기 이력의 `depositDate`는 CASH의 경우 `cashDetail.getStartDate()`, 없으면 `LocalDate.now()`. FUND는 `LocalDate.now()`.
- `units`는 CASH/FUND 모두 납입 이력에서 현재 선택적으로 관리 중이므로 `null` 허용 (현재 `DepositHistory.create` 시그니처 유지).
- 이 보정은 `addDeposit` 한 곳에서만 수행. `updateDeposit`/`deleteDeposit`는 기존 경로 유지(이 시점엔 이력이 존재).

## 대안 (채택하지 않음)

1. `recalculateFromDepositHistories`가 `investedAmount`에 **더하기**로 동작 → 재계산의 의미가 달라져 수정/삭제 경로가 깨짐. 기각.
2. `addCashItem`/`addFundItem` 시점에 초기 `DepositHistory`를 항상 생성 → 기존 데이터 마이그레이션 필요, 범위 확대. 이번 범위는 최소 수정.

## 구현 계획

### 변경 파일

- `src/main/java/.../portfolio/application/PortfolioService.java`

### 변경 내용

`addDeposit` 메서드 (라인 577-591) 에서 신규 `DepositHistory` 저장 직전에 아래 보정 수행:

```
List<DepositHistory> existing = depositHistoryRepository.findByPortfolioItemId(itemId);
if (existing.isEmpty()
        && item.getInvestedAmount() != null
        && item.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0) {
    LocalDate initialDate = resolveInitialDepositDate(item);
    DepositHistory initial = DepositHistory.create(
            itemId, initialDate, item.getInvestedAmount(), null, "기존 납입분");
    depositHistoryRepository.save(initial);
}
```

`resolveInitialDepositDate(PortfolioItem)` private 헬퍼 추가:
- CASH + `cashDetail.startDate` 존재 시 → 해당 날짜
- 그 외 → `LocalDate.now()`

이후 기존 흐름(신규 이력 저장 → 재계산 → 저장)을 그대로 타므로 합계가 `초기금액 + 신규납입`으로 복원됨.

### 주의사항

- `validateDepositTarget` 이후에 보정을 수행해야 함(CASH/FUND 외 타입 차단 유지).
- 중복 생성 방지: 반드시 `existing.isEmpty()` 가드를 통과한 경우에만.
- 기존 사용자(초기 금액 + 이미 납입 이력 1건 이상 있는 상태) 에는 영향 없음.

## 테스트 계획

명시적 요청이 없으므로 테스트 코드 작성은 생략. 수동 확인 시나리오:

1. 신규 CASH 항목 등록(초기 100만원) → 10만원 납입 추가 → 잔액 110만원, 이력 2건("기존 납입분" 100만원 + 신규 10만원).
2. 이미 납입 이력이 있는 항목에 추가 납입 → 보정 미수행, 기존 동작 유지.
3. 초기 금액 0원으로 등록된 항목 첫 납입 → 보정 미수행, 신규 납입만 기록.

## 작업 리스트

- [x] `PortfolioService.addDeposit` 에 이력 공백 + 초기금액 존재 시 "기존 납입분" 자동 생성 로직 추가
- [x] `resolveInitialDepositDate` private 헬퍼 추가 (CASH startDate 우선, fallback now)
- [ ] 수동 시나리오 확인 후 커밋

## 추가 발견: 납입 모달 총 납입 금액 미갱신 (2026-04-16)

### 현상
납입 추가/수정/삭제 성공 후 DB의 `investedAmount`는 갱신되지만, 납입 이력 모달 상단의 "총 납입 금액"은 이전 값 그대로 유지됨.

### 원인
`submitDeposit` / `submitEditDeposit` / `deleteDeposit` 는 `loadPortfolio()` 로 `portfolio.items` 배열을 새 인스턴스로 교체하지만, `portfolio.depositItem` 은 모달을 연 시점의 **이전 객체 레퍼런스**를 그대로 들고 있음. 모달 상단은 `portfolio.depositItem.investedAmount` 를 참조하므로 갱신되지 않음.

- `src/main/resources/static/js/components/portfolio.js:1261-1273` (submitDeposit)
- `src/main/resources/static/js/components/portfolio.js:1299-1312` (submitEditDeposit)
- `src/main/resources/static/js/components/portfolio.js:1319-1326` (deleteDeposit)
- `src/main/resources/static/index.html:2032` (총 납입 금액 바인딩)

### 해결 방안
`loadPortfolio()` 직후 `portfolio.depositItem` 을 `portfolio.items` 에서 `id` 기준으로 다시 찾아 재바인딩. 공통 헬퍼 `refreshDepositItem()` 를 추가하여 세 경로 모두에서 호출.

### 작업 리스트
- [x] `refreshDepositItem` 헬퍼 추가 (items에서 id로 조회하여 depositItem 재바인딩)
- [x] `submitDeposit`, `submitEditDeposit`, `deleteDeposit` 의 `loadPortfolio()` 직후 호출

## 후속 리팩토링 (2026-04-16)

- [x] `addDeposit`에서 `findByPortfolioItemId` 중복 호출 제거: 최초 조회 결과를 ArrayList로 보관하고 저장된 이력을 누적 후 `recalculateFromDepositHistories(histories)`에 직접 전달
- [x] 더 이상 호출되지 않는 `ensureInitialDepositHistory` 헬퍼 제거 (`resolveInitialDepositDate`는 유지)
- [x] `ecos.js::selectEcosCategory`에서 `_tooltipText`, `_rawTooltipText` 초기화 추가

## 알려진 제한사항

- **FUND 초기 이력의 `units=null`**: `addFundItem` 시점에는 좌수 정보가 없으므로 자동 생성된 "기존 납입분" 이력의 `units`는 `null`이다. 현재 `recalculateFromDepositHistories`는 `amount`만 합산하므로 정상 동작하지만, 향후 "총 보유 좌수 = 이력의 units 합산" 형태로 확장될 경우 초기 이력을 필터링하거나 가입 시점 좌수를 받아 채우는 별도 처리가 필요하다.
- **UI 상태 동시성**: `refreshDepositItem`은 `portfolio.items`에서 해당 id를 찾지 못하면 기존 `depositItem`을 유지한다(silent fallback). 동시에 항목이 삭제되는 드문 경우 모달이 stale 상태로 남을 수 있으나 현실 발생 가능성이 낮아 즉시 대응하지 않는다.
- **HTML 툴팁 카드 마크업 중복**: 파생/원시 지표 툴팁 카드의 SVG/스타일이 중복되나 Alpine 스코프 분리로 공통화 효용이 낮아 유지.