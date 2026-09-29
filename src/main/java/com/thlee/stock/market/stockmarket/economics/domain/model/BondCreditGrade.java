package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 회사채 신용등급 (AAA~BBB-). 등급 선택은 사용자 몫이며 종목 신용등급 판정이 아니다.
 */
@Getter
@RequiredArgsConstructor
public enum BondCreditGrade {
    AAA("AAA"),
    AA_PLUS("AA+"),
    AA("AA"),
    AA_MINUS("AA-"),
    A_PLUS("A+"),
    A("A"),
    A_MINUS("A-"),
    BBB_PLUS("BBB+"),
    BBB("BBB"),
    BBB_MINUS("BBB-");

    /** 출처와 화면에서 쓰는 등급 표기 */
    private final String label;

    public static Optional<BondCreditGrade> fromLabel(String label) {
        if (label == null) {
            return Optional.empty();
        }
        String normalized = label.strip();
        return Arrays.stream(values())
                .filter(grade -> grade.label.equals(normalized))
                .findFirst();
    }
}
