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

    // === 다중회사 ===

    List<FinancialAccount> getMultiFinancialAccounts(List<String> stockCodes, String year, String reportCode);

    List<FinancialIndex> getMultiFinancialIndices(List<String> stockCodes, String year, String reportCode, String indexClassCode);
}