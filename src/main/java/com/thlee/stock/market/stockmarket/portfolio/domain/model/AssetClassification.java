package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AllocationBucket;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;

import java.util.EnumSet;
import java.util.Set;

/**
 * 자산 배분용 안전/투자 고정 분류.
 * CASH·BOND·PENSION=안전, CRYPTO=배분 제외, 나머지=투자 (#102 확정 매핑, #110에서 PENSION 편입)
 */
public final class AssetClassification {

    private static final Set<AssetType> SAFE_TYPES =
            EnumSet.of(AssetType.CASH, AssetType.BOND, AssetType.PENSION);
    private static final Set<AssetType> EXCLUDED_TYPES = EnumSet.of(AssetType.CRYPTO);

    private AssetClassification() {
    }

    public static boolean isSafe(AssetType assetType) {
        return SAFE_TYPES.contains(assetType);
    }

    public static boolean isExcluded(AssetType assetType) {
        return EXCLUDED_TYPES.contains(assetType);
    }

    public static AllocationBucket classify(AssetType assetType) {
        if (isExcluded(assetType)) {
            throw new IllegalArgumentException("배분 대상이 아닌 자산 유형입니다: " + assetType);
        }
        return isSafe(assetType) ? AllocationBucket.SAFE : AllocationBucket.INVEST;
    }

    /**
     * 투자자산 자산군 목록 (배분 제외 유형 제외)
     */
    public static Set<AssetType> investTypes() {
        EnumSet<AssetType> types = EnumSet.allOf(AssetType.class);
        types.removeAll(SAFE_TYPES);
        types.removeAll(EXCLUDED_TYPES);
        return types;
    }
}
