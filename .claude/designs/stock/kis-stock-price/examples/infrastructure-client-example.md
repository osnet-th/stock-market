# KisStockPriceClient 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.KisProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOverseasPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisPriceApiResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * KIS 주식 현재가 조회 HTTP 클라이언트.
 * 국내/해외 API를 각각 호출하여 DTO를 반환한다.
 */
@Component
@RequiredArgsConstructor
public class KisStockPriceClient {

    private static final String DOMESTIC_PRICE_PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String OVERSEAS_PRICE_PATH = "/uapi/overseas-price/v1/quotations/price";
    private static final String DOMESTIC_TR_ID = "FHKST01010100";
    private static final String OVERSEAS_TR_ID = "HHDFS00000300";

    private final RestClient restClient;
    private final KisProperties properties;
    private final KisTokenManager tokenManager;

    /**
     * 국내 주식/ETF 현재가 조회.
     *
     * @param stockCode 종목코드 6자리 (예: 005930)
     */
    public KisDomesticPriceOutput getDomesticPrice(String stockCode) {
        try {
            KisPriceApiResponse<KisDomesticPriceOutput> response = restClient.get()
                .uri(properties.getBaseUrl() + DOMESTIC_PRICE_PATH,
                    uriBuilder -> uriBuilder
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .build())
                .headers(headers -> setKisHeaders(headers, DOMESTIC_TR_ID))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            validateResponse(response, stockCode);
            return response.getOutput();

        } catch (RestClientException e) {
            throw new KisApiException("국내 현재가 조회 실패 [" + stockCode + "]: " + e.getMessage(), e);
        }
    }

    /**
     * 해외 주식/ETF 현재가 조회.
     *
     * @param stockCode    종목코드 (예: AAPL)
     * @param exchangeCode 거래소코드
     */
    public KisOverseasPriceOutput getOverseasPrice(String stockCode, ExchangeCode exchangeCode) {
        try {
            KisPriceApiResponse<KisOverseasPriceOutput> response = restClient.get()
                .uri(properties.getBaseUrl() + OVERSEAS_PRICE_PATH,
                    uriBuilder -> uriBuilder
                        .queryParam("AUTH", "")
                        .queryParam("EXCD", exchangeCode.name())
                        .queryParam("SYMB", stockCode)
                        .build())
                .headers(headers -> setKisHeaders(headers, OVERSEAS_TR_ID))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            validateResponse(response, stockCode);
            return response.getOutput();

        } catch (RestClientException e) {
            throw new KisApiException("해외 현재가 조회 실패 [" + exchangeCode.name() + ":" + stockCode + "]: " + e.getMessage(), e);
        }
    }

    private void setKisHeaders(org.springframework.http.HttpHeaders headers, String trId) {
        headers.setBearerAuth(tokenManager.getAccessToken());
        headers.set("appkey", properties.getAppKey());
        headers.set("appsecret", properties.getAppSecret());
        headers.set("tr_id", trId);
    }

    private void validateResponse(KisPriceApiResponse<?> response, String stockCode) {
        if (response == null) {
            throw new KisApiException("현재가 조회 응답이 null [" + stockCode + "]");
        }
        if (!response.isSuccess()) {
            throw new KisApiException("현재가 조회 실패 [" + stockCode + "]: " + response.getMessage());
        }
    }
}
```

## 핵심 포인트

- `RestClient.get()` + `ParameterizedTypeReference`로 제네릭 응답 역직렬화
- KIS 공통 헤더 (Bearer token, appkey, appsecret, tr_id)를 `setKisHeaders`로 통합
- API 응답의 `rt_cd`가 `"0"`이 아니면 `KisApiException` throw
- URI 빌더를 사용하여 쿼리 파라미터를 안전하게 조합