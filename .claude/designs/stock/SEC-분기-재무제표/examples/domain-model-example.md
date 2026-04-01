# SecFinancialItem 도메인 변경

## SecFinancialItem.java

```java
public record SecFinancialItem(
        String label,
        String labelEn,
        Map<String, Long> values   // 변경: Integer → String ("2024", "2024Q1" 등)
) {
}
```

## SecFinancialItemResponse.java

```java
@Getter
@RequiredArgsConstructor
public class SecFinancialItemResponse {
    private final String label;
    private final String labelEn;
    private final Map<String, Long> values;   // 변경: Integer → String

    public static SecFinancialItemResponse from(SecFinancialItem item) {
        return new SecFinancialItemResponse(
                item.label(),
                item.labelEn(),
                item.values()
        );
    }
}
```

## 기존 연간 코드 호환성 수정 (SecFinancialAdapter)

```java
// 변경 전: Map<Integer, Long> yearValues
// 변경 후: Map<String, Long> yearValues

private SecFinancialItem buildItem(ParsedCompanyFacts parsed,
                                   String label, String labelEn, String... tags) {
    Map<Integer, Double> values = getTagValues(parsed, tags);

    Map<String, Long> yearValues = new LinkedHashMap<>();
    for (Integer year : parsed.recentYears()) {
        Double val = values.get(year);
        yearValues.put(String.valueOf(year), val != null ? Math.round(val) : null);  // Integer → String
    }

    return new SecFinancialItem(label, labelEn, yearValues);
}
```
