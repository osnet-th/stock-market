package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.ggdatadream;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateRestClientConfig;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.BaseUri;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.SecretMasker;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.ggdatadream.exception.GgDataDreamApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 경기도 데이터드림 어댑터 — 경기 시군만 지원 (region prefix 41).
 */
@Component
@Slf4j
public class GgDataDreamAdapter implements RealEstateMarketSourceAdapter {

    private static final String DATASET_PATH = "/AptMaeMul";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    public GgDataDreamAdapter(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                              RealEstateMarketProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public RealEstateMarketSource supportedSource() {
        return RealEstateMarketSource.GG_DATA_DREAM;
    }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.GYEONGGI_LOCAL);
    }

    @Override
    public boolean supportsRegion(RegionCode region) {
        return region.isGyeonggi();
    }

    @SuppressWarnings("unchecked")
    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.GYEONGGI_LOCAL) {
            return FetchResult.failure("unsupported category: " + category);
        }
        // region 가드는 supportsRegion() 으로 사전 필터됨
        RealEstateMarketProperties.SourceProps props = properties.getGgDataDream();
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("GG_DATA_DREAM api-key not configured");
        }
        BaseUri base = BaseUri.parse(props.getBaseUrl());
        try {
            Map<String, Object> response = restClient.get()
                    .uri(builder -> builder
                            .scheme(base.scheme())
                            .host(base.host())
                            .port(base.port())
                            .path(base.contextPath() + DATASET_PATH)
                            .queryParam("KEY", props.getApiKey())
                            .queryParam("Type", "json")
                            .queryParam("SIGUNGU_CODE", region.value())
                            .queryParam("pSize", 200)
                            .build())
                    .retrieve()
                    .body(Map.class);
            List<Map<String, Object>> rows = extractRows(response);
            return FetchResult.success(buildIndicators(region, rows, window));
        } catch (RestClientException e) {
            throw new GgDataDreamApiException("GG_DATA_DREAM API call failed", SecretMasker.sanitize(e));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.gg] fetch failed: region={}", region, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(Map<String, Object> response) {
        if (response == null) return List.of();
        for (Object value : response.values()) {
            if (value instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?>) {
                for (Object inner : (List<?>) value) {
                    if (inner instanceof Map<?, ?> wrapper && wrapper.containsKey("row")) {
                        Object rowValue = wrapper.get("row");
                        if (rowValue instanceof List<?> rowList) {
                            return (List<Map<String, Object>>) rowList;
                        }
                    }
                }
            }
        }
        return List.of();
    }

    private List<RealEstateMarketIndicator> buildIndicators(RegionCode region,
                                                            List<Map<String, Object>> rows,
                                                            FetchWindow window) {
        BigDecimal count = BigDecimal.valueOf(rows.size());
        BigDecimal avgPrice = averageDealAmount(rows);
        String reference = window.to().toString().substring(0, 7);
        LocalDateTime snapshot = LocalDateTime.now();
        return List.of(
                indicator(region, "GG_TRADE_COUNT", count, reference, snapshot),
                indicator(region, "GG_TRADE_AVG_PRICE", avgPrice, reference, snapshot)
        );
    }

    private BigDecimal averageDealAmount(List<Map<String, Object>> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Map<String, Object> row : rows) {
            Object raw = row.get("DEAL_AMT");
            if (raw == null) continue;
            try {
                sum = sum.add(new BigDecimal(raw.toString().replace(",", "").trim()));
                count++;
            } catch (NumberFormatException ignored) {
            }
        }
        if (count == 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                String code,
                                                BigDecimal value,
                                                String reference,
                                                LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(RealEstateMarketCategory.GYEONGGI_LOCAL)
                .source(RealEstateMarketSource.GG_DATA_DREAM)
                .indicatorCode(code)
                .referenceText(reference)
                .value(value)
                .sourceUrl("https://data.gg.go.kr/")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }

}