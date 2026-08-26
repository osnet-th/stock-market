package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.MajorShareholder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MajorShareholderResponse {
    private final String bsnsYear;
    private final String stockKind;
    private final String name;
    private final String relation;
    private final String baseQuantity;
    private final String baseRatio;
    private final String termEndQuantity;
    private final String termEndRatio;
    private final String remark;
    private final String settlementDate;

    public static MajorShareholderResponse from(MajorShareholder shareholder) {
        return new MajorShareholderResponse(
                shareholder.bsnsYear(),
                shareholder.stockKind(),
                shareholder.name(),
                shareholder.relation(),
                shareholder.baseQuantity(),
                shareholder.baseRatio(),
                shareholder.termEndQuantity(),
                shareholder.termEndRatio(),
                shareholder.remark(),
                shareholder.settlementDate()
        );
    }
}
