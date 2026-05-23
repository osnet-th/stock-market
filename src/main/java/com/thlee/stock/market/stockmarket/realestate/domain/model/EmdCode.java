package com.thlee.stock.market.stockmarket.realestate.domain.model;

import java.util.Objects;

/**
 * 법정동 코드 (8자리) Value Object.
 * <p>
 * 시군구 단위 즐겨찾기/조회는 EmdCode 없이 처리된다.
 * 빈 문자열 sentinel은 infrastructure 레이어에서만 다루며 도메인은 Optional로 노출.
 */
public final class EmdCode {

    private final String value;

    private EmdCode(String value) {
        this.value = value;
    }

    public static EmdCode of(String value) {
        if (value == null || value.length() != 8) {
            throw new IllegalArgumentException("EmdCode must be 8 digits: " + value);
        }
        return new EmdCode(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmdCode that)) return false;
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
