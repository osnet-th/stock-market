# Presentation 계층 코드 예시

## ListedStockController

```java
package com.thlee.stock.market.stockmarket.stock.presentation;

import com.thlee.stock.market.stockmarket.stock.application.ListedStockSearchService;
import com.thlee.stock.market.stockmarket.stock.application.dto.ListedStockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class ListedStockController {

    private final ListedStockSearchService listedStockSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<ListedStockResponse>> searchStocks(
            @RequestParam String name) {
        List<ListedStockResponse> results = listedStockSearchService.searchStocks(name);
        return ResponseEntity.ok(results);
    }
}
```

## 요청/응답 예시

**Request**
```
GET /api/stocks/search?name=삼성
Authorization: Bearer {JWT_TOKEN}
```

**Response (200 OK)**
```json
[
  {
    "stockCode": "005930",
    "stockName": "삼성전자",
    "marketType": "KOSPI",
    "corpName": "삼성전자주식회사"
  },
  {
    "stockCode": "006400",
    "stockName": "삼성SDI",
    "marketType": "KOSPI",
    "corpName": "삼성에스디아이주식회사"
  },
  {
    "stockCode": "028260",
    "stockName": "삼성물산",
    "marketType": "KOSPI",
    "corpName": "삼성물산주식회사"
  }
]
```