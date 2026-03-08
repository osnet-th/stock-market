# Infrastructure 계층 코드 예시

## DataGoKrProperties (설정)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "stock.api.datagokr")
public class DataGoKrProperties {
    private String baseUrl;
    private String serviceKey;
    private int numOfRows = 100;
}
```

## DataGoKrStockItem (응답 항목 DTO)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataGoKrStockItem {
    private String basDt;     // 기준일자
    private String srtnCd;    // 단축코드 (종목코드 6자리)
    private String isinCd;    // ISIN코드
    private String mrktCtg;   // 시장구분
    private String itmsNm;    // 종목명
    private String crno;      // 법인등록번호
    private String corpNm;    // 법인명
}
```

## DataGoKrStockResponse (응답 DTO)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class DataGoKrStockResponse {

    private Response response;

    @Getter
    @Setter
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @Setter
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @Setter
    public static class Body {
        private int numOfRows;
        private int pageNo;
        private int totalCount;
        private Items items;
    }

    @Getter
    @Setter
    public static class Items {
        private List<DataGoKrStockItem> item;
    }

    public List<DataGoKrStockItem> getItemList() {
        if (response == null || response.getBody() == null
                || response.getBody().getItems() == null
                || response.getBody().getItems().getItem() == null) {
            return Collections.emptyList();
        }
        return response.getBody().getItems().getItem();
    }

    public boolean isSuccess() {
        return response != null
            && response.getHeader() != null
            && "00".equals(response.getHeader().getResultCode());
    }
}
```

## DataGoKrApiException (예외)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception;

public class DataGoKrApiException extends RuntimeException {
    public DataGoKrApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataGoKrApiException(String message) {
        super(message);
    }
}
```

## DataGoKrStockApiClient (HTTP 클라이언트)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.config.DataGoKrProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.dto.DataGoKrStockResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception.DataGoKrApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class DataGoKrStockApiClient {

    private final RestClient restClient;
    private final DataGoKrProperties properties;

    public DataGoKrStockResponse searchByName(String stockName) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl())
                .queryParam("serviceKey", properties.getServiceKey())
                .queryParam("resultType", "json")
                .queryParam("numOfRows", properties.getNumOfRows())
                .queryParam("pageNo", 1)
                .queryParam("likeItmsNm", stockName)
                .build(true)
                .toUri();

            return restClient.get()
                .uri(uri)
                .retrieve()
                .body(DataGoKrStockResponse.class);
        } catch (RestClientException e) {
            throw new DataGoKrApiException("공공데이터포털 상장종목 API 호출 실패: " + e.getMessage(), e);
        }
    }
}
```

## DataGoKrListedStockAdapter (어댑터 - Port 구현체)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr;

import com.thlee.stock.market.stockmarket.stock.domain.model.ListedStock;
import com.thlee.stock.market.stockmarket.stock.domain.service.ListedStockPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.dto.DataGoKrStockResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception.DataGoKrApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataGoKrListedStockAdapter implements ListedStockPort {

    private final DataGoKrStockApiClient apiClient;

    @Override
    public List<ListedStock> searchByName(String stockName) {
        DataGoKrStockResponse response = apiClient.searchByName(stockName);

        if (!response.isSuccess()) {
            throw new DataGoKrApiException("공공데이터포털 응답 오류: "
                + response.getResponse().getHeader().getResultMsg());
        }

        return response.getItemList().stream()
            .map(item -> new ListedStock(
                item.getSrtnCd(),
                item.getItmsNm(),
                item.getMrktCtg(),
                item.getCorpNm()
            ))
            .toList();
    }
}
```

## 프로퍼티 설정

```yaml
# application.yml
stock:
  api:
    datagokr:
      base-url: https://apis.data.go.kr/1160100/service/GetKrxListedInfoService/getItemInfo
      service-key: ${DATAGOKR_SERVICE_KEY}
      num-of-rows: 100
```