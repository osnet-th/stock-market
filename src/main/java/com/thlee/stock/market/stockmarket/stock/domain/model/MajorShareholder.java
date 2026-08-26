package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 최대주주 및 특수관계인 주식소유 현황 (사업연도 단위)
 */
public record MajorShareholder(
        String bsnsYear,
        String stockKind,
        String name,
        String relation,
        String baseQuantity,
        String baseRatio,
        String termEndQuantity,
        String termEndRatio,
        String remark,
        String settlementDate
) {}
