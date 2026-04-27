package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;

import java.math.BigDecimal;

/**
 * 매도 이력 사후 수정 파라미터.
 * quantity / salePrice / reason / memo만 변경 가능 (soldAt·fxRate·totalAssetAtSale은 스냅샷 유지).
 */
public record UpdateSaleParam(
        int quantity,
        BigDecimal salePrice,
        SaleReason reason,
        String memo
) {
}