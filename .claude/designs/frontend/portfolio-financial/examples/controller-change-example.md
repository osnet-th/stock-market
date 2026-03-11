# Controller 파라미터 enum 변경 예시

## 변경 전

```java
@GetMapping("/{stockCode}/financial/accounts")
public ResponseEntity<List<FinancialAccountResponse>> getFinancialAccounts(
        @PathVariable String stockCode,
        @RequestParam String year,
        @RequestParam String reportCode) {
    return ResponseEntity.ok(stockFinancialService.getFinancialAccounts(stockCode, year, reportCode));
}

@GetMapping("/{stockCode}/financial/indices")
public ResponseEntity<List<FinancialIndexResponse>> getFinancialIndices(
        @PathVariable String stockCode,
        @RequestParam String year,
        @RequestParam String reportCode,
        @RequestParam String indexClassCode) {
    return ResponseEntity.ok(stockFinancialService.getFinancialIndices(stockCode, year, reportCode, indexClassCode));
}
```

## 변경 후

```java
@GetMapping("/{stockCode}/financial/accounts")
public ResponseEntity<List<FinancialAccountResponse>> getFinancialAccounts(
        @PathVariable String stockCode,
        @RequestParam String year,
        @RequestParam ReportCode reportCode) {
    return ResponseEntity.ok(
            stockFinancialService.getFinancialAccounts(stockCode, year, reportCode.getCode()));
}

@GetMapping("/{stockCode}/financial/indices")
public ResponseEntity<List<FinancialIndexResponse>> getFinancialIndices(
        @PathVariable String stockCode,
        @RequestParam String year,
        @RequestParam ReportCode reportCode,
        @RequestParam IndexClassCode indexClassCode) {
    return ResponseEntity.ok(
            stockFinancialService.getFinancialIndices(
                    stockCode, year, reportCode.getCode(), indexClassCode.getCode()));
}
```

## 변경 범위

Controller에서 enum을 받아 `.getCode()`로 변환하여 Service에 전달.
**Service/Port/Adapter는 변경 없음** — 기존대로 String 코드값을 받음.

### 변경 대상 메서드 (reportCode 사용)

| 메서드 | reportCode | indexClassCode |
|---|---|---|
| `getFinancialAccounts` | String → ReportCode | - |
| `getFinancialIndices` | String → ReportCode | String → IndexClassCode |
| `getFullFinancialStatements` | String → ReportCode | - |
| `getStockQuantities` | String → ReportCode | - |
| `getDividendInfos` | String → ReportCode | - |
| `getPrivateFundUsages` | String → ReportCode | - |
| `getPublicFundUsages` | String → ReportCode | - |
| `getMultiFinancialAccounts` | String → ReportCode | - |
| `getMultiFinancialIndices` | String → ReportCode | String → IndexClassCode |

- `getLawsuits`는 reportCode를 사용하지 않으므로 변경 없음