package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.*;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockFinancialPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockFinancialService {

    private final StockFinancialPort stockFinancialPort;

    public List<FinancialAccountResponse> getFinancialAccounts(String stockCode, String year, String reportCode) {
        return stockFinancialPort.getFinancialAccounts(stockCode, year, reportCode)
                .stream()
                .map(FinancialAccountResponse::from)
                .toList();
    }

    public List<FinancialIndexResponse> getFinancialIndices(String stockCode, String year, String reportCode, String indexClassCode) {
        return stockFinancialPort.getFinancialIndices(stockCode, year, reportCode, indexClassCode)
                .stream()
                .map(FinancialIndexResponse::from)
                .toList();
    }

    public List<FullFinancialStatementResponse> getFullFinancialStatements(String stockCode, String year, String reportCode, String fsDiv) {
        return stockFinancialPort.getFullFinancialStatements(stockCode, year, reportCode, fsDiv)
                .stream()
                .map(FullFinancialStatementResponse::from)
                .toList();
    }

    public List<StockQuantityResponse> getStockQuantities(String stockCode, String year, String reportCode) {
        return stockFinancialPort.getStockQuantities(stockCode, year, reportCode)
                .stream()
                .map(StockQuantityResponse::from)
                .toList();
    }

    public List<DividendInfoResponse> getDividendInfos(String stockCode, String year, String reportCode) {
        return stockFinancialPort.getDividendInfos(stockCode, year, reportCode)
                .stream()
                .map(DividendInfoResponse::from)
                .toList();
    }

    public List<LawsuitResponse> getLawsuits(String stockCode, String startDate, String endDate) {
        return stockFinancialPort.getLawsuits(stockCode, startDate, endDate)
                .stream()
                .map(LawsuitResponse::from)
                .toList();
    }

    public List<FundUsageResponse> getPrivateFundUsages(String stockCode, String year, String reportCode) {
        return stockFinancialPort.getPrivateFundUsages(stockCode, year, reportCode)
                .stream()
                .map(FundUsageResponse::from)
                .toList();
    }

    public List<FundUsageResponse> getPublicFundUsages(String stockCode, String year, String reportCode) {
        return stockFinancialPort.getPublicFundUsages(stockCode, year, reportCode)
                .stream()
                .map(FundUsageResponse::from)
                .toList();
    }

    // === 다중회사 ===

    public List<FinancialAccountResponse> getMultiFinancialAccounts(List<String> stockCodes, String year, String reportCode) {
        return stockFinancialPort.getMultiFinancialAccounts(stockCodes, year, reportCode)
                .stream()
                .map(FinancialAccountResponse::from)
                .toList();
    }

    public List<FinancialIndexResponse> getMultiFinancialIndices(List<String> stockCodes, String year, String reportCode, String indexClassCode) {
        return stockFinancialPort.getMultiFinancialIndices(stockCodes, year, reportCode, indexClassCode)
                .stream()
                .map(FinancialIndexResponse::from)
                .toList();
    }
}