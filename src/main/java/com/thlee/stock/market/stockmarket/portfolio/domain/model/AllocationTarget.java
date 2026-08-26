package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 사용자별 목표 자산 배분 설정 (사용자당 1건)
 * - 상위: 안전자산/투자자산 목표 비율 (합 100)
 * - 하위: 투자자산 내부 자산군별 목표 비율 (합 100, 선택)
 * - 허용밴드(%p): 편차 경고 강조 기준
 */
@Getter
public class AllocationTarget {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal BAND_MAX = new BigDecimal("20");

    private Long id;
    private Long userId;
    private BigDecimal safeRatio;
    private BigDecimal investRatio;
    private BigDecimal bandPctPoint;
    private Map<AssetType, BigDecimal> investAssetRatios;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 재구성용 생성자 (Repository에서 조회 시 사용)
     */
    public AllocationTarget(Long id,
                            Long userId,
                            BigDecimal safeRatio,
                            BigDecimal investRatio,
                            BigDecimal bandPctPoint,
                            Map<AssetType, BigDecimal> investAssetRatios,
                            LocalDateTime createdAt,
                            LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.safeRatio = safeRatio;
        this.investRatio = investRatio;
        this.bandPctPoint = bandPctPoint;
        this.investAssetRatios = toUnmodifiable(investAssetRatios);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AllocationTarget create(Long userId,
                                          BigDecimal safeRatio,
                                          BigDecimal investRatio,
                                          BigDecimal bandPctPoint,
                                          Map<AssetType, BigDecimal> investAssetRatios) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        validateRatios(safeRatio, investRatio, bandPctPoint, investAssetRatios);
        AllocationTarget target = new AllocationTarget(
                null, userId, safeRatio, investRatio, bandPctPoint,
                investAssetRatios, LocalDateTime.now(), LocalDateTime.now());
        return target;
    }

    public void update(BigDecimal safeRatio,
                       BigDecimal investRatio,
                       BigDecimal bandPctPoint,
                       Map<AssetType, BigDecimal> investAssetRatios) {
        validateRatios(safeRatio, investRatio, bandPctPoint, investAssetRatios);
        this.safeRatio = safeRatio;
        this.investRatio = investRatio;
        this.bandPctPoint = bandPctPoint;
        this.investAssetRatios = toUnmodifiable(investAssetRatios);
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateRatios(BigDecimal safeRatio,
                                       BigDecimal investRatio,
                                       BigDecimal bandPctPoint,
                                       Map<AssetType, BigDecimal> investAssetRatios) {
        validatePercent(safeRatio, "안전자산 목표 비율");
        validatePercent(investRatio, "투자자산 목표 비율");
        if (safeRatio.add(investRatio).compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("안전자산과 투자자산 목표 비율의 합은 100이어야 합니다.");
        }
        if (bandPctPoint == null || bandPctPoint.compareTo(BigDecimal.ZERO) < 0
                || bandPctPoint.compareTo(BAND_MAX) > 0) {
            throw new IllegalArgumentException("허용밴드는 0~20%p 사이여야 합니다.");
        }
        validateInvestAssetRatios(investAssetRatios);
    }

    private static void validateInvestAssetRatios(Map<AssetType, BigDecimal> ratios) {
        if (ratios == null || ratios.isEmpty()) {
            return;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<AssetType, BigDecimal> entry : ratios.entrySet()) {
            AssetType type = entry.getKey();
            if (AssetClassification.isExcluded(type)) {
                throw new IllegalArgumentException("배분 대상이 아닌 자산 유형입니다: " + type.getDescription());
            }
            if (AssetClassification.isSafe(type)) {
                throw new IllegalArgumentException("안전자산 유형은 투자자산 세부 비율에 설정할 수 없습니다: " + type.getDescription());
            }
            validatePercent(entry.getValue(), type.getDescription() + " 목표 비율");
            sum = sum.add(entry.getValue());
        }
        if (sum.compareTo(HUNDRED) != 0) {
            throw new IllegalArgumentException("투자자산 세부 목표 비율의 합은 100이어야 합니다.");
        }
    }

    private static void validatePercent(BigDecimal value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "은(는) 필수입니다.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException(name + "은(는) 0~100 사이여야 합니다.");
        }
    }

    private static Map<AssetType, BigDecimal> toUnmodifiable(Map<AssetType, BigDecimal> ratios) {
        if (ratios == null) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(ratios));
    }
}
