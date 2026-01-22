package com.thlee.stock.market.stockmarket.user.domain.model;

import com.thlee.stock.market.stockmarket.user.domain.exception.InvalidUserArgumentException;

import java.util.Objects;

/**
 * 전화번호 Value Object
 */
public class PhoneNumber {
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 11;

    private final String value;

    public PhoneNumber(String value) {
        this.value = normalizeAndValidate(value);
    }

    private String normalizeAndValidate(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidUserArgumentException("전화번호는 필수입니다.");
        }

        // 하이픈 제거
        String normalized = value.replaceAll("-", "");

        // 숫자만 포함되어 있는지 확인
        if (!normalized.matches("\\d+")) {
            throw new InvalidUserArgumentException("전화번호는 숫자만 포함해야 합니다.");
        }

        if (normalized.length() < MIN_LENGTH) {
            throw new InvalidUserArgumentException("전화번호는 10자 이상이어야 합니다.");
        }

        if (normalized.length() > MAX_LENGTH) {
            throw new InvalidUserArgumentException("전화번호는 11자 이하여야 합니다.");
        }

        return normalized;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
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