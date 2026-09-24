package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.SrimInput;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record SrimInputData(
        @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal equity,
        String equityDate,
        @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal shares,
        String sharesDate,
        @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal requiredReturn,
        String currency,
        @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal referencePrice,
        String referencePriceDate,
        List<Year> years
) {
    public SrimInput toDomain() {
        if (years != null && (years.size() > 30 || years.stream().anyMatch(Objects::isNull)))
            throw new IllegalArgumentException("연도 행은 null 없이 최대 30개입니다.");
        return new SrimInput(equity, equityDate, shares, sharesDate, requiredReturn, currency,
                referencePrice, referencePriceDate, years == null ? List.of() : years.stream().map(Year::toDomain).toList());
    }

    public static SrimInputData from(SrimInput i) {
        return new SrimInputData(i.equity(), i.equityDate(), i.shares(), i.sharesDate(), i.requiredReturn(),
                i.currency(), i.referencePrice(), i.referencePriceDate(), i.years().stream().map(Year::from).toList());
    }

    public record Year(
            @JsonDeserialize(using = SrimYearDeserializer.class) Integer year,
            String mode,
            @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal directRoe,
            @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal previousEquity,
            @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal expectedEquity,
            @JsonSerialize(using = SrimDecimalSerializer.class) BigDecimal expectedIncome
    ) {
        public SrimInput.Year toDomain() {
            return new SrimInput.Year(year, mode, directRoe, previousEquity, expectedEquity, expectedIncome);
        }
        public static Year from(SrimInput.Year y) {
            return new Year(y.year(), y.mode(), y.directRoe(), y.previousEquity(), y.expectedEquity(), y.expectedIncome());
        }
    }
}
