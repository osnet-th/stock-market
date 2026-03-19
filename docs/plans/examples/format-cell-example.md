# formatFinancialCell + 요약 카드 포맷 변경 예시

## app.js - formatFinancialCell 수정

```javascript
// 변경 전
formatFinancialCell(value, type) {
    if (value == null || value === '') return '-';
    if (type === 'amount') {
        var num = parseFloat(String(value).replace(/,/g, ''));
        return isNaN(num) ? value : Format.number(num, 0);
    }
    if (type === 'number') {
        var n = parseFloat(String(value).replace(/,/g, ''));
        return isNaN(n) ? value : Format.number(n);
    }
    return value;
},

// 변경 후
formatFinancialCell(value, type) {
    if (value == null || value === '') return '-';
    if (type === 'amount') {
        return Format.compactNumber(value);
    }
    if (type === 'number') {
        var n = parseFloat(String(value).replace(/,/g, ''));
        return isNaN(n) ? value : Format.number(n);
    }
    return value;
}
```

## app.js - getFinancialSummaryCards 수정

요약 카드의 `card.value`도 compactNumber로 포맷팅:

```javascript
// 변경 전
cards.push({
    label: cfg.label,
    value: current,            // 원본 문자열 그대로 (예: "1,234,567,890")
    changeRate: changeRate
});

// 변경 후
cards.push({
    label: cfg.label,
    value: Format.compactNumber(current),  // 억/만 단위로 변환 (예: "12.3억")
    changeRate: changeRate
});
```

### 변경 포인트

- 테이블 셀: `formatFinancialCell()`에서 `Format.compactNumber()` 사용
- 요약 카드: `getFinancialSummaryCards()`에서 `card.value`에 `Format.compactNumber()` 적용
- `type === 'number'`는 변경 없음 (재무지표 값은 비율/지수이므로 원본 표시 유지)