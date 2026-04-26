package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.KisProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisApiResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisApiResult;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
public class KisApiClient {

    private final RestClient restClient;
    private final KisProperties properties;
    private final KisTokenManager tokenManager;

    public KisApiClient(@Qualifier("kisRestClient") RestClient restClient,
                        KisProperties properties,
                        KisTokenManager tokenManager) {
        this.restClient = restClient;
        this.properties = properties;
        this.tokenManager = tokenManager;
    }

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

    /**
     * KIS GET API 호출 (응답 wrapper 우회).
     *
     * <p>{@link KisApiResponse} 형태가 아닌 응답(예: output1+output2 동시 반환) 을 위해 raw body
     * 를 그대로 반환한다. 응답 검증/파싱 책임은 호출자에 있다.
     */
    public <R> R getRaw(String path,
                        String trId,
                        Function<UriBuilder, URI> uriFunc,
                        ParameterizedTypeReference<R> responseType,
                        String description) {
        try {
            R body = restClient.get()
                .uri(properties.getUrl() + path, uriFunc)
                .headers(headers -> {
                    headers.setBearerAuth(tokenManager.getAccessToken());
                    headers.set("appkey", properties.getKey());
                    headers.set("appsecret", properties.getSecret());
                    headers.set("tr_id", trId);
                })
                .retrieve()
                .body(responseType);
            if (body == null) {
                throw new KisApiException(description + " 응답이 null");
            }
            return body;
        } catch (RestClientException e) {
            throw new KisApiException(description + " 실패: " + e.getMessage(), e);
        }
    }

    /**
     * KIS GET API 연속조회 호출.
     * 응답 헤더(tr_cont)를 포함한 결과를 반환하여 페이지네이션을 지원한다.
     *
     * @param path         API 경로
     * @param trId         거래 ID
     * @param trCont       연속조회 키 (최초: "", 연속: "N")
     * @param uriFunc      쿼리 파라미터 조립 함수
     * @param responseType 응답 타입 레퍼런스
     * @param description  에러 메시지용 API 설명
     * @return 응답 output과 다음 페이지 존재 여부
     */
    public <T> KisApiResult<T> getWithContinuation(String path,
                                                    String trId,
                                                    String trCont,
                                                    Function<UriBuilder, URI> uriFunc,
                                                    ParameterizedTypeReference<KisApiResponse<T>> responseType,
                                                    String description) {
        try {
            ResponseEntity<KisApiResponse<T>> entity = restClient.get()
                .uri(properties.getUrl() + path, uriFunc)
                .headers(headers -> {
                    headers.setBearerAuth(tokenManager.getAccessToken());
                    headers.set("appkey", properties.getKey());
                    headers.set("appsecret", properties.getSecret());
                    headers.set("tr_id", trId);
                    headers.set("tr_cont", trCont);
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new KisApiException(description + " 클라이언트 오류: " + response.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new KisApiException(description + " 서버 오류: " + response.getStatusCode());
                })
                .toEntity(responseType);

            KisApiResponse<T> response = entity.getBody();
            if (response == null) {
                throw new KisApiException(description + " 응답이 null");
            }
            if (!response.isSuccess()) {
                throw new KisApiException(description + " 실패: " + response.getMessage());
            }

            String responseTrCont = entity.getHeaders().getFirst("tr_cont");
            boolean hasNext = "F".equals(responseTrCont) || "M".equals(responseTrCont);

            return new KisApiResult<>(response.getOutput(), hasNext);

        } catch (RestClientException e) {
            throw new KisApiException(description + " 실패: " + e.getMessage(), e);
        }
    }
}