package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.config.SecProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecCompanyFactsResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class SecApiClient {

    private final RestClient restClient;
    private final SecProperties secProperties;

    public SecApiClient(@Qualifier("secRestClient") RestClient restClient, SecProperties secProperties) {
        this.restClient = restClient;
        this.secProperties = secProperties;
    }

    /**
     * SEC EDGAR Company Facts API 호출
     * @param cik10 10자리 패딩된 CIK (예: "CIK0000320193")
     */
    public SecCompanyFactsResponse fetchCompanyFacts(String cik10) {
        String uri = secProperties.getBaseUrl() + "/api/xbrl/companyfacts/" + cik10 + ".json";

        try {
            SecCompanyFactsResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
                        int statusCode = clientResponse.getStatusCode().value();
                        if (statusCode == 404) {
                            throw new SecApiException(SecErrorType.COMPANY_NOT_FOUND,
                                    "SEC에 등록되지 않은 기업입니다: " + cik10);
                        }
                        if (statusCode == 403) {
                            throw new SecApiException(SecErrorType.RATE_LIMIT_EXCEEDED,
                                    "SEC API 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요");
                        }
                        throw new SecApiException(SecErrorType.API_ERROR,
                                "SEC API 클라이언트 오류: " + statusCode);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, clientResponse) -> {
                        throw new SecApiException(SecErrorType.API_ERROR,
                                "SEC API 서버 오류: " + clientResponse.getStatusCode().value());
                    })
                    .body(SecCompanyFactsResponse.class);

            if (response == null) {
                throw new SecApiException(SecErrorType.API_ERROR, "SEC API 응답이 null입니다");
            }

            return response;
        } catch (SecApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw new SecApiException(SecErrorType.API_ERROR,
                    "SEC API 호출 실패: " + e.getMessage(), e);
        }
    }
}
