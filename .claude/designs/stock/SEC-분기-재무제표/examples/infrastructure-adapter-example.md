# SecFinancialAdapter 분기 추출

## ParsedCompanyFacts 확장

```java
record ParsedCompanyFacts(
        Map<String, Map<Integer, Double>> usdData,       // 연간: 태그 → {연도 → 값}
        Map<String, Map<Integer, Double>> sharesData,     // 연간: 주당 데이터
        List<Integer> recentYears,                        // 최근 3개 연도
        Map<String, Map<String, Double>> quarterlyUsdData,    // 분기: 태그 → {"2024Q1" → 값}
        Map<String, Map<String, Double>> quarterlySharesData, // 분기: 주당 데이터
        List<String> recentQuarters                       // 최근 8분기 ["2024Q3", "2024Q2", ...]
) {}
```

## extractQuarterlyValues()

```java
/**
 * 10-Q 필터로 분기 데이터 추출.
 * 키: "2024Q1" 형식, 중복 분기는 최신 filed 기준
 */
private Map<String, Double> extractQuarterlyValues(List<FactEntry> entries) {
    return entries.stream()
            .filter(e -> "10-Q".equals(e.getForm()))
            .filter(e -> e.getFy() != null && e.getFp() != null && e.getVal() != null)
            .filter(e -> Set.of("Q1", "Q2", "Q3", "Q4").contains(e.getFp()))
            .collect(Collectors.toMap(
                    e -> e.getFy().intValue() + e.getFp(),   // "2024Q1"
                    FactEntry::getVal,
                    (existing, replacement) -> replacement
            ));
}
```

## parseCompanyFacts() 확장

```java
private ParsedCompanyFacts parseCompanyFacts(SecCompanyFactsResponse response) {
    Map<String, TagData> usGaap = response.getUsGaapFacts();

    // 연간 데이터 (기존)
    Map<String, Map<Integer, Double>> usdData = new HashMap<>();
    Map<String, Map<Integer, Double>> sharesData = new HashMap<>();
    Set<Integer> allYears = new TreeSet<>(Comparator.reverseOrder());

    // 분기 데이터 (추가)
    Map<String, Map<String, Double>> quarterlyUsdData = new HashMap<>();
    Map<String, Map<String, Double>> quarterlySharesData = new HashMap<>();
    Set<String> allQuarters = new TreeSet<>(Comparator.reverseOrder());

    for (Map.Entry<String, TagData> entry : usGaap.entrySet()) {
        String tag = entry.getKey();
        TagData tagData = entry.getValue();

        // 연간 (기존 로직)
        Map<Integer, Double> usdYearValues = extractAnnualValues(tagData.getUsdEntries());
        if (!usdYearValues.isEmpty()) {
            usdData.put(tag, usdYearValues);
            allYears.addAll(usdYearValues.keySet());
        }
        Map<Integer, Double> sharesYearValues = extractAnnualValues(tagData.getSharesEntries());
        if (!sharesYearValues.isEmpty()) {
            sharesData.put(tag, sharesYearValues);
            allYears.addAll(sharesYearValues.keySet());
        }

        // 분기 (추가)
        Map<String, Double> qtrUsdValues = extractQuarterlyValues(tagData.getUsdEntries());
        if (!qtrUsdValues.isEmpty()) {
            quarterlyUsdData.put(tag, qtrUsdValues);
            allQuarters.addAll(qtrUsdValues.keySet());
        }
        Map<String, Double> qtrSharesValues = extractQuarterlyValues(tagData.getSharesEntries());
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

## 분기 재무제표 구성

```java
public List<SecFinancialStatement> getQuarterlyFinancialStatements(String ticker) {
    ParsedCompanyFacts parsed = getParsedFacts(ticker);

    return List.of(
            buildQuarterlyIncomeStatement(parsed),
            buildQuarterlyBalanceSheet(parsed),
            buildQuarterlyCashFlowStatement(parsed)
    );
}

private SecFinancialItem buildQuarterlyItem(ParsedCompanyFacts parsed,
                                             String label, String labelEn, String... tags) {
    Map<String, Double> values = getQuarterlyTagValues(parsed, tags);

    Map<String, Long> quarterValues = new LinkedHashMap<>();
    for (String quarter : parsed.recentQuarters()) {
        Double val = values.get(quarter);
        quarterValues.put(quarter, val != null ? Math.round(val) : null);
    }

    return new SecFinancialItem(label, labelEn, quarterValues);
}

private Map<String, Double> getQuarterlyTagValues(ParsedCompanyFacts parsed, String... tags) {
    for (String tag : tags) {
        Map<String, Double> values = parsed.quarterlyUsdData().get(tag);
        if (values != null && !values.isEmpty()) {
            return values;
        }
    }
    return Map.of();
}
```
