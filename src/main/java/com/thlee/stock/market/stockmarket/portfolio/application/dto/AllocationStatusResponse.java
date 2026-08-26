package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 목표 자산 배분 현황 (평가액 기준, CRYPTO 제외)
 */
@Getter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AllocationStatusResponse {

    private final boolean configured;
    private final BigDecimal totalEvaluated;
    private final BigDecimal bandPctPoint;
    private final int excludedCryptoCount;
    private final List<BucketStatus> buckets;
    private final List<AssetStatus> investAssets;

    @Getter
    @RequiredArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BucketStatus {
        private final String bucket;
        private final String bucketName;
        private final BigDecimal currentAmount;
        private final BigDecimal currentRatio;
        private final BigDecimal targetRatio;
        private final BigDecimal targetAmount;
        private final BigDecimal deviationAmount;
        private final BigDecimal deviationPctPoint;
        private final Boolean bandExceeded;
    }

    @Getter
    @RequiredArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AssetStatus {
        private final String assetType;
        private final String assetTypeName;
        private final BigDecimal currentAmount;
        private final BigDecimal currentRatio;
        private final BigDecimal targetRatio;
        private final BigDecimal targetAmount;
        private final BigDecimal deviationAmount;
        private final BigDecimal deviationPctPoint;
        private final Boolean bandExceeded;
    }
}
