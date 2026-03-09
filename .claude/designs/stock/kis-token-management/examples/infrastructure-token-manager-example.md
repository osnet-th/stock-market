# KisTokenManager 구현 예시

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.KisProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisTokenResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class KisTokenManager {

    private static final String TOKEN_CACHE_KEY = "kis_access_token";
    private static final String TOKEN_ENDPOINT = "/oauth2/tokenP";

    private final RestClient restClient;
    private final KisProperties properties;
    private final Cache<String, String> tokenCache;

    public KisTokenManager(RestClient restClient, KisProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
        this.tokenCache = Caffeine.newBuilder()
            .expireAfterWrite(23, TimeUnit.HOURS)
            .maximumSize(1)
            .build();
    }

    /**
     * KIS Access Token 반환.
     * 캐시에 토큰이 있으면 즉시 반환, 없으면 발급 후 캐시에 저장.
     */
    public String getAccessToken() {
        String token = tokenCache.getIfPresent(TOKEN_CACHE_KEY);
        if (token != null) {
            return token;
        }
        return issueAndCacheToken();
    }

    private String issueAndCacheToken() {
        try {
            Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", properties.getAppKey(),
                "appsecret", properties.getAppSecret()
            );

            KisTokenResponse response = restClient.post()
                .uri(properties.getBaseUrl() + TOKEN_ENDPOINT)
                .body(body)
                .retrieve()
                .body(KisTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new KisApiException("KIS 토큰 발급 응답이 비어있습니다");
            }

            String accessToken = response.getAccessToken();
            tokenCache.put(TOKEN_CACHE_KEY, accessToken);
            log.info("KIS Access Token 발급 완료");

            return accessToken;
        } catch (RestClientException e) {
            throw new KisApiException("KIS Access Token 발급 실패: " + e.getMessage(), e);
        }
    }
}
```

## 사용 예시 (향후 KIS API 클라이언트에서)

```java
@Component
@RequiredArgsConstructor
public class KisStockApiClient {

    private final RestClient restClient;
    private final KisProperties properties;
    private final KisTokenManager tokenManager;

    public KisStockPriceResponse getPrice(String stockCode) {
        return restClient.get()
            .uri(properties.getBaseUrl() + "/uapi/domestic-stock/v1/quotations/inquire-price"
                + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode)
            .header("authorization", "Bearer " + tokenManager.getAccessToken())
            .header("appkey", properties.getAppKey())
            .header("appsecret", properties.getAppSecret())
            .header("tr_id", "FHKST01010100")
            .retrieve()
            .body(KisStockPriceResponse.class);
    }
}
```