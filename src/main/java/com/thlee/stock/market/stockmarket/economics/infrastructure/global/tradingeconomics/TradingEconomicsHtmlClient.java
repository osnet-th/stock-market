package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config.TradingEconomicsProperties;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsFetchException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TradingEconomicsHtmlClient {

    private final TradingEconomicsProperties properties;

    public Document fetch(String url) {
        try {
            return Jsoup.connect(url)
                .userAgent(properties.getUserAgent())
                .timeout(properties.getTimeout())
                .get();
        } catch (IOException e) {
            throw new TradingEconomicsFetchException(
                "HTML 수집 실패: url=" + url + ", cause=" + e.getMessage(), e);
        }
    }
}