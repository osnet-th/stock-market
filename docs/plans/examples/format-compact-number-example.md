# Format.compactNumber 소수점 개선 예시

## format.js - compactNumber 수정

```javascript
// 변경 전
compactNumber(value) {
    // ...
    if (absNum >= 1_0000_0000) {
        return sign + (absNum / 1_0000_0000).toFixed(0) + '억';  // 12억 (소수점 없음)
    }
    // ...
}

// 변경 후
compactNumber(value) {
    // ...
    if (absNum >= 1_0000_0000) {
        return sign + (absNum / 1_0000_0000).toFixed(1) + '억';  // 12.3억 (소수점 1자리)
    }
    // ...
}
```

### 변경 포인트

- 억 단위에서 `toFixed(0)` → `toFixed(1)` 로 변경
- 결과: `1,234,567,890` → `12.3억` (브레인스토밍 결정 사항 반영)
- 조 단위는 이미 `toFixed(1)` 적용되어 있으므로 변경 불필요
- 만 단위도 `toFixed(0)` 유지 (만 단위에서 소수점은 불필요)