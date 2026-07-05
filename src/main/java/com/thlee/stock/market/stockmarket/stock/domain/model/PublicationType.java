package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * DART 공시유형 (list.json pblntf_ty)
 */
@Getter
@RequiredArgsConstructor
public enum PublicationType {

    A("A", "정기공시"),
    B("B", "주요사항보고"),
    C("C", "발행공시"),
    D("D", "지분공시"),
    E("E", "기타공시"),
    F("F", "외부감사관련"),
    G("G", "펀드공시"),
    H("H", "자산유동화"),
    I("I", "거래소공시"),
    J("J", "공정위공시");

    private final String code;
    private final String label;
}
