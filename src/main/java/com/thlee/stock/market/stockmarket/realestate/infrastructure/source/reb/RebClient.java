package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateRestClientConfig;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.BaseUri;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.SecretMasker;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.exception.RebApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * 한국부동산원 R-ONE Open API 클라이언트.
 * <p>
 * 통계표 ID(STATBL_ID), DTL_STATBL_ID, GRP_ID 등 통계표별 파라미터는 어댑터가 결정한다.
 * R-ONE 응답 envelope은 Map으로 받아 어댑터에서 정규화.
 */
@Component
public class RebClient {

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    public RebClient(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                     RealEstateMarketProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public Map<String, Object> fetch(String path, Map<String, String> queryParams) {
        RealEstateMarketProperties.SourceProps reb = properties.getReb();
        if (reb.getApiKey() == null || reb.getApiKey().isBlank()) {
            throw new IllegalStateException("REB api-key not configured");
        }
        BaseUri base = BaseUri.parse(reb.getBaseUrl());
        try {
            return restClient.get()
                    .uri(builder -> {
                        var uri = builder.scheme(base.scheme())
                                .host(base.host())
                                .port(base.port())
                                // 기존 동작 유지: REB는 contextPath 합치지 않고 path만 사용
                                .path(path)
                                .queryParam("KEY", reb.getApiKey())
                                .queryParam("Type", "json");
                        queryParams.forEach(uri::queryParam);
                        return uri.build();
                    })
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new RebApiException("REB API call failed: " + path, SecretMasker.sanitize(e));
        }
    }
}