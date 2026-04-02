---
title: "fix: 주식 수정 모달에서 연결된 계좌가 표시되지 않는 문제"
type: fix
status: active
date: 2026-04-02
---

# fix: 주식 수정 모달에서 연결된 계좌가 표시되지 않는 문제

GitHub Issue: #22

## 문제

포트폴리오에서 주식 수정 시 계좌 연결(연결 안함 → 계좌 선택) 후 저장하고 다시 수정 모달을 열면 "연결 안 함"으로 표시됨. DB 저장은 정상(추가매수 시 잔액 차감 확인됨).

## 원인

1. **Alpine.js 타입 불일치**: `linkedCashItemId`는 JSON에서 number(`5`)로 전달되나, `<select>` 요소의 value는 항상 string. Alpine.js의 `x-model` 비교에서 타입 불일치 발생 가능
2. **`x-if` 중첩 렌더링 타이밍**: 수정 모달은 `x-if`가 3단계 중첩(`editingItem` → `assetType === 'STOCK'` → `x-for` 옵션). `x-model` 초기화 시점에 동적 옵션이 아직 렌더링되지 않을 수 있음
3. **모달 외부 클릭 시 상태 미정리**: `@click.outside`로 닫을 때 `editingItem = null`이 누락되어 다음 모달 오픈 시 stale 데이터 잔존

## Acceptance Criteria

- [ ] 주식 수정에서 계좌 연결 후 저장 → 재편집 시 연결된 계좌가 정상 표시
- [ ] 계좌 연결 해제 후 저장 → 재편집 시 "연결 안 함" 표시
- [ ] 동일 계좌 유지 후 저장 → 재편집 시 동일 계좌 표시
- [ ] 추가매수 시 연결 계좌 잔액 차감 기존 동작 유지
- [ ] 모달 외부 클릭으로 닫은 후 다시 편집해도 정상 동작

## 작업 리스트

- [x] 프론트엔드: select 옵션 `:value` 타입 통일 (`index.html`)
- [x] 프론트엔드: `openEditModal()` cashItemId 타입 변환 + `$nextTick` 적용 (`portfolio.js`)
- [x] 프론트엔드: 모달 외부 클릭 시 `editingItem = null` 추가 (`index.html`)
- [x] 백엔드: `updateStockItem()` 응답에 `linkedCashItemId` 포함 (`PortfolioService.java`)

## 구현

### `index.html` — select 옵션 타입 통일

**수정 위치**: line 1460 (수정 모달), line 1203 (등록 모달)

두 곳 모두 `:value="cash.id"` → `:value="String(cash.id)"` 변경.
이를 통해 `x-model` 값(string)과 option value(string) 타입을 일치시킴.

### `index.html` — 모달 외부 클릭 핸들러

**수정 위치**: line 1391

`@click.outside="portfolio.showEditModal = false"` →
`@click.outside="portfolio.showEditModal = false; portfolio.editingItem = null"` 변경.

### `portfolio.js` — `openEditModal()` 타입 변환 + `$nextTick`

**수정 위치**: line 901, 935-936

```javascript
// line 901: number → string 변환 (null-safe)
form.cashItemId = item.linkedCashItemId != null ? String(item.linkedCashItemId) : '';

// line 935-936: $nextTick으로 select 렌더링 후 값 재설정
this.portfolio.editForm = form;
this.portfolio.showEditModal = true;
this.$nextTick(() => {
    if (item.linkedCashItemId != null) {
        this.portfolio.editForm.cashItemId = String(item.linkedCashItemId);
    }
});
```

`$nextTick`을 사용하여 중첩 `x-if` + `x-for` DOM 렌더링 완료 후 값을 재설정.

### `PortfolioService.java` — 응답 일관성 보완

**수정 위치**: `updateStockItem()` line 291

```java
// 변경 전
return PortfolioItemResponse.from(saved);

// 변경 후
Long linkedCashItemId = cashStockLinkRepository.findByStockItemId(itemId)
    .map(CashStockLink::getCashItemId)
    .orElse(null);
return PortfolioItemResponse.from(saved, linkedCashItemId);
```

`@Transactional` 내에서 JPA flush mode AUTO이므로 직전 save 결과 조회 가능.

## 주의사항

- `submitEditItem()`의 `Number(form.cashItemId)` 변환 (line 993)은 string → number 변환이므로 기존 동작 유지됨
- `$nextTick` 한 번으로 중첩 `x-if` 렌더링이 완료되지 않을 경우, 이중 `$nextTick` 또는 `requestAnimationFrame` fallback 검토
- `deductOnLink` confirm 취소 시 "연결만" 진행되는 UX는 이번 범위 밖 (별도 이슈)
- 삭제된 현금 자산에 대한 `linkedCashItemId` 처리는 이번 범위 밖 (별도 이슈)

## 검증 방법

1. 주식 수정 → 계좌 연결(연결 안함 → 계좌 선택) → 저장 → 재편집 → 연결 계좌 확인
2. 주식 수정 → 계좌 해제(계좌 → 연결 안함) → 저장 → 재편집 → "연결 안 함" 확인
3. 추가매수 → 연결 계좌 잔액 차감 정상 확인
4. 모달 외부 클릭 닫기 → 재편집 → 정상 표시 확인
5. 브라우저 개발자 도구에서 `GET /api/portfolio/items` 응답의 `linkedCashItemId` 값 확인

## Sources

- 분석 문서: [.claude/analyzes/portfolio/cash-link-display-bug/cash-link-display-bug.md](../../.claude/analyzes/portfolio/cash-link-display-bug/cash-link-display-bug.md)
- Related issue: #22