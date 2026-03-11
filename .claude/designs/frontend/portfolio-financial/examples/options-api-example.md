# 옵션 조회 API 예시

## FinancialOptionsResponse

위치: `stock/application/dto/FinancialOptionsResponse.java`

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.IndexClassCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.ReportCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public class FinancialOptionsResponse {

    private final List<CodeOption> reportCodes;
    private final List<CodeOption> indexClassCodes;

    private FinancialOptionsResponse(List<CodeOption> reportCodes, List<CodeOption> indexClassCodes) {
        this.reportCodes = reportCodes;
        this.indexClassCodes = indexClassCodes;
    }

    public static FinancialOptionsResponse create() {
        List<CodeOption> reports = Arrays.stream(ReportCode.values())
                .map(r -> new CodeOption(r.name(), r.getCode(), r.getLabel()))
                .toList();

        List<CodeOption> indices = Arrays.stream(IndexClassCode.values())
                .map(i -> new CodeOption(i.name(), i.getCode(), i.getLabel()))
                .toList();

        return new FinancialOptionsResponse(reports, indices);
    }

    @Getter
    public static class CodeOption {
        private final String code;
        private final String dartCode;
        private final String label;

        CodeOption(String code, String dartCode, String label) {
            this.code = code;
            this.dartCode = dartCode;
            this.label = label;
        }
    }
}
```

## StockFinancialController 추가

```java
@GetMapping("/financial/options")
public ResponseEntity<FinancialOptionsResponse> getFinancialOptions() {
    return ResponseEntity.ok(FinancialOptionsResponse.create());
}
```