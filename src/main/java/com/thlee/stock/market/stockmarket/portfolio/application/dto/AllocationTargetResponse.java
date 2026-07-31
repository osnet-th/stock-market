package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.AllocationTarget;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class AllocationTargetResponse {

    private final BigDecimal safeRatio;
    private final BigDecimal investRatio;
    private final BigDecimal bandPctPoint;
    private final List<AssetRatioResponse> investAssets;

    @Getter
    @RequiredArgsConstructor
    public static class AssetRatioResponse {
        private final String assetType;
        private final String assetTypeName;
        private final BigDecimal targetRatio;
    }

    public static AllocationTargetResponse from(AllocationTarget target) {
        List<AssetRatioResponse> assets = target.getInvestAssetRatios().entrySet().stream()
                .map(entry -> new AssetRatioResponse(
                        entry.getKey().name(),
                        entry.getKey().getDescription(),
                        entry.getValue()))
                .toList();
        return new AllocationTargetResponse(
                target.getSafeRatio(),
                target.getInvestRatio(),
                target.getBandPctPoint(),
                assets
        );
    }
}
