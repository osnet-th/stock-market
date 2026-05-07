package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.hug;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.hug.exception.HugApiException;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit.dto.MolitApiResponse;
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
 * HUG 분양보증 사고현황/사고금액/사고세대수 어댑터 (data.go.kr envelope).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HugAdapter implements RealEstateMarketSourceAdapter {

    private static final String INCIDENT_PATH = "/MhmDamageMnyDamageHshldCo/getMhmDamageMnyDamageHshldCo";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    @Override
    public RealEstateMarketSource supportedSource() { return RealEstateMarketSource.HUG; }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.GUARANTEE_RISK);
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.GUARANTEE_RISK) {
            return FetchResult.failure("unsupported category: " + category);
        }
        RealEstateMarketProperties.SourceProps hug = properties.getHug();
        if (hug.getApiKey() == null || hug.getApiKey().isBlank()) {
            throw new IllegalStateException("HUG api-key not configured");
        }
        try {
            MolitApiResponse response = restClient.get()
                    .uri(builder -> builder
                            .scheme("https")
                            .host(stripScheme(hug.getBaseUrl()))
                            .path(stripContext(hug.getBaseUrl()) + INCIDENT_PATH)
                            .queryParam("serviceKey", hug.getApiKey())
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
            throw new HugApiException("HUG API call failed", e);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.hug] fetch failed: region={}", region, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    private List<RealEstateMarketIndicator> buildIndicators(RegionCode region,
                                                            List<Map<String, Object>> rows,
                                                            FetchWindow window) {
        long count = rows.size();
        BigDecimal totalAmount = sumField(rows, "damageAmt", "DamageAmt");
        BigDecimal totalHouseholds = sumField(rows, "damageHshldCo", "DamageHshldCo");
        String reference = window.to().toString().substring(0, 7);
        LocalDateTime snapshot = LocalDateTime.now();
        return List.of(
                indicator(region, "GUARANTEE_INCIDENT_COUNT", BigDecimal.valueOf(count), reference, snapshot),
                indicator(region, "GUARANTEE_INCIDENT_AMOUNT", totalAmount, reference, snapshot),
                indicator(region, "GUARANTEE_INCIDENT_HOUSEHOLDS", totalHouseholds, reference, snapshot)
        );
    }

    private BigDecimal sumField(List<Map<String, Object>> rows, String... keys) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            for (String key : keys) {
                Object raw = row.get(key);
                if (raw == null) continue;
                try {
                    total = total.add(new BigDecimal(raw.toString().replace(",", "").trim()));
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
                .category(RealEstateMarketCategory.GUARANTEE_RISK)
                .source(RealEstateMarketSource.HUG)
                .indicatorCode(code)
                .referenceText(reference)
                .value(value)
                .sourceUrl("https://www.khug.or.kr/")
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