package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.BulkHoldingReport;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BulkHoldingReportResponse {
    private final String receiptNo;
    private final String receiptDate;
    private final String reportType;
    private final String reporter;
    private final String holdingQuantity;
    private final String quantityChange;
    private final String holdingRatio;
    private final String ratioChange;
    private final String reportReason;

    public static BulkHoldingReportResponse from(BulkHoldingReport report) {
        return new BulkHoldingReportResponse(
                report.receiptNo(),
                report.receiptDate(),
                report.reportType(),
                report.reporter(),
                report.holdingQuantity(),
                report.quantityChange(),
                report.holdingRatio(),
                report.ratioChange(),
                report.reportReason()
        );
    }
}
