package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.UsFiling;

/**
 * SEC 제출 서식 1건 응답 (US 리포트 공시 패널용)
 */
public record SecFilingResponse(
        String form,
        String filingDate,
        String accessionNumber,
        String primaryDocument,
        String description,
        String viewerUrl
) {

    public static SecFilingResponse from(UsFiling filing) {
        return new SecFilingResponse(
                filing.form(),
                filing.filingDate(),
                filing.accessionNumber(),
                filing.primaryDocument(),
                filing.description(),
                filing.viewerUrl());
    }
}
