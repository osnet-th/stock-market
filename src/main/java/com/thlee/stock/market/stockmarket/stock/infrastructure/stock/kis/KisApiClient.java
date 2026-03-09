package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.KisProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisApiResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.function.Function;

/**
 * KIS Open API 범용 HTTP 클라이언트.
 * 인증 헤더, 응답 검증 등 공통 관심사를 캡슐화한다.
 */
@Component
@RequiredArgsConstructor
public class KisApiClient {

    private final RestClient restClient;
    private final KisProperties properties;
    private final KisTokenManager tokenManager;

    /**
     * KIS GET API 호출.
     *
     * @param path         API 경로 (예: /uapi/domestic-stock/v1/quotations/inquire-price)
     * @param trId         거래 ID (예: FHKST01010100)
     * @param uriFunc      쿼리 파라미터 조립 함수
     * @param responseType 응답 타입 레퍼런스
     * @param description  에러 메시지용 API 설명 (예: "국내 현재가 조회")
     * @return 응답 output
     */
    public <T> T get(String path,
                     String trId,
                     Function<UriBuilder, URI> uriFunc,
                     ParameterizedTypeReference<KisApiResponse<T>> responseType,
                     String description) {
        try {
            KisApiResponse<T> response = restClient.get()
                .uri(properties.getUrl() + path, uriFunc)
                .headers(headers -> {
                    headers.setBearerAuth(tokenManager.getAccessToken());
                    headers.set("appkey", properties.getKey());
                    headers.set("appsecret", properties.getSecret());
                    headers.set("tr_id", trId);
                })
                .retrieve()
                .body(responseType);

            if (response == null) {
                throw new KisApiException(description + " 응답이 null");
            }
            if (!response.isSuccess()) {
                throw new KisApiException(description + " 실패: " + response.getMessage());
            }

            return response.getOutput();

        } catch (RestClientException e) {
            throw new KisApiException(description + " 실패: " + e.getMessage(), e);
        }
    }
}