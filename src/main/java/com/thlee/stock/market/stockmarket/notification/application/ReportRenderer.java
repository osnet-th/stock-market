package com.thlee.stock.market.stockmarket.notification.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;

import java.time.LocalDate;

/**
 * 리포트 렌더링 포트 인터페이스
 */
public interface ReportRenderer {
    String renderReport(PortfolioEvaluation evaluation, LocalDate date);
}