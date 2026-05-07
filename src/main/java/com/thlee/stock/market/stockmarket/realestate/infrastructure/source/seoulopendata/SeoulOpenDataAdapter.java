package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.seoulopendata;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.seoulopendata.exception.SeoulOpenDataApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 서울 열린데이터광장 어댑터 — 서울 자치구만 지원 (region prefix 11).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SeoulOpenDataAdapter implements RealEstateMarketSourceAdapter {

    private static final String TRADE_DATASET = "tbLnOpendataRtmsV";
    private static final String REDEVELOPMENT_DATASET = "tbLnOpendataRcptDevelopV";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    @Override
    public RealEstateMarketSource supportedSource() {
        return RealEstateMarketSource.SEOUL_OPEN_DATA;
    }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.SEOUL_LOCAL);
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.SEOUL_LOCAL) {
            return FetchResult.failure("unsupported category: " + category);
        }
        if (!region.isSeoul()) {
            return FetchResult.success(List.of());
        }
        RealEstateMarketProperties.SourceProps props = properties.getSeoulOpenData();
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("SEOUL_OPEN_DATA api-key not configured");
        }
        try {
            BigDecimal tradeCount = countDataset(props, region, TRADE_DATASET);
            BigDecimal redevelopmentArea = countDataset(props, region, REDEVELOPMENT_DATASET);
            String reference = window.to().toString().substring(0, 7);
            LocalDateTime snapshot = LocalDateTime.now();
            return FetchResult.success(List.of(
                    indicator(region, "SEOUL_TRADE_COUNT", tradeCount, reference, snapshot),
                    indicator(region, "REDEVELOPMENT_AREA_COUNT", redevelopmentArea, reference, snapshot)
            ));
        } catch (RestClientException e) {
            throw new SeoulOpenDataApiException("SEOUL_OPEN_DATA API call failed", e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.seoul] fetch failed: region={}", region, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private BigDecimal countDataset(RealEstateMarketProperties.SourceProps props,
                                    RegionCode region,
                                    String dataset) {
        Map<String, Object> response = restClient.get()
                .uri(builder -> builder
                        .scheme("http")
                        .host(stripScheme(props.getBaseUrl()))
                        .port(extractPort(props.getBaseUrl()))
                        .path(String.format("/%s/json/%s/1/1/", props.getApiKey(), dataset))
                        .build())
                .retrieve()
                .body(Map.class);
        if (response == null) return BigDecimal.ZERO;
        Object outer = response.get(dataset);
        if (!(outer instanceof Map<?, ?> wrapper)) return BigDecimal.ZERO;
        Object total = wrapper.get("list_total_count");
        if (total == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(total.toString().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                String code,
                                                BigDecimal value,
                                                String reference,
                                                LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(RealEstateMarketCategory.SEOUL_LOCAL)
                .source(RealEstateMarketSource.SEOUL_OPEN_DATA)
                .indicatorCode(code)
                .referenceText(reference)
                .value(value)
                .sourceUrl("https://data.seoul.go.kr/")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }

    private String stripScheme(String baseUrl) {
        String t = baseUrl.replaceFirst("^https?://", "");
        int colon = t.indexOf(':');
        int slash = t.indexOf('/');
        int end = colon > 0 ? colon : (slash > 0 ? slash : t.length());
        return t.substring(0, end);
    }

    private int extractPort(String baseUrl) {
        String t = baseUrl.replaceFirst("^https?://", "");
        int colon = t.indexOf(':');
        if (colon < 0) return -1;
        int slash = t.indexOf('/', colon);
        String portStr = slash > 0 ? t.substring(colon + 1, slash) : t.substring(colon + 1);
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}