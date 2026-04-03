# 포트폴리오 주식 카드 - 연결 계좌 정보 표시

**날짜**: 2026-04-03  
**상태**: 브레인스톰 완료

---

## 무엇을 만드는가

포트폴리오 주식 카드에서 연결된 현금 자산 정보를 바로 확인할 수 있도록 표시.
현재는 수정 모달을 열어야만 연결 여부를 알 수 있어 불편함.

---

## 핵심 결정

### 표시 위치
- 종목 요약(getItemSummary) 줄 아래, 주가 정보 위에 배치

### 표시 형태
- **연결된 경우**: `{계좌명} 연결` (예: "토스CMA 연결")
- **미연결인 경우**: `계좌 미연결` (회색 텍스트)
- 주식(STOCK) 자산 타입에서만 표시

### 카드 레이아웃 예시

```
삼성전자                원금 5,000,000원  5,230,000원
10주 | 평균 50,000원 | 배당 2.5%
토스CMA 연결                    ← 연결된 경우
(또는) 계좌 미연결               ← 미연결인 경우
현재가 52,300원 (+4.6%)
```

---

## 변경 범위

| 파일 | 변경 내용 |
|------|-----------|
| `index.html` (~906행 부근) | getItemSummary 아래에 연결 계좌 표시 줄 추가 |
| `portfolio.js` | `getLinkedCashName(item)` 헬퍼 함수 추가 (linkedCashItemId로 계좌명 조회) |

### 구현 방향

**portfolio.js** - 헬퍼 함수:
```js
getLinkedCashName(item) {
    if (!item.linkedCashItemId) return null;
    const cash = this.portfolio.items.find(i => i.id === item.linkedCashItemId);
    return cash ? cash.itemName : null;
}
```

**index.html** - 카드에 연결 계좌 줄 추가 (getItemSummary 아래):
```html
<p x-show="item.assetType === 'STOCK'" class="text-xs mt-0.5"
   :class="item.linkedCashItemId ? 'text-amber-600' : 'text-gray-300'"
   x-text="getLinkedCashName(item) ? getLinkedCashName(item) + ' 연결' : '계좌 미연결'">
</p>
```