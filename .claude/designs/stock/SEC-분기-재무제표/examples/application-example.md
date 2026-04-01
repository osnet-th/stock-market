# Controller + Service 확장

## SecFinancialPort.java

```java
public interface SecFinancialPort {
    List<SecFinancialStatement> getFinancialStatements(String ticker);
    List<SecFinancialStatement> getQuarterlyFinancialStatements(String ticker);  // 추가
    List<SecInvestmentMetric> getInvestmentMetrics(String ticker);
}
```

## SecFinancialService.java

```java
public List<SecFinancialStatementResponse> getQuarterlyFinancialStatements(String ticker) {
    return secFinancialPort.getQuarterlyFinancialStatements(ticker).stream()
            .map(SecFinancialStatementResponse::from)
            .toList();
}
```

## SecFinancialController.java

```java
@GetMapping("/{ticker}/sec/financial/statements/quarterly")
public ResponseEntity<List<SecFinancialStatementResponse>> getQuarterlyFinancialStatements(
        @PathVariable String ticker) {
    return ResponseEntity.ok(secFinancialService.getQuarterlyFinancialStatements(ticker));
}
```
