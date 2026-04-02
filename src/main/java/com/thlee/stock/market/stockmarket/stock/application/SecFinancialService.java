package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.SecFinancialStatementResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.SecInvestmentMetricResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecInvestmentMetric;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.service.SecFinancialPort;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecFinancialService {

    private final SecFinancialPort secFinancialPort;
    private final StockPricePort stockPricePort;

    public List<SecFinancialStatementResponse> getFinancialStatements(String ticker) {
        return secFinancialPort.getFinancialStatements(ticker).stream()
                .map(SecFinancialStatementResponse::from)
                .toList();
    }

    public List<SecFinancialStatementResponse> getQuarterlyFinancialStatements(String ticker) {
        return secFinancialPort.getQuarterlyFinancialStatements(ticker).stream()
                .map(SecFinancialStatementResponse::from)
                .toList();
    }

    public Long getCik(String ticker) {
        return secFinancialPort.getCik(ticker);
    }

    public List<SecInvestmentMetricResponse> getInvestmentMetrics(String ticker) {
        List<SecInvestmentMetric> metrics = secFinancialPort.getInvestmentMetrics(ticker);
        List<SecInvestmentMetricResponse> responses = new ArrayList<>(
                metrics.stream().map(SecInvestmentMetricResponse::from).toList()
        );

        // PER 계산: 현재가 / EPS (Application Service에서 cross-port 조합)
        Double per = calculatePer(ticker, metrics);
        responses.add(new SecInvestmentMetricResponse("PER", per, "x",
                "주가수익비율 (Price to Earnings Ratio)"));

        return responses;
    }

    private Double calculatePer(String ticker, List<SecInvestmentMetric> metrics) {
        Double epsBasic = metrics.stream()
                .filter(m -> "EPS (기본)".equals(m.name()))
                .map(SecInvestmentMetric::value)
                .findFirst()
                .orElse(null);

        if (epsBasic == null || epsBasic <= 0) {
            return null;
        }

        try {
            StockPrice price = stockPricePort.getPrice(ticker, MarketType.NASDAQ, ExchangeCode.NAS);
            double currentPrice = Double.parseDouble(price.currentPrice());
            return Math.round(currentPrice / epsBasic * 100.0) / 100.0;
        } catch (Exception e) {
            log.warn("PER 계산을 위한 현재가 조회 실패 (ticker: {}): {}", ticker, e.getMessage());
            return null;
        }
    }
}