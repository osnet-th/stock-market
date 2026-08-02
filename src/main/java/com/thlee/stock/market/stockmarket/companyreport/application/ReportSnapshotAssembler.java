package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;

/**
 * 국가별 리포트 스냅샷 조립 전략.
 * 종목코드 형태로 국가를 판별한다 — 6자리 숫자는 KR(DART), 영문 티커는 US(SEC).
 */
public interface ReportSnapshotAssembler {

    boolean supports(String stockCode);

    ReportSnapshot assemble(String stockCode);
}
