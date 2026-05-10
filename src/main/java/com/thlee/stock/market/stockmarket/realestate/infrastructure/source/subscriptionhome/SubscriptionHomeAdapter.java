package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.subscriptionhome;

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
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.subscriptionhome.exception.SubscriptionHomeApiException;
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
 * 청약홈 청약 경쟁률 어댑터.
 * <p>
 * 출처: 한국부동산원 청약홈 분양정보/경쟁률 서비스 (odcloud 게시).
 * odcloud 표준 응답: {data:[{...}], totalCount, currentCount, page, perPage}.
 * <p>
 * MVP는 APT 경쟁률(getAPTLttotPblancCmpet)만 수집한다. 오피스텔/도시형/공공임대 등
 * 다른 operation은 후속 sprint에서 주택유형 확장과 함께 도입.
 * 응답 필드명({@code HSSPLY_AREA_CODE}, {@code PBLANC_CMPET_RT})은 운영 dry-run으로 검증 필요.
 */
@Component
@Slf4j
public class SubscriptionHomeAdapter implements RealEstateMarketSourceAdapter {

    private static final String COMPETITION_PATH =
            "/ApplyhomeInfoCmpetRtSvc/v1/getAPTLttotPblancCmpet";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    public SubscriptionHomeAdapter(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                                   RealEstateMarketProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public RealEstateMarketSource supportedSource() {
        return RealEstateMarketSource.SUBSCRIPTION_HOME;
    }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.SUBSCRIPTION);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.SUBSCRIPTION) {
            return FetchResult.failure("unsupported category: " + category);
        }
        RealEstateMarketProperties.SourceProps props = properties.getSubscriptionHome();
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("SUBSCRIPTION_HOME api-key not configured");
        }
        BaseUri base = BaseUri.parse(props.getBaseUrl());
        try {
            Map<String, Object> response = restClient.get()
                    .uri(builder -> builder
                            .scheme(base.scheme())
                            .host(base.host())
                            .port(base.port())
                            .path(base.contextPath() + COMPETITION_PATH)
                            .queryParam("serviceKey", props.getApiKey())
                            .queryParam("page", 1)
                            .queryParam("perPage", 200)
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (response == null) return FetchResult.success(List.of());
            List<Map<String, Object>> rows = (List<Map<String, Object>>)
                    response.getOrDefault("data", List.of());
            List<Map<String, Object>> filtered = filterByRegion(rows, region);
            return FetchResult.success(buildIndicators(region, filtered, window));
        } catch (RestClientException e) {
            throw new SubscriptionHomeApiException("SUBSCRIPTION_HOME API call failed", SecretMasker.sanitize(e));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.subscriptionhome] fetch failed: region={}", region, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    private List<Map<String, Object>> filterByRegion(List<Map<String, Object>> rows, RegionCode region) {
        return rows.stream()
                .filter(row -> {
                    Object code = row.get("HSSPLY_AREA_CODE");
                    return code != null && region.value().equals(code.toString().substring(0,
                            Math.min(5, code.toString().length())));
                })
                .toList();
    }

    private List<RealEstateMarketIndicator> buildIndicators(RegionCode region,
                                                            List<Map<String, Object>> rows,
                                                            FetchWindow window) {
        BigDecimal supplyCount = BigDecimal.valueOf(rows.size());
        BigDecimal avgRatio = averageRatio(rows);
        long failed = rows.stream().filter(this::isFailed).count();
        String reference = window.to().toString().substring(0, 7);
        LocalDateTime snapshot = LocalDateTime.now();
        return List.of(
                indicator(region, "AVG_FIRST_PRIORITY_RATIO", avgRatio, reference, snapshot),
                indicator(region, "RECENT_SUPPLY_COUNT", supplyCount, reference, snapshot),
                indicator(region, "FAILED_SUPPLY_COUNT", BigDecimal.valueOf(failed), reference, snapshot)
        );
    }

    private BigDecimal averageRatio(List<Map<String, Object>> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Map<String, Object> row : rows) {
            Object raw = row.get("PBLANC_CMPET_RT");
            if (raw == null) continue;
            try {
                sum = sum.add(new BigDecimal(raw.toString().replaceAll("[^0-9.]", "")));
                count++;
            } catch (NumberFormatException ignored) {
            }
        }
        if (count == 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private boolean isFailed(Map<String, Object> row) {
        Object raw = row.get("PBLANC_CMPET_RT");
        if (raw == null) return false;
        try {
            return new BigDecimal(raw.toString().replaceAll("[^0-9.]", ""))
                    .compareTo(BigDecimal.ONE) < 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                String code,
                                                BigDecimal value,
                                                String reference,
                                                LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(RealEstateMarketCategory.SUBSCRIPTION)
                .source(RealEstateMarketSource.SUBSCRIPTION_HOME)
                .indicatorCode(code)
                .referenceText(reference)
                .value(value)
                .sourceUrl("https://www.applyhome.co.kr/")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }

}