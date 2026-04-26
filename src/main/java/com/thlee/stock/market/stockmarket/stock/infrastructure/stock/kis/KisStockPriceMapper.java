package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.DailyPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDailyChartItem;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticMultiPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisDomesticPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOverseasPriceOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisOvertimePriceOutput;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KisStockPriceMapper {

    private static final DateTimeFormatter KIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * KIS 일봉 응답 항목을 도메인 {@link DailyPrice} 로 변환. 날짜 오름차순 정렬을 보장한다.
     * 비어있거나 날짜 파싱 실패 항목은 건너뛴다 (장애 격리).
     */
    public static List<DailyPrice> fromDailyChart(List<KisDailyChartItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(KisStockPriceMapper::toDailyPrice)
                .filter(p -> p != null)
                .sorted(Comparator.comparing(DailyPrice::date))
                .toList();
    }

    private static DailyPrice toDailyPrice(KisDailyChartItem item) {
        if (item == null || isBlank(item.getBusinessDate()) || isBlank(item.getClosePrice())) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(item.getBusinessDate(), KIS_DATE_FORMAT);
            return new DailyPrice(
                    date,
                    parseDecimal(item.getOpenPrice()),
                    parseDecimal(item.getHighPrice()),
                    parseDecimal(item.getLowPrice()),
                    parseDecimal(item.getClosePrice()),
                    parseLong(item.getVolume())
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            return new BigDecimal(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String s) {
        if (isBlank(s)) {
            return null;
        }
        try {
            return Long.parseLong(s.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

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