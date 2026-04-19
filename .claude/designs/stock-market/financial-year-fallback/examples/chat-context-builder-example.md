# ChatContextBuilder 수정 예시

## assembleFacts 메서드

```java
private String assembleFacts(String stockCode, AnalysisTask task) {
    StringBuilder sb = new StringBuilder();
    int effectiveYear = resolveEffectiveYear(stockCode);

    for (FinancialCategory category : task.categories()) {
        switch (category) {
            case ACCOUNT -> appendAccounts(sb, stockCode, effectiveYear);
            case PROFITABILITY -> appendIndicesAcrossYears(sb, "수익성 지표", stockCode, effectiveYear, "M210000");
            case STABILITY -> appendIndicesAcrossYears(sb, "���정성 지표", stockCode, effectiveYear, "M220000");
            case GROWTH -> appendIndicesAcrossYears(sb, "성장성 지표", stockCode, effectiveYear, "M230000");
            case ACTIVITY -> appendIndicesAcrossYears(sb, "활동성 지표", stockCode, effectiveYear, "M240000");
            case VALUATION -> appendValuation(sb, stockCode);
        }
    }
    return sb.toString();
}
```

## resolveEffectiveYear 메서드

```java
private int resolveEffectiveYear(String stockCode) {
    int currentYear = LocalDate.now().getYear();
    try {
        List<FinancialAccountResponse> accounts = stockFinancialService.getFinancialAccounts(
                stockCode, String.valueOf(currentYear), REPORT_CODE_ANNUAL);
        if (!accounts.isEmpty()) {
            return currentYear;
        }
    } catch (Exception e) {
        log.debug("당해연도 재무계정 조회 실패, 전년도 시도: stockCode={}, year={}", stockCode, currentYear);
    }
    return currentYear - 1;
}
```
