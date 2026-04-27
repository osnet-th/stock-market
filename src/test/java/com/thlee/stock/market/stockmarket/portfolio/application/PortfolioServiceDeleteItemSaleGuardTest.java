package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockSaleHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.CashStockLinkRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.DepositHistoryRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockPurchaseHistoryRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockSaleHistoryRepository;
import com.thlee.stock.market.stockmarket.stock.domain.service.ExchangeRatePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PortfolioService.deleteItem — 매도 이력 가드")
class PortfolioServiceDeleteItemSaleGuardTest {

    @Mock PortfolioItemRepository portfolioItemRepository;
    @Mock StockPurchaseHistoryRepository purchaseHistoryRepository;
    @Mock StockSaleHistoryRepository stockSaleHistoryRepository;
    @Mock DepositHistoryRepository depositHistoryRepository;
    @Mock CashStockLinkRepository cashStockLinkRepository;
    @Mock PortfolioEvaluationService portfolioEvaluationService;
    @Mock ExchangeRatePort exchangeRatePort;
    @Mock KeywordService keywordService;
    @Mock KeywordRepository keywordRepository;
    @Mock UserKeywordRepository userKeywordRepository;
    @Mock DomainEventLogger domainEventLogger;

    @InjectMocks PortfolioService portfolioService;

    private static final Long USER_ID = 1L;
    private static final Long STOCK_ITEM_ID = 100L;
    private static final LocalDate TODAY = LocalDate.now();

    private PortfolioItem stockItem(int quantity, PortfolioItemStatus status) {
        StockDetail detail = new StockDetail(
                StockSubType.INDIVIDUAL, "005930", "KOSPI", "KRX", "KR",
                quantity, BigDecimal.valueOf(70_000),
                BigDecimal.valueOf(2), PriceCurrency.KRW, null
        );
        return new PortfolioItem(
                STOCK_ITEM_ID, USER_ID, "삼성전자", AssetType.STOCK,
                BigDecimal.valueOf(70_000).multiply(BigDecimal.valueOf(Math.max(quantity, 1))),
                false, Region.DOMESTIC, null,
                status, 0L, LocalDateTime.now(), LocalDateTime.now(),
                detail, null, null, null, null
        );
    }

    private StockSaleHistory anySaleHistory() {
        return StockSaleHistory.create(
                STOCK_ITEM_ID, 1, BigDecimal.valueOf(70_000), BigDecimal.valueOf(80_000),
                "KRW", BigDecimal.ONE, BigDecimal.valueOf(1_000_000),
                SaleReason.OTHER, null, "005930", "삼성전자",
                false, TODAY, TODAY
        );
    }

    @Test
    @DisplayName("매도 이력이 있는 STOCK 항목 삭제 시도 → IllegalArgumentException")
    void deleteStock_withSaleHistory_throws() {
        PortfolioItem stock = stockItem(10, PortfolioItemStatus.ACTIVE);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(stockSaleHistoryRepository.findByPortfolioItemId(STOCK_ITEM_ID))
                .willReturn(List.of(anySaleHistory()));

        assertThatThrownBy(() -> portfolioService.deleteItem(USER_ID, STOCK_ITEM_ID, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("매도 이력");
    }

    @Test
    @DisplayName("CLOSED 항목은 매도 이력 ≥ 1이므로 삭제 차단됨")
    void deleteClosedStock_alwaysHasSaleHistory_throws() {
        PortfolioItem stock = stockItem(0, PortfolioItemStatus.CLOSED);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(stockSaleHistoryRepository.findByPortfolioItemId(STOCK_ITEM_ID))
                .willReturn(List.of(anySaleHistory()));

        assertThatThrownBy(() -> portfolioService.deleteItem(USER_ID, STOCK_ITEM_ID, false, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("매도 이력 0건 + ACTIVE 항목은 정상 삭제")
    void deleteStock_withoutSaleHistory_succeeds() {
        PortfolioItem stock = stockItem(10, PortfolioItemStatus.ACTIVE);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(stockSaleHistoryRepository.findByPortfolioItemId(STOCK_ITEM_ID))
                .willReturn(List.of());
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.empty());

        assertThatCode(() -> portfolioService.deleteItem(USER_ID, STOCK_ITEM_ID, false, null))
                .doesNotThrowAnyException();
    }
}