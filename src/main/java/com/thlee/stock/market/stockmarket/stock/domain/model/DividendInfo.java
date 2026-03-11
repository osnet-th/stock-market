package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 배당에 관한 사항
 */
public record DividendInfo(
        String category,
        String stockKind,
        String currentTerm,
        String previousTerm,
        String beforePreviousTerm
) {}