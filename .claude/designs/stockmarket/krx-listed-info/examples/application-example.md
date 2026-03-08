# Application 계층 코드 예시

## ListedStockResponse (응답 DTO)

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.ListedStock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ListedStockResponse {
    private final String stockCode;
    private final String stockName;
    private final String marketType;
    private final String corpName;

    public static ListedStockResponse from(ListedStock stock) {
        return new ListedStockResponse(
            stock.stockCode(),
            stock.stockName(),
            stock.marketType(),
            stock.corpName()
        );
    }
}
```

## ListedStockSearchService (유스케이스)

```java
package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.ListedStockResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ListedStock;
import com.thlee.stock.market.stockmarket.stock.domain.service.ListedStockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListedStockSearchService {

    private final ListedStockPort listedStockPort;

    public List<ListedStockResponse> searchStocks(String stockName) {
        List<ListedStock> stocks = listedStockPort.searchByName(stockName);

        return stocks.stream()
            .map(ListedStockResponse::from)
            .toList();
    }
}
```