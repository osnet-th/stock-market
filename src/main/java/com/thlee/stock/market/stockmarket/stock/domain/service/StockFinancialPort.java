package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.*;

import java.util.List;

/**
 * DART 재무정보 조회 포트
 */
public interface StockFinancialPort {

    // === 단일회사 ===

    List<FinancialAccount> getFinancialAccounts(String stockCode, String year, String reportCode);

    List<FinancialIndex> getFinancialIndices(String stockCode, String year, String reportCode, String indexClassCode);

    List<FullFinancialStatement> getFullFinancialStatements(String stockCode, String year, String reportCode, String fsDiv);

    List<StockQuantity> getStockQuantities(String stockCode, String year, String reportCode);

    List<DividendInfo> getDividendInfos(String stockCode, String year, String reportCode);

    List<Lawsuit> getLawsuits(String stockCode, String startDate, String endDate);

    List<FundUsage> getPrivateFundUsages(String stockCode, String year, String reportCode);

    List<FundUsage> getPublicFundUsages(String stockCode, String year, String reportCode);

    /**
     * 공시 목록 조회 (접수일 최신순, 최대 100건)
     * @param publicationType 공시유형 (A:정기공시 ~ J:공정위공시, null이면 전체)
     */
    List<Disclosure> getDisclosures(String stockCode, String startDate, String endDate, String publicationType);

    // === 다중회사 ===

    List<FinancialAccount> getMultiFinancialAccounts(List<String> stockCodes, String year, String reportCode);

    List<FinancialIndex> getMultiFinancialIndices(List<String> stockCodes, String year, String reportCode, String indexClassCode);
}