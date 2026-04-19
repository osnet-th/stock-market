# ValuationMetricService 수정 예시

## calculate 메서드

```java
public ValuationMetricResponse calculate(String stockCode) {
    List<String> warnings = new ArrayList<>();
    String year = resolveEffectiveYear(stockCode, warnings);

    List<FinancialAccountResponse> accounts = safeGetAccounts(stockCode, year, warnings);
    // ... 이하 기존 로직 동일
}
```

## resolveEffectiveYear 메서드

```java
private String resolveEffectiveYear(String stockCode, List<String> warnings) {
    int currentYear = LocalDate.now().getYear();
    try {
        List<FinancialAccountResponse> accounts = stockFinancialService.getFinancialAccounts(
                stockCode, String.valueOf(currentYear), REPORT_CODE_ANNUAL);
        if (!accounts.isEmpty()) {
            return String.valueOf(currentYear);
        }
    } catch (Exception e) {
        log.debug("당��연도 재무계정 조회 실패, 전년도 시도: stockCode={}, year={}", stockCode, currentYear);
    }
    return String.valueOf(currentYear - 1);
}
```