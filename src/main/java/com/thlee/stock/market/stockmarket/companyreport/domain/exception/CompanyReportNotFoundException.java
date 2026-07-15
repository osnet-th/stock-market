package com.thlee.stock.market.stockmarket.companyreport.domain.exception;

public class CompanyReportNotFoundException extends RuntimeException {

    public CompanyReportNotFoundException(Long id) {
        super("기업분석리포트를 찾을 수 없습니다: " + id);
    }
}
