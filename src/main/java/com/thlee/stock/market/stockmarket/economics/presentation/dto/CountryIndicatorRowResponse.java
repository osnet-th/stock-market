package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorValue;

import java.math.BigDecimal;

public record CountryIndicatorRowResponse(
    String countryName,
    BigDecimal lastValue,
    BigDecimal previousValue,
    String reference,
    String unit
) {

    public static CountryIndicatorRowResponse from(CountryIndicatorSnapshot snapshot) {
        return new CountryIndicatorRowResponse(
            snapshot.getCountryName(),
            toNumeric(snapshot.getLastValue()),
            toNumeric(snapshot.getPreviousValue()),
            snapshot.getReferenceText(),
            toUnit(snapshot.getLastValue())
        );
    }

    private static BigDecimal toNumeric(IndicatorValue value) {
        return value != null ? value.getNumericValue() : null;
    }

    private static String toUnit(IndicatorValue value) {
        return value != null ? value.getUnit() : null;
    }
}