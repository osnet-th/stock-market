package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.KisProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisTokenResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
public class KisTokenManager {

    private static final String TOKEN_ENDPOINT = "/oauth2/tokenP";

    private final RestClient restClient;
    private final KisProperties properties;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt;

    public KisTokenManager(@Qualifier("kisRestClient") RestClient restClient, KisProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * KIS Access Token 반환.
     * 캐시에 토큰이 있으면 즉시 반환, 없으면 발급 후 캐시에 저장.
     */
    public String getAccessToken() {
        // Double-checked locking: 정상 캐시 히트는 lock 없이 반환 → 동시 호출 직렬화 회피.
        String t = cachedToken;
        Instant exp = tokenExpiresAt;
        if (t != null && exp != null && Instant.now().isBefore(exp)) {
            return t;
        }
        synchronized (this) {
            if (cachedToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt)) {
                return cachedToken;
            }
            return issueAndCacheToken();
        }
    }

    private String issueAndCacheToken() {
        try {
            Map<String, String> body = Map.of(
                "grant_type", "client_credentials",
                "appkey", properties.getKey(),
                "appsecret", properties.getSecret()
            );

            KisTokenResponse response = restClient.post()
                .uri(properties.getUrl() + TOKEN_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(KisTokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new KisApiException("KIS 토큰 발급 응답이 비어있습니다");
            }

            String accessToken = response.getAccessToken();
            cachedToken = accessToken;
            tokenExpiresAt = Instant.now().plusSeconds(response.getExpiresIn());
            log.info("KIS Access Token 발급 완료 (만료: {})", tokenExpiresAt);

            return accessToken;
        } catch (RestClientException e) {
            throw new KisApiException("KIS Access Token 발급 실패: " + e.getMessage(), e);
        }
    }
}