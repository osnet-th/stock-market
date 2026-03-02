package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorValue;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto.RawTableRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class TradingEconomicsValueNormalizer {

    public List<CountryIndicatorSnapshot> normalize(
            GlobalEconomicIndicatorType indicatorType,
            List<RawTableRow> rows) {

        return rows.stream()
            .map(row -> toSnapshot(indicatorType, row))
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<CountryIndicatorSnapshot> toSnapshot(
            GlobalEconomicIndicatorType indicatorType,
            RawTableRow row) {
        try {
            return Optional.of(CountryIndicatorSnapshot.builder()
                .countryName(row.country())
                .indicatorType(indicatorType)
                .lastValue(toIndicatorValue(row.last(), row.unit()))
                .previousValue(toIndicatorValue(row.previous(), row.unit()))
                .referenceText(row.reference())
                .collectedAt(LocalDateTime.now())
                .build());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private IndicatorValue toIndicatorValue(String rawText, String unit) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        return new IndicatorValue(rawText, parseNumeric(rawText), unit);
    }

    private BigDecimal parseNumeric(String text) {
        try {
            String cleaned = text.replaceAll("[,%\\s]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}