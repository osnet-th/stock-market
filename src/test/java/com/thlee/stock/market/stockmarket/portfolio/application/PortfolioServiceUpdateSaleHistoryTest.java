package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockSaleHistoryResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.UpdateSaleParam;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashStockLink;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockSaleHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
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
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PortfolioService — 매도 이력 사후 수정/삭제")
class PortfolioServiceUpdateSaleHistoryTest {

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
    private static final Long CASH_ITEM_ID = 200L;
    private static final Long HISTORY_ID = 300L;
    private static final LocalDate TODAY = LocalDate.now();

    private PortfolioItem stockItem(int quantity, PortfolioItemStatus status) {
        StockDetail detail = new StockDetail(
                StockSubType.INDIVIDUAL, "005930", "KOSPI", "KRX", "KR",
                quantity, BigDecimal.valueOf(70_000),
                BigDecimal.valueOf(2), PriceCurrency.KRW, null
        );
        return new PortfolioItem(
                STOCK_ITEM_ID, USER_ID, "삼성전자", AssetType.STOCK,
                BigDecimal.valueOf(70_000).multiply(BigDecimal.valueOf(Math.max(quantity, 0))),
                false, Region.DOMESTIC, null,
                status, 0L, LocalDateTime.now(), LocalDateTime.now(),
                detail, null, null, null, null
        );
    }

    private PortfolioItem cashItem(BigDecimal amount) {
        CashDetail cashDetail = new CashDetail(CashSubType.CMA,
                BigDecimal.valueOf(3.5), TODAY, null, null, null, null);
        return new PortfolioItem(
                CASH_ITEM_ID, USER_ID, "CMA", AssetType.CASH,
                amount, false, Region.DOMESTIC, null,
                PortfolioItemStatus.ACTIVE, 0L,
                LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null, cashDetail
        );
    }

    private StockSaleHistory existingHistory(int quantity, BigDecimal salePrice, boolean unrecorded) {
        return StockSaleHistory.create(
                STOCK_ITEM_ID, quantity,
                BigDecimal.valueOf(70_000), salePrice,
                "KRW", BigDecimal.ONE,
                BigDecimal.valueOf(10_000_000),
                SaleReason.TARGET_PRICE_REACHED,
                "익절", "005930", "삼성전자",
                unrecorded, TODAY, TODAY
        );
    }

    @BeforeEach
    void stubSaveEcho() {
        given(stockSaleHistoryRepository.save(any(StockSaleHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("매도 이력 수량 감소: PortfolioItem 보유 수량 복원, CASH 차감")
    void update_quantityDecrease_restoresStockAndDeductsCash() {
        PortfolioItem stock = stockItem(7, PortfolioItemStatus.ACTIVE); // 매도 후 잔여 7
        PortfolioItem cash = cashItem(BigDecimal.valueOf(740_000)); // 매도로 240,000 입금된 상태
        StockSaleHistory history = existingHistory(3, BigDecimal.valueOf(80_000), false);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(stockSaleHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));

        StockSaleHistoryResponse resp = portfolioService.updateSaleHistory(
                USER_ID, STOCK_ITEM_ID, HISTORY_ID,
                new UpdateSaleParam(2, BigDecimal.valueOf(80_000), SaleReason.TARGET_PRICE_REACHED, "수정"));

        // 수량 3 → 2: 보유 수량 7 + 1 = 8
        assertThat(stock.getStockDetail().getQuantity()).isEqualTo(8);
        // 새 salePriceKrw = 80,000 * 2 = 160,000
        // 차액 = 160,000 - 240,000 = -80,000 → CASH 차감 80,000
        assertThat(cash.getInvestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(660_000));
        assertThat(resp.getQuantity()).isEqualTo(2);
        // profit = (80000 - 70000) * 2 = 20,000
        assertThat(resp.getProfit()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
    }

    @Test
    @DisplayName("매도 이력 단가 수정: profit 재계산, CASH 차액 조정")
    void update_salePriceChange_recomputesAndAdjustsCash() {
        PortfolioItem stock = stockItem(7, PortfolioItemStatus.ACTIVE);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(740_000));
        StockSaleHistory history = existingHistory(3, BigDecimal.valueOf(80_000), false);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(stockSaleHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));

        StockSaleHistoryResponse resp = portfolioService.updateSaleHistory(
                USER_ID, STOCK_ITEM_ID, HISTORY_ID,
                new UpdateSaleParam(3, BigDecimal.valueOf(90_000), SaleReason.TARGET_PRICE_REACHED, null));

        // 수량 동일, salePrice 80→90 → newSalePriceKrw = 270,000, delta = +30,000
        assertThat(cash.getInvestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(770_000));
        // profit = (90000 - 70000) * 3 = 60,000
        assertThat(resp.getProfit()).isEqualByComparingTo(BigDecimal.valueOf(60_000));
    }

    @Test
    @DisplayName("CLOSED 항목의 매도 이력 수정으로 잔여 수량 > 0 → reopenItem")
    void update_fromClosedToActive_reopens() {
        PortfolioItem stock = stockItem(0, PortfolioItemStatus.CLOSED);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(800_000));
        StockSaleHistory history = existingHistory(10, BigDecimal.valueOf(80_000), false);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(stockSaleHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.empty());

        portfolioService.updateSaleHistory(USER_ID, STOCK_ITEM_ID, HISTORY_ID,
                new UpdateSaleParam(8, BigDecimal.valueOf(80_000), SaleReason.TARGET_PRICE_REACHED, null));

        // 수량 10 → 8: 잔여 0 + 2 = 2 (양수)
        assertThat(stock.getStockDetail().getQuantity()).isEqualTo(2);
        assertThat(stock.getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
    }

    @Test
    @DisplayName("매도 이력 수정으로 추가 차감하여 잔여 수량 0 → closeItem 자동")
    void update_quantityIncrease_closesIfDepleted() {
        PortfolioItem stock = stockItem(2, PortfolioItemStatus.ACTIVE);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(640_000));
        StockSaleHistory history = existingHistory(8, BigDecimal.valueOf(80_000), false);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(stockSaleHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));

        portfolioService.updateSaleHistory(USER_ID, STOCK_ITEM_ID, HISTORY_ID,
                new UpdateSaleParam(10, BigDecimal.valueOf(80_000), SaleReason.TARGET_PRICE_REACHED, null));

        // 수량 8 → 10: 잔여 2 - 2 = 0 → 자동 CLOSED
        assertThat(stock.getStockDetail().getQuantity()).isZero();
        assertThat(stock.getStatus()).isEqualTo(PortfolioItemStatus.CLOSED);
    }

    @Test
    @DisplayName("매도 이력 수정으로 보유 수량 초과 → IllegalArgumentException")
    void update_overflowQuantity_throws() {
        PortfolioItem stock = stockItem(2, PortfolioItemStatus.ACTIVE);
        StockSaleHistory history = existingHistory(8, BigDecimal.valueOf(80_000), false);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(stockSaleHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.updateSaleHistory(USER_ID, STOCK_ITEM_ID, HISTORY_ID,
                new UpdateSaleParam(15, BigDecimal.valueOf(80_000), SaleReason.OTHER, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("보유 수량");
    }

    @Test
    @DisplayName("매도 이력 삭제: 보유 수량 복원, status 복원, CASH 차감")
    void delete_restoresAllState() {
        PortfolioItem stock = stockItem(0, PortfolioItemStatus.CLOSED);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(800_000));
        StockSaleHistory history = existingHistory(10, BigDecimal.valueOf(80_000), false);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(stockSaleHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));

        portfolioService.deleteSaleHistory(USER_ID, STOCK_ITEM_ID, HISTORY_ID);

        assertThat(stock.getStockDetail().getQuantity()).isEqualTo(10);
        assertThat(stock.getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
        // CASH 차감 = 800,000 - 800,000 = 0
        assertThat(cash.getInvestedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("매도 이력 삭제: unrecordedDeposit=true 이력 삭제 시 CASH 차감 건너뜀")
    void delete_unrecorded_skipsCashDeduction() {
        PortfolioItem stock = stockItem(8, PortfolioItemStatus.ACTIVE);
        StockSaleHistory history = existingHistory(2, BigDecimal.valueOf(80_000), true);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(stockSaleHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(history));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID)).willReturn(Optional.empty());

        portfolioService.deleteSaleHistory(USER_ID, STOCK_ITEM_ID, HISTORY_ID);

        assertThat(stock.getStockDetail().getQuantity()).isEqualTo(10);
        // CASH는 호출되지 않으므로 검증할 cashItem 없음
    }

    @Test
    @DisplayName("다른 사용자 이력 접근 → IllegalArgumentException")
    void update_otherUserItem_throws() {
        PortfolioItem stock = stockItem(7, PortfolioItemStatus.ACTIVE);
        // user 변경
        PortfolioItem stockOfOtherUser = new PortfolioItem(
                STOCK_ITEM_ID, 999L, stock.getItemName(), stock.getAssetType(),
                stock.getInvestedAmount(), stock.isNewsEnabled(), stock.getRegion(),
                stock.getMemo(), stock.getStatus(), stock.getVersion(),
                stock.getCreatedAt(), stock.getUpdatedAt(),
                stock.getStockDetail(), null, null, null, null
        );
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stockOfOtherUser));

        assertThatThrownBy(() -> portfolioService.updateSaleHistory(USER_ID, STOCK_ITEM_ID, HISTORY_ID,
                new UpdateSaleParam(1, BigDecimal.valueOf(80_000), SaleReason.OTHER, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("이력의 portfolioItemId가 요청 stockItemId와 다를 때 IllegalArgumentException")
    void update_historyPortfolioItemMismatch_throws() {
        PortfolioItem stock = stockItem(7, PortfolioItemStatus.ACTIVE);
        StockSaleHistory historyForOtherItem = StockSaleHistory.create(
                999L, 3, BigDecimal.valueOf(70_000), BigDecimal.valueOf(80_000),
                "KRW", BigDecimal.ONE, BigDecimal.valueOf(1_000_000),
                SaleReason.OTHER, null, "X", "X", false, TODAY, TODAY
        );
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(stockSaleHistoryRepository.findById(HISTORY_ID)).willReturn(Optional.of(historyForOtherItem));

        assertThatThrownBy(() -> portfolioService.updateSaleHistory(USER_ID, STOCK_ITEM_ID, HISTORY_ID,
                new UpdateSaleParam(1, BigDecimal.valueOf(80_000), SaleReason.OTHER, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}