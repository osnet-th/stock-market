package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos;

import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config.EcosProperties;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.dto.EcosKeyStatResponse;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.exception.EcosApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class EcosApiClient {
    private final RestClient restClient;
    private final EcosProperties properties;

    public EcosKeyStatResponse fetchKeyStatistics() {
        try {
            return restClient.get()
                .uri(properties.getBaseUrl() + "/{apiKey}/json/kr/{startCount}/{endCount}",
                    properties.getApiKey(),
                    properties.getStartCount(),
                    properties.getEndCount())
                .retrieve()
                .body(EcosKeyStatResponse.class);
        } catch (RestClientException e) {
            throw new EcosApiException("ECOS API 호출 실패: " + e.getMessage(), e);
        }
    }
}