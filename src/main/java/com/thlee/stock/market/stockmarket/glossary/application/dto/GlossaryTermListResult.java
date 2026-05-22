package com.thlee.stock.market.stockmarket.glossary.application.dto;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;

import java.util.List;

/**
 * 용어 목록 조회 application 응답 value.
 * presentation 계층에서 {@code *Response} 로 다시 매핑된다.
 */
public record GlossaryTermListResult(
        List<GlossaryTerm> items,
        long totalCount,
        int page,
        int size
) {
}