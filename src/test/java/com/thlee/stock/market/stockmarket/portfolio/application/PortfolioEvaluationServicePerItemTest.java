package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioEvaluationService — per-item 평가")
class PortfolioEvaluationServicePerItemTest {

    @Mock
    PortfolioItemRepository portfolioItemRepository;

    @Mock
    StockPriceService stockPriceService;

    @InjectMocks
    PortfolioEvaluationService service;

    private PortfolioItem stockItemOf(Long userId, Long itemId) {
        StockDetail detail = new StockDetail(
                StockSubType.INDIVIDUAL,
                "005930",
                "KOSPI",
                "KRX",
                "KR",
                10,
                BigDecimal.valueOf(70_000),
                BigDecimal.valueOf(2),
                PriceCurrency.KRW,
                null
        );
        PortfolioItem item = PortfolioItem.createWithStock(userId, "삼성전자", Region.DOMESTIC, detail);
        // Reconstruction with id
        return new PortfolioItem(
                itemId, item.getUserId(), item.getItemName(), item.getAssetType(),
                item.getInvestedAmount(), item.isNewsEnabled(), item.getRegion(),
                item.getMemo(), item.getStatus(), item.getCreatedAt(), item.getUpdatedAt(),
                item.getStockDetail(), null, null, null, null
        );
    }

    private StockPriceResponse priceOf(String currentPriceKrw, String changeRate) {
        return new StockPriceResponse(
                "005930",
                currentPriceKrw,        // currentPrice
                "78000",                // previousClose
                "2000",                 // change
                "1",                    // changeSign
                changeRate,             // changeRate
                "1000000",              // volume
                "80000000000",          // tradingAmount
                "82000",                // high
                "78000",                // low
                "79000",                // open
                "KOSPI",
                "KRX",
                "KRW",
                BigDecimal.ONE,
                currentPriceKrw,        // currentPriceKrw
                null,
                null,
                0L
        );
    }

    @Test
    @DisplayName("STOCK 단건 평가: currentPrice가 있으면 evaluatedAmount는 currentPrice × quantity")
    void evaluateOne_withPrice_returnsAccurateEvaluatedAmount() {
        PortfolioItem item = stockItemOf(1L, 100L);
        given(portfolioItemRepository.findById(100L)).willReturn(Optional.of(item));
        given(stockPriceService.getPrice("005930", MarketType.KOSPI, ExchangeCode.KRX))
                .willReturn(priceOf("80000", "2.56"));

        Optional<ItemEvaluation> result = service.evaluateOne(1L, 100L);

        assertThat(result).isPresent();
        ItemEvaluation eval = result.get();
        assertThat(eval.getPortfolioItemId()).isEqualTo(100L);
        assertThat(eval.getCurrentPrice()).isEqualTo("80000");
        assertThat(eval.getChangeRate()).isEqualTo("2.56");
        // evaluated = 80000 * 10 = 800,000
        assertThat(eval.getEvaluatedAmount()).isEqualByComparingTo(BigDecimal.valueOf(800_000));
        assertThat(eval.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("STOCK 단건 평가: KIS 호출 실패 시 currentPrice=null + investedAmount 반환")
    void evaluateOne_priceFetchFails_returnsNullCurrentPrice() {
        PortfolioItem item = stockItemOf(1L, 100L);
        given(portfolioItemRepository.findById(100L)).willReturn(Optional.of(item));
        given(stockPriceService.getPrice("005930", MarketType.KOSPI, ExchangeCode.KRX))
                .willThrow(new RuntimeException("KIS down"));

        Optional<ItemEvaluation> result = service.evaluateOne(1L, 100L);

        assertThat(result).isPresent();
        ItemEvaluation eval = result.get();
        assertThat(eval.getCurrentPrice()).isNull();
        assertThat(eval.getEvaluatedAmount()).isEqualByComparingTo(item.getInvestedAmount());
    }

    @Test
    @DisplayName("다른 사용자 소유 itemId 조회 시 IllegalArgumentException")
    void evaluateOne_otherUserItem_throws() {
        PortfolioItem item = stockItemOf(2L, 100L); // owned by user 2
        given(portfolioItemRepository.findById(100L)).willReturn(Optional.of(item));

        assertThatThrownBy(() -> service.evaluateOne(1L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("권한");
    }

    @Test
    @DisplayName("비-STOCK 항목은 IllegalArgumentException")
    void evaluateOne_nonStock_throws() {
        PortfolioItem cash = PortfolioItem.create(1L, "예금", AssetType.CASH,
                BigDecimal.valueOf(1_000_000), Region.DOMESTIC);
        // Reconstruction with id
        PortfolioItem reconstructed = new PortfolioItem(
                200L, cash.getUserId(), cash.getItemName(), cash.getAssetType(),
                cash.getInvestedAmount(), cash.isNewsEnabled(), cash.getRegion(),
                cash.getMemo(), cash.getStatus(), cash.getCreatedAt(), cash.getUpdatedAt(),
                null, null, null, null, null
        );
        given(portfolioItemRepository.findById(200L)).willReturn(Optional.of(reconstructed));

        assertThatThrownBy(() -> service.evaluateOne(1L, 200L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("주식");
    }

    @Test
    @DisplayName("존재하지 않는 itemId면 IllegalArgumentException")
    void evaluateOne_notFound_throws() {
        given(portfolioItemRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluateOne(1L, 999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("computeTotalAsset: ACTIVE 항목들 평가금액 합계를 반환")
    void computeTotalAsset_returnsSumOfEvaluatedAmounts() {
        PortfolioItem stock = stockItemOf(1L, 100L);
        PortfolioItem cash = new PortfolioItem(
                101L, 1L, "예금", AssetType.CASH,
                BigDecimal.valueOf(500_000), false, Region.DOMESTIC,
                null, com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus.ACTIVE,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),
                null, null, null, null, null
        );
        given(portfolioItemRepository.findByUserIdIn(List.of(1L)))
                .willReturn(List.of(stock, cash));
        given(stockPriceService.getPrice("005930", MarketType.KOSPI, ExchangeCode.KRX))
                .willReturn(priceOf("80000", "2.56"));

        BigDecimal total = service.computeTotalAsset(1L);

        // 80000 * 10 (stock) + 500,000 (cash) = 1,300,000
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(1_300_000));
    }

    @Test
    @DisplayName("computeTotalAsset: 사용자 항목 없을 때 0 반환")
    void computeTotalAsset_emptyPortfolio_returnsZero() {
        given(portfolioItemRepository.findByUserIdIn(List.of(1L))).willReturn(List.of());

        BigDecimal total = service.computeTotalAsset(1L);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }
}