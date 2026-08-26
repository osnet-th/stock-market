package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.Disclosure;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공시 목록 항목 응답. rcept_no → DART 뷰어 URL 포함.
 */
@Getter
@RequiredArgsConstructor
public class DisclosureResponse {

    private static final String VIEWER_URL_PREFIX = "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=";

    private final String receiptDate;
    private final String reportName;
    private final String submitterName;
    private final String remark;
    private final String viewerUrl;

    public static DisclosureResponse from(Disclosure disclosure) {
        return new DisclosureResponse(
                disclosure.receiptDate(),
                disclosure.reportName(),
                disclosure.submitterName(),
                disclosure.remark(),
                VIEWER_URL_PREFIX + disclosure.receiptNo());
    }
}
