# 인프라 어댑터 예시

## EcosIndicatorAdapter 변경

API 응답에서 KeyStatIndicator 생성 시 `previousDataValue = null`:

```java
// 기존 Row → KeyStatIndicator 변환 부분에서
// previousDataValue를 null로 생성
new KeyStatIndicator(
    row.className(),
    row.keystatName(),
    row.dataValue(),
    null,  // previousDataValue — API에서 미제공
    row.cycle(),
    row.unitName()
)
```