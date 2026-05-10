package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.hub;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateRestClientConfig;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.SecretMasker;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.hub.exception.HubApiException;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit.dto.MolitApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * 건축HUB 주택 인허가/착공/사용승인 어댑터 (data.go.kr envelope).
 */
@Component
@Slf4j
public class HubAdapter implements RealEstateMarketSourceAdapter {

    private static final String HOUSING_PERMIT_PATH =
            "/HousingApprovalSttusService/getHousingApprovalSttus";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    public HubAdapter(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                      RealEstateMarketProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public RealEstateMarketSource supportedSource() { return RealEstateMarketSource.HUB; }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.SUPPLY);
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.SUPPLY) {
            return FetchResult.failure("unsupported category: " + category);
        }
        RealEstateMarketProperties.SourceProps hub = properties.getHub();
        if (hub.getApiKey() == null || hub.getApiKey().isBlank()) {
            throw new IllegalStateException("HUB api-key not configured");
        }
        try {
            MolitApiResponse response = restClient.get()
                    .uri(builder -> builder
                            .scheme("https")
                            .host(stripScheme(hub.getBaseUrl()))
                            .path(stripContext(hub.getBaseUrl()) + HOUSING_PERMIT_PATH)
                            .queryParam("serviceKey", hub.getApiKey())
                            .queryParam("sigunguCd", region.value())
                            .queryParam("strtYm", window.from().toString().replace("-", "").substring(0, 6))
                            .queryParam("endYm", window.to().toString().replace("-", "").substring(0, 6))
                            .queryParam("_type", "json")
                            .queryParam("numOfRows", 200)
                            .build())
                    .retrieve()
                    .body(MolitApiResponse.class);
            if (response == null || !response.isSuccess()) {
                return FetchResult.success(List.of());
            }
            return FetchResult.success(buildIndicators(region, response.rows(), window));
        } catch (RestClientException e) {
            throw new HubApiException("HUB API call failed", SecretMasker.sanitize(e));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.hub] fetch failed: region={}", region, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    private List<RealEstateMarketIndicator> buildIndicators(RegionCode region,
                                                             List<Map<String, Object>> rows,
                                                             FetchWindow window) {
        BigDecimal permit = sumField(rows, "permitHshldCnt", "PermitHshldCnt");
        BigDecimal started = sumField(rows, "strtnHshldCnt", "StrtnHshldCnt");
        BigDecimal approved = sumField(rows, "useapprHshldCnt", "UseapprHshldCnt");
        String reference = window.to().toString().substring(0, 7);
        LocalDateTime snapshot = LocalDateTime.now();
        return List.of(
                indicator(region, "PERMIT_HOUSEHOLDS_12M", permit, reference, snapshot),
                indicator(region, "STARTED_HOUSEHOLDS_12M", started, reference, snapshot),
                indicator(region, "APPROVED_HOUSEHOLDS_12M", approved, reference, snapshot)
        );
    }

    private BigDecimal sumField(List<Map<String, Object>> rows, String... keys) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            for (String key : keys) {
                Object raw = row.get(key);
                if (raw == null) continue;
                try {
                    total = total.add(new BigDecimal(raw.toString().trim()));
                } catch (NumberFormatException ignored) {
                }
                break;
            }
        }
        return total;
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                String code,
                                                BigDecimal value,
                                                String reference,
                                                LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(RealEstateMarketCategory.SUPPLY)
                .source(RealEstateMarketSource.HUB)
                .indicatorCode(code)
                .referenceText(reference)
                .value(value)
                .sourceUrl("https://www.hub.go.kr/")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }

    private String stripScheme(String baseUrl) {
        String t = baseUrl.replaceFirst("^https?://", "");
        int slash = t.indexOf('/');
        return slash > 0 ? t.substring(0, slash) : t;
    }

    private String stripContext(String baseUrl) {
        String t = baseUrl.replaceFirst("^https?://", "");
        int slash = t.indexOf('/');
        return slash > 0 ? t.substring(slash) : "";
    }
}