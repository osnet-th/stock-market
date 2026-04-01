package com.thlee.stock.market.stockmarket.stock.domain.model;

import java.util.Map;

/**
 * SEC 재무제표 개별 항목
 * @param label 한글명
 * @param labelEn 영문명
 * @param values 연도별 금액 (fiscalYear → amount), null 가능
 */
public record SecFinancialItem(
        String label,
        String labelEn,
        Map<Integer, Long> values
) {}