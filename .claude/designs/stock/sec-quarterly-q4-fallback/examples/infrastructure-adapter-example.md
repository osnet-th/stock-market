# SecFinancialAdapter Q4 Fallback 구현 예시

## extractQuarterlyValues() 변경

```java
/**
 * 10-Q + 10-K(fp="Q4") 필터로 분기 데이터 추출, 키: "2024Q1" 형식
 * 1순위: 10-K의 fp="Q4" 데이터 직접 추출
 */
private Map<String, Double> extractQuarterlyValues(List<FactEntry> entries) {
    return entries.stream()
            .filter(e -> "10-Q".equals(e.getForm())
                    || ("10-K".equals(e.getForm()) && "Q4".equals(e.getFp())))
            .filter(e -> e.getFy() != null && e.getFp() != null && e.getVal() != null)
            .filter(e -> VALID_QUARTERS.contains(e.getFp()))
            .collect(Collectors.toMap(
                    e -> e.getFy().intValue() + e.getFp(),
                    FactEntry::getVal,
                    (existing, replacement) -> replacement
            ));
}
```

## parseCompanyFacts() 내 Q4 Fallback 호출

```java
private ParsedCompanyFacts parseCompanyFacts(SecCompanyFactsResponse response) {
    Map<String, TagData> usGaap = response.getUsGaapFacts();

    // 연간 데이터
    Map<String, Map<Integer, Double>> usdData = new HashMap<>();
    Map<String, Map<Integer, Double>> sharesData = new HashMap<>();
    Set<Integer> allYears = new TreeSet<>(Comparator.reverseOrder());

    // 분기 데이터
    Map<String, Map<String, Double>> quarterlyUsdData = new HashMap<>();
    Map<String, Map<String, Double>> quarterlySharesData = new HashMap<>();
    Set<String> allQuarters = new TreeSet<>(Comparator.reverseOrder());

    for (Map.Entry<String, TagData> entry : usGaap.entrySet()) {
        String tag = entry.getKey();
        TagData tagData = entry.getValue();

        // 연간 USD
        Map<Integer, Double> usdYearValues = extractAnnualValues(tagData.getUsdEntries());
        if (!usdYearValues.isEmpty()) {
            usdData.put(tag, usdYearValues);
            allYears.addAll(usdYearValues.keySet());
        }

        // 연간 USD/shares
        Map<Integer, Double> sharesYearValues = extractAnnualValues(tagData.getSharesEntries());
        if (!sharesYearValues.isEmpty()) {
            sharesData.put(tag, sharesYearValues);
            allYears.addAll(sharesYearValues.keySet());
        }

        // 분기 USD (10-Q + 10-K fp="Q4")
        Map<String, Double> qtrUsdValues = extractQuarterlyValues(tagData.getUsdEntries());
        // Q4 Fallback: 1순위에서 Q4를 못 찾은 연도에 대해 FY 기반 계산
        fillQ4Fallback(qtrUsdValues, usdYearValues, tagData.getUsdEntries());
        if (!qtrUsdValues.isEmpty()) {
            quarterlyUsdData.put(tag, qtrUsdValues);
            allQuarters.addAll(qtrUsdValues.keySet());
        }

        // 분기 USD/shares (10-Q + 10-K fp="Q4")
        Map<String, Double> qtrSharesValues = extractQuarterlyValues(tagData.getSharesEntries());
        fillQ4Fallback(qtrSharesValues, sharesYearValues, tagData.getSharesEntries());
        if (!qtrSharesValues.isEmpty()) {
            quarterlySharesData.put(tag, qtrSharesValues);
            allQuarters.addAll(qtrSharesValues.keySet());
        }
    }

    List<Integer> recentYears = allYears.stream().limit(3).toList();
    List<String> recentQuarters = allQuarters.stream().limit(8).toList();

    return new ParsedCompanyFacts(usdData, sharesData, recentYears,
            quarterlyUsdData, quarterlySharesData, recentQuarters);
}
```

## fillQ4Fallback() 신규 메서드

```java
/**
 * 2순위 Fallback: 연간(FY) 데이터로 Q4 계산.
 * - 시점 데이터 (대차대조표, start==null): Q4 = FY
 * - 기간 데이터 (손익/현금흐름, start!=null): Q4 = FY - Q1 - Q2 - Q3
 *
 * @param quarterlyValues 현재까지 추출된 분기 데이터 (mutable, Q4가 추가됨)
 * @param annualValues    연간(FY) 데이터
 * @param entries         원본 FactEntry 목록 (시점/기간 구분용)
 */
private void fillQ4Fallback(Map<String, Double> quarterlyValues,
                             Map<Integer, Double> annualValues,
                             List<FactEntry> entries) {
    if (annualValues == null || annualValues.isEmpty()) {
        return;
    }

    // 시점 데이터 여부 판별: 해당 태그의 10-K FY 엔트리에서 start 필드 확인
    boolean isPointInTime = entries.stream()
            .filter(e -> "10-K".equals(e.getForm()) && "FY".equals(e.getFp()))
            .anyMatch(e -> e.getStart() == null);

    for (Map.Entry<Integer, Double> annualEntry : annualValues.entrySet()) {
        int year = annualEntry.getKey();
        String q4Key = year + "Q4";

        // 이미 1순위(10-K fp="Q4")로 Q4가 있으면 스킵
        if (quarterlyValues.containsKey(q4Key)) {
            continue;
        }

        Double fyValue = annualEntry.getValue();
        if (fyValue == null) {
            continue;
        }

        if (isPointInTime) {
            // 대차대조표: FY 값 = Q4 값
            quarterlyValues.put(q4Key, fyValue);
        } else {
            // 손익/현금흐름: Q4 = FY - Q1 - Q2 - Q3
            Double q1 = quarterlyValues.get(year + "Q1");
            Double q2 = quarterlyValues.get(year + "Q2");
            Double q3 = quarterlyValues.get(year + "Q3");

            if (q1 != null && q2 != null && q3 != null) {
                quarterlyValues.put(q4Key, fyValue - q1 - q2 - q3);
            }
        }
    }
}
```

## 주요 포인트

- `extractQuarterlyValues()` 반환 타입을 mutable `HashMap`으로 유지해야 `fillQ4Fallback()`에서 Q4 추가 가능
  - 현재 `Collectors.toMap()`은 `HashMap`을 반환하므로 변경 불필요
- `isPointInTime` 판별: 동일 태그의 10-K FY 엔트리에서 `start == null`이면 시점 데이터
- Fallback은 `extractQuarterlyValues()` 직후, `quarterlyUsdData.put()` 직전에 호출
