package com.thlee.stock.market.stockmarket.realestate.domain.model;

/**
 * Region 단위 구분.
 * <p>
 * SIDO: 광역시도 단위 (region_code 2자리, 예: "11" = 서울특별시)
 * SIGUNGU: 시군구 단위 (region_code 5자리, 예: "11680" = 서울 강남구)
 * <p>
 * Single source of truth는 {@code region_code} length. 본 enum은 query/dispatch 편의용.
 */
public enum RegionLevel {

    SIDO,
    SIGUNGU;

    /**
     * region_code 길이 기반 derive.
     *
     * @throws IllegalArgumentException null 또는 길이가 2/5가 아닌 경우
     */
    public static RegionLevel fromCode(String regionCode) {
        if (regionCode == null || (regionCode.length() != 2 && regionCode.length() != 5)) {
            throw new IllegalArgumentException("Invalid region code length: " + regionCode);
        }
        return regionCode.length() == 2 ? SIDO : SIGUNGU;
    }
}