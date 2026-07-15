package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.BulkHoldingReport;
import com.thlee.stock.market.stockmarket.stock.domain.model.CompanyProfile;
import com.thlee.stock.market.stockmarket.stock.domain.model.MajorShareholder;

import java.util.List;
import java.util.Optional;

/**
 * DART 기업개황/지분공시 조회 포트
 */
public interface CompanyDisclosurePort {

    Optional<CompanyProfile> getCompanyProfile(String stockCode);

    /**
     * 최대주주 및 특수관계인 소유 현황 (사업연도 단위)
     */
    List<MajorShareholder> getMajorShareholders(String stockCode, String year, String reportCode);

    /**
     * 대량보유 상황보고 목록 (DART가 반환하는 최근 보고분, 실측 약 2년치)
     */
    List<BulkHoldingReport> getBulkHoldingReports(String stockCode);
}
