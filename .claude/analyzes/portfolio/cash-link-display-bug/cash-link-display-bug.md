# 계좌 연결 수정 후 화면 미반영 버그 분석

GitHub Issue: #22

## 현재 상태

주식 수정 시 계좌 연결(연결 안함 → 계좌 선택) 후 저장 → 다시 수정 화면 진입 시 "연결 안 함"으로 표시됨.
실제 추가매수 시에는 연결된 계좌에서 잔액이 차감되므로 **DB 저장은 정상, 화면만 미반영**.

## 데이터 흐름 분석

### 저장 흐름 (정상)

1. 프론트: `submitEditItem()` → `API.updateStockItem()` 호출
2. 백엔드: `PortfolioService.updateStockItem()` (`:291`) → `handleCashLinkChange()` (`:633-645`)
3. `CashStockLink.create()` → `cashStockLinkRepository.save()` → **DB에 정상 저장**
4. `@Transactional` 커밋 → 응답 반환

### 응답 흐름 (문제 지점 1)

`PortfolioService.java:291`:
```java
return PortfolioItemResponse.from(saved);  // linkedCashItemId = null
```

`PortfolioItemResponse.from(PortfolioItem)` (`:56-58`):
```java
public static PortfolioItemResponse from(PortfolioItem item) {
    return from(item, null);  // linkedCashItemId 항상 null
}
```

**저장 응답에 `linkedCashItemId`가 누락됨.** 단, 프론트에서 이 응답 값을 직접 사용하지 않으므로 이것만으로 버그 원인이 되지는 않음.

### 재조회 흐름 (정상으로 보임)

1. 프론트: `submitEditItem()` 완료 후 `loadPortfolio()` 호출 (`portfolio.js:1061`)
2. `API.getPortfolioItems()` → `PortfolioService.getItems()` (`:238-257`)
3. `getItems()`는 `CashStockLink` 배치 조회 후 `linkedCashItemId`를 **정상 포함**하여 반환
4. `this.portfolio.items = results[0]` → 정상 데이터로 갱신

### 편집 모달 진입 (문제 지점 2)

`openEditModal()` (`portfolio.js:876-936`):
```javascript
form.cashItemId = item.linkedCashItemId || '';  // :901
this.portfolio.editForm = form;                  // :935
this.portfolio.showEditModal = true;              // :936
```

HTML 셀렉트 (`index.html:1456-1462`):
```html
<select x-model="portfolio.editForm.cashItemId">
    <option value="">연결 안 함</option>
    <template x-for="cash in getCashItems()" :key="cash.id">
        <option :value="cash.id" ...></option>
    </template>
</select>
```

## 문제점

### 원인 후보 1: Alpine.js `x-model` + `x-if` + `x-for` 렌더링 타이밍

편집 모달은 `<template x-if="portfolio.editingItem">`로 감싸져 있음 (`index.html:1402`).

`openEditModal()` 호출 시:
1. `editingItem` 설정 → `x-if` 활성화 → 템플릿 DOM 삽입
2. `editForm` 설정 → `x-model` 바인딩
3. `showEditModal = true` → 모달 표시

Alpine.js v3의 `x-if` 템플릿 생성 시, **`x-for` 옵션 렌더링보다 `x-model` 초기화가 먼저 실행될 가능성**이 있음:
- `x-model`이 `select.value = "5"` 설정 시점에 동적 옵션이 아직 없으면
- `<option value="">` (연결 안 함)만 존재하여 기본 선택됨
- 이후 `x-for`가 옵션을 렌더링해도 `x-model` 값은 이미 `""` (빈 문자열)로 변경됨

### 원인 후보 2: 타입 불일치 (number vs string)

- `item.linkedCashItemId`: JSON에서 `number` (예: `5`)
- `<option :value="cash.id">`: Alpine이 number 그대로 바인딩
- `<option value="">`: 문자열 `""`

HTML `<select>` 요소의 `value`는 항상 **문자열**로 처리됨. Alpine.js의 `x-model`이 number `5`를 설정할 때, 내부적으로 `select.value = "5"` (문자열 변환)이 되어야 하지만, `:value` 바인딩된 옵션과의 비교에서 타입 불일치가 발생할 수 있음.

## 영향 범위

- **프론트**: `portfolio.js` (openEditModal), `index.html` (수정 모달 셀렉트)
- **백엔드**: `PortfolioService.updateStockItem()` 응답 (직접 원인은 아니나 일관성 문제)

## 해결 방안

### 방안 1: 프론트엔드 타입 통일 + `$nextTick` (권장)

`portfolio.js:901` 수정:
```javascript
// number → string 변환으로 select 타입 일치
form.cashItemId = item.linkedCashItemId ? String(item.linkedCashItemId) : '';
```

모달 오픈 시 `$nextTick`으로 값 재설정:
```javascript
this.portfolio.editForm = form;
this.portfolio.showEditModal = true;
this.$nextTick(() => {
    this.portfolio.editForm.cashItemId = item.linkedCashItemId 
        ? String(item.linkedCashItemId) : '';
});
```

`submitEditItem()`의 `newCashItemId` 변환은 이미 `Number(form.cashItemId)`로 처리하므로 변경 불필요.

### 방안 2: 백엔드 응답 일관성 보완 (추가 권장)

`PortfolioService.updateStockItem()` (`:291`) 수정:
```java
Long linkedCashItemId = cashStockLinkRepository.findByStockItemId(itemId)
    .map(CashStockLink::getCashItemId)
    .orElse(null);
return PortfolioItemResponse.from(saved, linkedCashItemId);
```

이 수정은 직접적 버그 원인 해결은 아니지만, 응답 일관성을 보장하고 향후 응답 값을 직접 활용하는 경우에 대비.

## 검증 방안

1. 브라우저 개발자 도구로 `GET /api/portfolio/items` 응답에서 해당 주식의 `linkedCashItemId` 값 확인
2. `openEditModal()` 내부에서 `console.log(item.linkedCashItemId, typeof item.linkedCashItemId)` 확인
3. `editForm.cashItemId` 설정 후 `$nextTick`에서 `select.value` 확인

## 권장 사항

**방안 1 + 방안 2 동시 적용** 권장:
- 방안 1: 프론트엔드 셀렉트 타입 일관성 확보 (직접 수정)
- 방안 2: 백엔드 응답 일관성 보완 (간접 수정)