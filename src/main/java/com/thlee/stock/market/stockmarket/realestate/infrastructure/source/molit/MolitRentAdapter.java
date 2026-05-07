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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MOLIT 아파트 전월세 실거래가 어댑터 — RENT 카테고리.
 * <p>
 * 전세보증금 평균, 월세 거래 비중, 전세가율 추정값을 채운다.
 * 전세가율은 매매 평균이 같은 region/period에 있다는 전제로 RentAdapter가 직접 계산하지 않고
 * Aggregator(read 시점) 또는 SaveService 후처리에서 계산하는 것이 정합 — 본 어댑터는 plain 전세 평균만.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MolitRentAdapter implements RealEstateMarketSourceAdapter {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final MolitClient client;

    @Override
    public RealEstateMarketSource supportedSource() {
        return RealEstateMarketSource.MOLIT;
    }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.RENT);
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.RENT) {
            return FetchResult.failure("unsupported category: " + category);
        }

        List<Map<String, Object>> rows = collectRows(region, window);
        if (rows.isEmpty()) {
            return FetchResult.success(List.of());
        }

        BigDecimal jeonseAvg = jeonseDepositAverage(rows);
        BigDecimal monthlyRatio = monthlyRentRatio(rows);

        String reference = window.to().toString();
        LocalDateTime snapshot = LocalDateTime.now();
        List<RealEstateMarketIndicator> result = new ArrayList<>(2);
        if (jeonseAvg != null) {
            result.add(indicator(region, "JEONSE_DEPOSIT_AVG", reference, jeonseAvg, snapshot));
        }
        result.add(indicator(region, "MONTHLY_RENT_RATIO", reference, monthlyRatio, snapshot));
        return FetchResult.success(result);
    }

    private List<Map<String, Object>> collectRows(RegionCode region, FetchWindow window) {
        List<Map<String, Object>> all = new ArrayList<>();
        var ym = window.from().withDayOfMonth(1);
        while (!ym.isAfter(window.to())) {
            try {
                MolitApiResponse response = client.fetchAptRent(region.value(), ym.format(YEAR_MONTH));
                if (!response.isSuccess()) {
                    log.warn("[realestate.molit.rent] non-success response: region={}, ym={}",
                            region, ym);
                } else {
                    all.addAll(response.rows());
                }
            } catch (Exception e) {
                log.warn("[realestate.molit.rent] fetch failed: region={}, ym={}", region, ym, e);
            }
            ym = ym.plusMonths(1);
        }
        return all;
    }

    private BigDecimal jeonseDepositAverage(List<Map<String, Object>> rows) {
        List<BigDecimal> deposits = rows.stream()
                .filter(row -> {
                    BigDecimal monthly = parseAmount(row, "monthlyRent", "MonthlyRent");
                    return monthly == null || monthly.signum() == 0;
                })
                .map(row -> parseAmount(row, "deposit", "Deposit"))
                .filter(d -> d != null && d.signum() > 0)
                .toList();
        if (deposits.isEmpty()) return null;
        BigDecimal sum = deposits.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(deposits.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal monthlyRentRatio(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return BigDecimal.ZERO;
        long monthly = rows.stream()
                .filter(row -> {
                    BigDecimal m = parseAmount(row, "monthlyRent", "MonthlyRent");
                    return m != null && m.signum() > 0;
                })
                .count();
        return BigDecimal.valueOf(monthly)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(rows.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal parseAmount(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object raw = row.get(key);
            if (raw == null) continue;
            try {
                return new BigDecimal(raw.toString().replace(",", "").trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                 String indicatorCode,
                                                 String referenceText,
                                                 BigDecimal value,
                                                 LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(RealEstateMarketCategory.RENT)
                .source(RealEstateMarketSource.MOLIT)
                .indicatorCode(indicatorCode)
                .referenceText(referenceText)
                .value(value)
                .sourceUrl("https://www.data.go.kr/data/15126474/openapi.do")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }
}