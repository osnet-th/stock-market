package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "economics.api.global.tradingeconomics")
public class TradingEconomicsProperties {
    private String baseUrl;
    private int timeout = 5000;
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
}