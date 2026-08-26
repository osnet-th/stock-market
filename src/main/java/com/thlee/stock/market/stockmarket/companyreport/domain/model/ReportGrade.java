package com.thlee.stock.market.stockmarket.companyreport.domain.model;

/**
 * 투자판단 5단계 등급. A가 하나라도 있으면 매수재료로 본다.
 */
public enum ReportGrade {
    A, B, C, D, E;

    public static ReportGrade fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return valueOf(value.trim().toUpperCase());
    }
}
