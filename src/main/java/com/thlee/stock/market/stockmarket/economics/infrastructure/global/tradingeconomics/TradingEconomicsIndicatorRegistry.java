package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.config.TradingEconomicsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradingEconomicsIndicatorRegistry {

    private final TradingEconomicsProperties properties;

    /**
     * 지표 타입 → 전체 URL 생성
     * 예: CORE_CONSUMER_PRICES → https://ko.tradingeconomics.com/country-list/core-consumer-prices?continent=g20
     */
    public String getUrl(GlobalEconomicIndicatorType type) {
        return properties.getBaseUrl()
            + "/country-list/" + type.getPathSegment()
            + "?continent=g20";
    }
}