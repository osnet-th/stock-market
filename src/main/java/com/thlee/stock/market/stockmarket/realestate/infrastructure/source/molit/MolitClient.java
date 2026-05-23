package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateRestClientConfig;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.BaseUri;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.SecretMasker;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit.dto.MolitApiResponse;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit.exception.MolitApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 국토교통부 data.go.kr 실거래가 API 클라이언트.
 * <p>
 * 인증키 미설정 시 즉시 {@link IllegalStateException} — 일배치 스케줄러가 항목 단위 skip.
 */
@Component
public class MolitClient {

    private static final String APT_TRADE_PATH = "/RTMSDataSvcAptTradeDev/getRTMSDataSvcAptTradeDev";
    private static final String APT_RENT_PATH = "/RTMSDataSvcAptRent/getRTMSDataSvcAptRent";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    public MolitClient(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                       RealEstateMarketProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public MolitApiResponse fetchAptTrade(String regionCode, String dealYearMonth) {
        return invoke(APT_TRADE_PATH, regionCode, dealYearMonth);
    }

    public MolitApiResponse fetchAptRent(String regionCode, String dealYearMonth) {
        return invoke(APT_RENT_PATH, regionCode, dealYearMonth);
    }

    private MolitApiResponse invoke(String path, String regionCode, String dealYearMonth) {
        RealEstateMarketProperties.SourceProps molit = properties.getMolit();
        requireApiKey(molit);
        BaseUri base = BaseUri.parse(molit.getBaseUrl());
        try {
            return restClient.get()
                    .uri(builder -> builder
                            .scheme(base.scheme())
                            .host(base.host())
                            .port(base.port())
                            .path(base.contextPath() + path)
                            .queryParam("serviceKey", molit.getApiKey())
                            .queryParam("LAWD_CD", regionCode)
                            .queryParam("DEAL_YMD", dealYearMonth)
                            .queryParam("_type", "json")
                            .queryParam("numOfRows", 1000)
                            .build())
                    .retrieve()
                    .body(MolitApiResponse.class);
        } catch (RestClientException e) {
            throw new MolitApiException("MOLIT API 호출 실패: " + path, SecretMasker.sanitize(e));
        }
    }

    private void requireApiKey(RealEstateMarketProperties.SourceProps props) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("MOLIT api-key not configured");
        }
    }
}
