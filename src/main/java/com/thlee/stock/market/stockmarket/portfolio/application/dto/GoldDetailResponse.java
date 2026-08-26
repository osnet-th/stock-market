package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.GoldDetail;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class GoldDetailResponse {

    private final BigDecimal quantityGrams;

    public static GoldDetailResponse from(GoldDetail detail) {
        return new GoldDetailResponse(detail.getQuantityGrams());
    }
}
