package com.thlee.stock.market.stockmarket.realestate.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 행정안전부 시도(2자리) 또는 시군구(5자리) 코드 Value Object.
 * <p>
 * 예) 서울특별시 = "11", 서울 강동구 = "11740", 경기 성남시 분당구 = "41135"
 */
public final class RegionCode {

    public static final String SEOUL_PREFIX = "11";
    public static final String GG_PREFIX = "41";

    private static final Pattern VALUE_PATTERN = Pattern.compile("^(\\d{2}|\\d{5})$");

    private final String value;

    private RegionCode(String value) {
        this.value = value;
    }

    public static RegionCode of(String value) {
        if (value == null || !VALUE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("RegionCode must be 2 or 5 digits: " + value);
        }
        return new RegionCode(value);
    }

    public String value() {
        return value;
    }

    public boolean isSido() {
        return value.length() == 2;
    }

    public boolean isSigungu() {
        return value.length() == 5;
    }

    /**
     * 시도(앞 2자리) 코드 반환. SIDO 코드는 자기 자신 반환.
     */
    public String sidoCode() {
        return isSido() ? value : value.substring(0, 2);
    }

    public RegionLevel level() {
        return RegionLevel.fromCode(value);
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