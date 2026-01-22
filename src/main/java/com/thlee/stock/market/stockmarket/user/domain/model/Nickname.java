package com.thlee.stock.market.stockmarket.user.domain.model;

import com.thlee.stock.market.stockmarket.user.domain.exception.InvalidUserArgumentException;

import java.util.Objects;

/**
 * 닉네임 Value Object
 */
public class Nickname {
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 20;

    private final String value;

    public Nickname(String value) {
        validate(value);
        this.value = value;
    }

    private void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidUserArgumentException("닉네임은 필수입니다.");
        }

        if (value.length() < MIN_LENGTH) {
            throw new InvalidUserArgumentException("닉네임은 2자 이상이어야 합니다.");
        }

        if (value.length() > MAX_LENGTH) {
            throw new InvalidUserArgumentException("닉네임은 20자 이하여야 합니다.");
        }

        if (!value.matches("^[a-zA-Z가-힣0-9]+$")) {
            throw new InvalidUserArgumentException("닉네임은 영어, 한글, 숫자만 사용할 수 있습니다.");
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nickname nickname = (Nickname) o;
        return Objects.equals(value, nickname.value);
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