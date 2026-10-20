package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.mcp.application.dto.McpHoldings;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioService;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockDetailResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 보유 종목 + 현재가 조합. 현재가는 종목마다 기존 시세 조회(캐시)를 쓰고, 실패한 종목은 평가액을 비운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class McpPortfolioQueryService {

    private final PortfolioService portfolioService;
    private final StockPriceService stockPriceService;

    public McpHoldings holdings(Long userId) {
        List<PortfolioItemResponse> stocks = portfolioService.getItems(userId).stream()
                .filter(item -> AssetType.STOCK.name().equals(item.getAssetType()))
                .filter(item -> item.getStockDetail() != null)
                .filter(item -> !PortfolioItemStatus.CLOSED.name().equals(item.getStatus()))
                .toList();
        Map<String, McpHoldingsMapper.Quote> quotes = new HashMap<>();
        for (PortfolioItemResponse item : stocks) {
            StockDetailResponse d = item.getStockDetail();
            if (!quotes.containsKey(d.getStockCode())) {
                quotes.put(d.getStockCode(), quote(d));
            }
        }
        return McpHoldingsMapper.toHoldings(stocks, quotes);
    }

    private McpHoldingsMapper.Quote quote(StockDetailResponse d) {
        try {
            StockPriceResponse p = stockPriceService.getPrice(d.getStockCode(),
                    MarketType.valueOf(d.getMarket()), ExchangeCode.valueOf(d.getExchangeCode()));
            return new McpHoldingsMapper.Quote(p.getCurrentPrice(), p.getCurrency(), p.getCurrentPriceKrw(),
                    p.getChangeRate(), p.getCachedAt());
        } catch (RuntimeException e) {
            log.warn("MCP 보유 종목 현재가 조회 실패 [{}]: {}", d.getStockCode(), e.getMessage());
            return null;
        }
    }
}
