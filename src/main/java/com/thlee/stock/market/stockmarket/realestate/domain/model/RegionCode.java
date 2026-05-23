package com.thlee.stock.market.stockmarket.realestate.domain.model;

import java.util.Objects;

/**
 * 행정안전부 시군구 코드 (5자리) Value Object.
 * <p>
 * 예) 서울 강동구 = "11740", 경기 성남시 분당구 = "41135"
 */
public final class RegionCode {

    public static final String SEOUL_PREFIX = "11";
    public static final String GG_PREFIX = "41";

    private final String value;

    private RegionCode(String value) {
        this.value = value;
    }

    public static RegionCode of(String value) {
        if (value == null || value.length() != 5) {
            throw new IllegalArgumentException("RegionCode must be 5 digits: " + value);
        }
        return new RegionCode(value);
    }

    public String value() {
        return value;
    }

    public String sidoCode() {
        return value.substring(0, 2);
    }

    public boolean isSeoul() {
        return value.startsWith(SEOUL_PREFIX);
    }

    public boolean isGyeonggi() {
        return value.startsWith(GG_PREFIX);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegionCode that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}