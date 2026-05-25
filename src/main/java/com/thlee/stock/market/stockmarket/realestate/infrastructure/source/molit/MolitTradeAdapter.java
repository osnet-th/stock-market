package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit.dto.MolitApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MOLIT 아파트 매매 실거래가 어댑터.
 * <p>
 * TRADE 카테고리(거래량/평균가/중위가/해제 비율)와 PRICE_INDEX의 PRICE_PER_SQM(MOLIT 출처)를 채운다.
 * raw 거래 row는 보존하지 않고 가공된 indicator만 history에 적재한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MolitTradeAdapter implements RealEstateMarketSourceAdapter {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final MolitClient client;

    @Override
    public RealEstateMarketSource supportedSource() {
        return RealEstateMarketSource.MOLIT;
    }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.TRADE, RealEstateMarketCategory.PRICE_INDEX);
    }

    @Override
    public boolean supportsRegion(RegionCode region) {
        // MOLIT은 시군구 단위(5자리)만 — SIDO row 차단
        return region != null && region.isSigungu();
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (!supportedCategories().contains(category)) {
            return FetchResult.failure("unsupported category: " + category);
        }

        List<Map<String, Object>> rows = collectRows(region, window);
        if (rows.isEmpty()) {
            return FetchResult.success(List.of());
        }

        return switch (category) {
            case TRADE -> FetchResult.success(buildTradeIndicators(region, rows, window));
            case PRICE_INDEX -> FetchResult.success(buildPriceIndexIndicators(region, rows, window));
            default -> FetchResult.failure("unsupported category: " + category);
        };
    }

    private List<Map<String, Object>> collectRows(RegionCode region, FetchWindow window) {
        List<Map<String, Object>> all = new ArrayList<>();
        var ym = window.from().withDayOfMonth(1);
        while (!ym.isAfter(window.to())) {
            try {
                MolitApiResponse response = client.fetchAptTrade(region.value(), ym.format(YEAR_MONTH));
                if (!response.isSuccess()) {
                    log.warn("[realestate.molit.trade] non-success response: region={}, ym={}",
                            region, ym);
                } else {
                    all.addAll(response.rows());
                }
            } catch (Exception e) {
                log.warn("[realestate.molit.trade] fetch failed: region={}, ym={}", region, ym, e);
            }
            ym = ym.plusMonths(1);
        }
        return all;
    }

    private List<RealEstateMarketIndicator> buildTradeIndicators(RegionCode region,
                                                                  List<Map<String, Object>> rows,
                                                                  FetchWindow window) {
        List<BigDecimal> prices = extractPrices(rows);
        BigDecimal volume = BigDecimal.valueOf(rows.size());
        BigDecimal avg = average(prices);
        BigDecimal median = median(prices);
        BigDecimal cancelRatio = cancelRatio(rows);

        String reference = window.to().toString();
        LocalDateTime snapshot = LocalDateTime.now();

        return List.of(
                indicator(region, RealEstateMarketCategory.TRADE,
                        "APT_TRADE_VOLUME", reference, volume, snapshot),
                indicator(region, RealEstateMarketCategory.TRADE,
                        "APT_TRADE_AVG_PRICE", reference, avg, snapshot),
                indicator(region, RealEstateMarketCategory.TRADE,
                        "APT_TRADE_MEDIAN_PRICE", reference, median, snapshot),
                indicator(region, RealEstateMarketCategory.TRADE,
                        "APT_TRADE_CANCEL_RATIO", reference, cancelRatio, snapshot)
        );
    }

    private List<RealEstateMarketIndicator> buildPriceIndexIndicators(RegionCode region,
                                                                       List<Map<String, Object>> rows,
                                                                       FetchWindow window) {
        BigDecimal pricePerSqm = pricePerSqm(rows);
        if (pricePerSqm == null) {
            return List.of();
        }
        return List.of(indicator(
                region,
                RealEstateMarketCategory.PRICE_INDEX,
                "PRICE_PER_SQM",
                window.to().toString(),
                pricePerSqm,
                LocalDateTime.now()));
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                 RealEstateMarketCategory category,
                                                 String indicatorCode,
                                                 String referenceText,
                                                 BigDecimal value,
                                                 LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(category)
                .source(RealEstateMarketSource.MOLIT)
                .indicatorCode(indicatorCode)
                .referenceText(referenceText)
                .value(value)
                .sourceUrl("https://www.data.go.kr/data/15126469/openapi.do")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }

    private List<BigDecimal> extractPrices(List<Map<String, Object>> rows) {
        return rows.stream()
                .map(this::parseDealAmount)
                .filter(amount -> amount != null && amount.signum() > 0)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private BigDecimal parseDealAmount(Map<String, Object> row) {
        Object raw = row.get("dealAmount");
        if (raw == null) raw = row.get("DealAmount");
        if (raw == null) return null;
        try {
            return new BigDecimal(raw.toString().replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) return BigDecimal.ZERO;
        int n = values.size();
        if (n % 2 == 1) {
            return values.get(n / 2);
        }
        BigDecimal lo = values.get(n / 2 - 1);
        BigDecimal hi = values.get(n / 2);
        return lo.add(hi).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal cancelRatio(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return BigDecimal.ZERO;
        long cancelled = rows.stream()
                .filter(row -> {
                    Object cd = row.getOrDefault("cdealType", row.get("CdealType"));
                    return cd != null && "O".equalsIgnoreCase(cd.toString().trim());
                })
                .count();
        return BigDecimal.valueOf(cancelled)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal pricePerSqm(List<Map<String, Object>> rows) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        BigDecimal totalArea = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            BigDecimal price = parseDealAmount(row);
            BigDecimal area = parseArea(row);
            if (price == null || area == null || area.signum() <= 0) continue;
            totalPrice = totalPrice.add(price);
            totalArea = totalArea.add(area);
        }
        if (totalArea.signum() <= 0) return null;
        return totalPrice.divide(totalArea, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal parseArea(Map<String, Object> row) {
        Object raw = row.get("excluUseAr");
        if (raw == null) raw = row.get("ExcluUseAr");
        if (raw == null) return null;
        try {
            return new BigDecimal(raw.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
