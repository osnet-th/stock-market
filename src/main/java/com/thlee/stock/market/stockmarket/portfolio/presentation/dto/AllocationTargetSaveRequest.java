package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class AllocationTargetSaveRequest {

    private BigDecimal safeRatio;
    private BigDecimal investRatio;
    private BigDecimal bandPctPoint;
    private List<AssetRatio> investAssets;

    @Getter
    public static class AssetRatio {
        private String assetType;
        private BigDecimal targetRatio;
    }
}
