package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * R-ONE 가격지수/거래호수/지가변동률 어댑터.
 * <p>
 * 통계표 ID는 운영 시점에 R-ONE Open API 명세를 확인하여 yml/properties 단계에서 채워 넣는다.
 * MVP 시점에는 인증키 미발급으로 IllegalStateException 후 스케줄러가 skip.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RebAdapter implements RealEstateMarketSourceAdapter {

    private static final String SALE_PRICE_INDEX_PATH = "/SttsApiTblData.do";

    private final RebClient client;

    @Override
    public RealEstateMarketSource supportedSource() {
        return RealEstateMarketSource.REB;
    }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(
                RealEstateMarketCategory.PRICE_INDEX,
                RealEstateMarketCategory.RENT,
                RealEstateMarketCategory.OFFICIAL_STATS);
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (!supportedCategories().contains(category)) {
            return FetchResult.failure("unsupported category: " + category);
        }
        try {
            String statTblId = resolveStatTblId(category);
            if (statTblId == null) {
                return FetchResult.failure("REB stat table not mapped for " + category);
            }
            Map<String, Object> response = client.fetch(SALE_PRICE_INDEX_PATH, Map.of(
                    "STATBL_ID", statTblId,
                    "Region_Code", region.value(),
                    "STAT_DATE_FROM", window.from().toString().replace("-", "").substring(0, 6),
                    "STAT_DATE_TO", window.to().toString().replace("-", "").substring(0, 6)));
            BigDecimal latestValue = parseLatestIndex(response);
            if (latestValue == null) {
                return FetchResult.success(List.of());
            }
            return FetchResult.success(List.of(buildIndicator(region, category, statTblId, latestValue, window)));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.reb] fetch failed: region={}, category={}", region, category, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    private String resolveStatTblId(RealEstateMarketCategory category) {
        return switch (category) {
            case PRICE_INDEX -> "A_2024_00404"; // 매매가격지수 (실 운영 시 명세 확인)
            case RENT -> "A_2024_00405";        // 전세가격지수
            case OFFICIAL_STATS -> "A_2024_00410"; // 거래호수
            default -> null;
        };
    }

    private BigDecimal parseLatestIndex(Map<String, Object> response) {
        if (response == null) return null;
        Object data = response.get("SttsApiTblData");
        if (!(data instanceof List<?> list) || list.isEmpty()) return null;
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> row)) return null;
        Object value = row.get("DTA_VAL");
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RealEstateMarketIndicator buildIndicator(RegionCode region,
                                                     RealEstateMarketCategory category,
                                                     String statTblId,
                                                     BigDecimal value,
                                                     FetchWindow window) {
        String indicatorCode = switch (category) {
            case PRICE_INDEX -> "SALE_PRICE_INDEX";
            case RENT -> "JEONSE_PRICE_INDEX";
            case OFFICIAL_STATS -> "APT_TRANSACTION_COUNT";
            default -> "REB_" + statTblId;
        };
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(category)
                .source(RealEstateMarketSource.REB)
                .indicatorCode(indicatorCode)
                .referenceText(window.to().toString().substring(0, 7))
                .value(value)
                .sourceUrl("https://www.reb.or.kr/r-one/")
                .cycle("M")
                .snapshotDate(LocalDateTime.now())
                .build();
    }
}