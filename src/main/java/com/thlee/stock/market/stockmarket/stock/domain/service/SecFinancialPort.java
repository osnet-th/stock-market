package com.thlee.stock.market.stockmarket.stock.domain.service;

import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialStatement;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecInvestmentMetric;

import java.util.List;

/**
 * SEC 재무정보 조회 포트
 */
public interface SecFinancialPort {

    /**
     * 재무제표 3종 (손익계산서, 재무상태표, 현금흐름표) 조회
     */
    List<SecFinancialStatement> getFinancialStatements(String ticker);

    /**
     * 분기 재무제표 3종 (10-Q 기반, 최근 8분기)
     */
    List<SecFinancialStatement> getQuarterlyFinancialStatements(String ticker);

    /**
     * 투자 지표 조회 (EPS, ROE, 부채비율, 영업이익률 — PER 제외)
     */
    List<SecInvestmentMetric> getInvestmentMetrics(String ticker);
}