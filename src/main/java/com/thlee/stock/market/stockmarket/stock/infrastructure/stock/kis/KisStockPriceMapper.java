package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticMultiPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOverseasPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOvertimePriceOutput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KisStockPriceMapper {

    public static StockPrice fromDomestic(KisDomesticPriceOutput output,
                                          String stockCode,
                                          MarketType marketType,
                                          ExchangeCode exchangeCode) {
        return new StockPrice(
            stockCode,
            output.getCurrentPrice(),
            output.getPreviousClose(),
            output.getChange(),
            output.getChangeSign(),
            output.getChangeRate(),
            output.getVolume(),
            output.getTradingAmount(),
            output.getHighPrice(),
            output.getLowPrice(),
            output.getOpenPrice(),
            marketType,
            exchangeCode
        );
    }

    public static StockPrice fromDomesticMulti(KisDomesticMultiPriceOutput output) {
        return new StockPrice(
            output.getStockCode(),
            output.getCurrentPrice(),
            output.getPreviousClose(),
            output.getChange(),
            output.getChangeSign(),
            output.getChangeRate(),
            output.getVolume(),
            output.getTradingAmount(),
            output.getHighPrice(),
            output.getLowPrice(),
            output.getOpenPrice(),
            MarketType.KOSPI,
            ExchangeCode.KRX
        );
    }

    public static StockPrice fromOvertime(KisOvertimePriceOutput output,
                                          String stockCode,
                                          MarketType marketType,
                                          ExchangeCode exchangeCode) {
        return new StockPrice(
            stockCode,
            output.getCurrentPrice(),
            output.getPreviousClose(),
            output.getChange(),
            output.getChangeSign(),
            output.getChangeRate(),
            output.getVolume(),
            output.getTradingAmount(),
            output.getHighPrice(),
            output.getLowPrice(),
            output.getOpenPrice(),
            marketType,
            exchangeCode
        );
    }

    public static StockPrice fromOverseas(KisOverseasPriceOutput output,
                                          String stockCode,
                                          MarketType marketType,
                                          ExchangeCode exchangeCode) {
        return new StockPrice(
            stockCode,
            output.getCurrentPrice(),
            output.getPreviousClose(),
            output.getChange(),
            output.getChangeSign(),
            output.getChangeRate(),
            output.getVolume(),
            output.getTradingAmount(),
            null,
            null,
            null,
            marketType,
            exchangeCode
        );
    }
}