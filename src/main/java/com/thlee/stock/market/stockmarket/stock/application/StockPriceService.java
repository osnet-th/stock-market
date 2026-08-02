package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.BulkStockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.PriceHistoryResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.CachedStockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ChartPeriod;
import com.thlee.stock.market.stockmarket.stock.domain.model.DailyPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.service.ExchangeRatePort;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPort;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import com.thlee.stock.market.stockmarket.stock.presentation.dto.BulkStockPriceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final StockPricePort stockPricePort;
    private final ExchangeRatePort exchangeRatePort;
    private final StockPort stockPort;

    public StockPriceResponse getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        CachedStockPrice cached = stockPricePort.getPriceWithCacheInfo(stockCode, marketType, exchangeCode);
        String currency = exchangeCode.getCurrency();
        BigDecimal exchangeRate = exchangeRatePort.getRate(currency);
        return StockPriceResponse.from(cached.stockPrice(), currency, exchangeRate, cached.cachedAt());
    }

    public BulkStockPriceResponse getPrices(List<BulkStockPriceRequest.StockPriceItem> stocks) {
        Map<String, StockPriceResponse> prices = new LinkedHashMap<>();

        List<BulkStockPriceRequest.StockPriceItem> domesticStocks = new ArrayList<>();
        List<BulkStockPriceRequest.StockPriceItem> overseasStocks = new ArrayList<>();

        for (BulkStockPriceRequest.StockPriceItem item : stocks) {
            if (item.getMarketType().isDomestic()) {
                domesticStocks.add(item);
            } else {
                overseasStocks.add(item);
            }
        }

        // 국내 주식: 멀티종목 API로 일괄 조회
        if (!domesticStocks.isEmpty()) {
            List<String> stockCodes = domesticStocks.stream()
                .map(BulkStockPriceRequest.StockPriceItem::getStockCode)
                .toList();
            Map<String, CachedStockPrice> domesticPrices = stockPricePort.getDomesticPricesWithCacheInfo(stockCodes);
            BigDecimal krwRate = exchangeRatePort.getRate("KRW");

            for (BulkStockPriceRequest.StockPriceItem item : domesticStocks) {
                CachedStockPrice cached = domesticPrices.get(item.getStockCode());
                if (cached != null) {
                    prices.put(item.getStockCode(),
                        StockPriceResponse.from(cached.stockPrice(), "KRW", krwRate, cached.cachedAt()));
                } else {
                    prices.put(item.getStockCode(), null);
                }
            }
        }

        // 해외 주식: 기존 개별 조회
        for (BulkStockPriceRequest.StockPriceItem item : overseasStocks) {
            try {
                StockPriceResponse response = getPrice(
                        item.getStockCode(),
                        item.getMarketType(),
                        item.getExchangeCode()
                );
                prices.put(item.getStockCode(), response);
            } catch (Exception e) {
                prices.put(item.getStockCode(), null);
            }
        }

        return new BulkStockPriceResponse(prices);
    }

    /** 상장일 조회 없이 전구간을 커버하기 위한 기본 시작일 — KIS는 실데이터 존재 구간만 반환한다. */
    private static final LocalDate PRICE_HISTORY_DEFAULT_FROM = LocalDate.of(1960, 1, 1);

    /**
     * 기간별(일/주/월봉) 가격 히스토리 조회. 국내 6자리 코드와 미국 영문 티커를 지원한다.
     *
     * @param period 봉 주기
     * @param from   시작일 (null이면 1960-01-01 — 상장 이후 전구간)
     * @param to     종료일 (null이면 오늘)
     */
    public PriceHistoryResponse getPriceHistory(String stockCode, ChartPeriod period,
                                                LocalDate from, LocalDate to) {
        LocalDate effectiveFrom = from != null ? from : PRICE_HISTORY_DEFAULT_FROM;
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        Listing listing = resolveListing(stockCode);
        List<DailyPrice> prices = stockPricePort.getPriceHistory(
            stockCode, listing.marketType(), listing.exchangeCode(), period, effectiveFrom, effectiveTo);
        return PriceHistoryResponse.from(stockCode, period, prices);
    }

    /**
     * 종목코드 형태로 시장 해석 — 6자리 숫자는 국내(기간별시세 API는 시장구분 "J" 공통이라 KOSPI/KRX로 충분),
     * 티커는 종목 마스터에서 거래소를 찾고 미존재 시 나스닥 가정 (오판 시 KIS 조회 실패 → 빈 리스트 degrade).
     */
    private Listing resolveListing(String stockCode) {
        if (stockCode.matches("\\d{6}")) {
            return new Listing(MarketType.KOSPI, ExchangeCode.KRX);
        }
        return stockPort.findByCode(stockCode.toUpperCase())
            .filter(stock -> stock.marketType() != null && !stock.marketType().isDomestic())
            .map(stock -> new Listing(stock.marketType(), stock.exchangeCode()))
            .orElse(new Listing(MarketType.NASDAQ, ExchangeCode.NAS));
    }

    private record Listing(MarketType marketType, ExchangeCode exchangeCode) {}
}
