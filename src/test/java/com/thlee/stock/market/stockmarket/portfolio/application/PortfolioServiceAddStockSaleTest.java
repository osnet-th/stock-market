package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.AddStockSaleParam;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.StockSaleHistoryResponse;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PortfolioService.addStockSale")
class PortfolioServiceAddStockSaleTest {

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
    private static final LocalDate TODAY = LocalDate.now();

    private PortfolioItem stockItemKrw(int quantity) {
        StockDetail detail = new StockDetail(
                StockSubType.INDIVIDUAL, "005930", "KOSPI", "KRX", "KR",
                quantity, BigDecimal.valueOf(70_000),
                BigDecimal.valueOf(2), PriceCurrency.KRW, null
        );
        PortfolioItem item = PortfolioItem.createWithStock(USER_ID, "삼성전자", Region.DOMESTIC, detail);
        return reconstructWithId(item, STOCK_ITEM_ID);
    }

    private PortfolioItem stockItemUsd(int quantity) {
        StockDetail detail = new StockDetail(
                StockSubType.INDIVIDUAL, "AAPL", "NAS", "NAS", "US",
                quantity, BigDecimal.valueOf(150),
                BigDecimal.valueOf(0.5), PriceCurrency.USD, BigDecimal.valueOf(1_500_000)
        );
        PortfolioItem item = PortfolioItem.createWithStock(USER_ID, "Apple", Region.INTERNATIONAL, detail);
        return reconstructWithId(item, STOCK_ITEM_ID);
    }

    private PortfolioItem cashItem(BigDecimal amount) {
        // 도메인 factory는 amount > 0 검증을 강제하므로 0원 케이스를 위해 재구성 생성자 사용
        CashDetail cashDetail = new CashDetail(CashSubType.CMA,
                BigDecimal.valueOf(3.5), TODAY, null, null, null, null);
        return new PortfolioItem(
                CASH_ITEM_ID, USER_ID, "CMA", AssetType.CASH,
                amount, false, Region.DOMESTIC, null,
                PortfolioItemStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now(),
                null, null, null, null, cashDetail
        );
    }

    private PortfolioItem reconstructWithId(PortfolioItem origin, Long id) {
        return new PortfolioItem(
                id, origin.getUserId(), origin.getItemName(), origin.getAssetType(),
                origin.getInvestedAmount(), origin.isNewsEnabled(), origin.getRegion(),
                origin.getMemo(), origin.getStatus(), origin.getCreatedAt(), origin.getUpdatedAt(),
                origin.getStockDetail(), origin.getBondDetail(), origin.getRealEstateDetail(),
                origin.getFundDetail(), origin.getCashDetail()
        );
    }

    @BeforeEach
    void stubSaveEcho() {
        // save 호출 시 입력 그대로 반환 (id 부여 없는 echo)
        given(stockSaleHistoryRepository.save(any(StockSaleHistory.class)))
                .willAnswer(inv -> inv.getArgument(0));
    }

    private AddStockSaleParam param(int quantity, BigDecimal salePrice, BigDecimal fxRate, Long depositCashItemId) {
        return new AddStockSaleParam(quantity, salePrice, TODAY,
                SaleReason.TARGET_PRICE_REACHED, "익절", fxRate, depositCashItemId);
    }

    @Test
    @DisplayName("KRW 부분 매도: 수량 차감, status ACTIVE 유지, 연결된 CASH 자동 입금")
    void krwPartialSale_withLink_creditsCashAndKeepsActive() {
        PortfolioItem stock = stockItemKrw(10);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(500_000));
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(10_000_000));

        StockSaleHistoryResponse resp = portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(3, BigDecimal.valueOf(80_000), null, null));

        assertThat(resp.getQuantity()).isEqualTo(3);
        // profit = (80000 - 70000) * 3 = 30,000
        assertThat(resp.getProfit()).isEqualByComparingTo(BigDecimal.valueOf(30_000));
        assertThat(resp.getSalePriceKrw()).isEqualByComparingTo(BigDecimal.valueOf(240_000));
        assertThat(resp.isUnrecordedDeposit()).isFalse();
        assertThat(stock.getStockDetail().getQuantity()).isEqualTo(7);
        assertThat(stock.getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
        // CASH 잔액 = 500,000 + 240,000 = 740,000
        assertThat(cash.getInvestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(740_000));
    }

    @Test
    @DisplayName("KRW 전량 매도: status CLOSED, CashStockLink 해제")
    void krwFullSale_closesItemAndRemovesLink() {
        PortfolioItem stock = stockItemKrw(5);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(0));
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(5_000_000));

        portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(5, BigDecimal.valueOf(80_000), null, null));

        assertThat(stock.getStockDetail().getQuantity()).isZero();
        assertThat(stock.getStatus()).isEqualTo(PortfolioItemStatus.CLOSED);
    }

    @Test
    @DisplayName("USD 매도: 사용자 fxRate가 자동 조회보다 우선 적용된다")
    void usdSale_userFxRateOverridesPort() {
        PortfolioItem stock = stockItemUsd(5);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(0));
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(20_000_000));

        StockSaleHistoryResponse resp = portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(5, BigDecimal.valueOf(180), BigDecimal.valueOf(1_400), null));

        // profit (USD) = (180-150)*5 = 150
        assertThat(resp.getProfit()).isEqualByComparingTo(BigDecimal.valueOf(150));
        // salePriceKrw = 180 * 1400 * 5 = 1,260,000
        assertThat(resp.getSalePriceKrw()).isEqualByComparingTo(BigDecimal.valueOf(1_260_000));
        assertThat(resp.getProfitKrw()).isEqualByComparingTo(BigDecimal.valueOf(210_000));
        // CASH 입금 = salePriceKrw
        assertThat(cash.getInvestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_260_000));
    }

    @Test
    @DisplayName("USD 매도: 환율 자동 조회 실패 시 unrecordedDeposit=true, CASH 입금 건너뜀")
    void usdSale_fxLookupFails_unrecorded() {
        PortfolioItem stock = stockItemUsd(5);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(exchangeRatePort.getRate("USD")).willThrow(new RuntimeException("FX down"));
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(20_000_000));

        StockSaleHistoryResponse resp = portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(2, BigDecimal.valueOf(180), null, null));

        assertThat(resp.isUnrecordedDeposit()).isTrue();
        assertThat(resp.getSalePriceKrw()).isNull();
        assertThat(resp.getProfitKrw()).isNull();
    }

    @Test
    @DisplayName("CashStockLink 미연결 + depositCashItemId 지정 → 해당 CASH에 입금")
    void noLink_withDepositCashItemId_credits() {
        PortfolioItem stock = stockItemKrw(10);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(100_000));
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.empty());
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(1_000_000));

        portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(2, BigDecimal.valueOf(80_000), null, CASH_ITEM_ID));

        // CASH 잔액 = 100,000 + 160,000 = 260,000
        assertThat(cash.getInvestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(260_000));
    }

    @Test
    @DisplayName("CashStockLink 미연결 + depositCashItemId 미지정 + CASH 0개 → unrecordedDeposit=true")
    void noLink_noDepositId_noCash_unrecorded() {
        PortfolioItem stock = stockItemKrw(10);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID)).willReturn(Optional.empty());
        given(portfolioItemRepository.findByUserId(USER_ID))
                .willReturn(List.of(stock));   // CASH 항목 없음
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(1_000_000));

        StockSaleHistoryResponse resp = portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(2, BigDecimal.valueOf(80_000), null, null));

        assertThat(resp.isUnrecordedDeposit()).isTrue();
    }

    @Test
    @DisplayName("CashStockLink 미연결 + depositCashItemId 미지정 + CASH 1개 이상 → IllegalArgumentException")
    void noLink_noDepositId_hasCash_throws() {
        PortfolioItem stock = stockItemKrw(10);
        PortfolioItem cash = cashItem(BigDecimal.valueOf(100_000));
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID)).willReturn(Optional.empty());
        given(portfolioItemRepository.findByUserId(USER_ID))
                .willReturn(List.of(stock, cash));
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(1_000_000));

        assertThatThrownBy(() -> portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(2, BigDecimal.valueOf(80_000), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("입금할 원화");
    }

    @Test
    @DisplayName("보유 수량 초과 매도 → IllegalArgumentException")
    void overQuantity_throws() {
        PortfolioItem stock = stockItemKrw(5);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(1_000_000));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cashItem(BigDecimal.ZERO)));

        assertThatThrownBy(() -> portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(6, BigDecimal.valueOf(80_000), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("보유 수량");
    }

    @Test
    @DisplayName("미래 soldAt → IllegalArgumentException (도메인에서 차단)")
    void futureSoldAt_throws() {
        PortfolioItem stock = stockItemKrw(10);
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));
        given(portfolioEvaluationService.computeTotalAsset(USER_ID))
                .willReturn(BigDecimal.valueOf(1_000_000));
        given(cashStockLinkRepository.findByStockItemId(STOCK_ITEM_ID))
                .willReturn(Optional.of(new CashStockLink(1L, CASH_ITEM_ID, STOCK_ITEM_ID, LocalDateTime.now())));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cashItem(BigDecimal.ZERO)));

        AddStockSaleParam future = new AddStockSaleParam(1, BigDecimal.valueOf(80_000),
                TODAY.plusDays(1), SaleReason.OTHER, null, null, null);
        assertThatThrownBy(() -> portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID, future))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CLOSED 항목 매도 시도 → IllegalArgumentException")
    void closedItem_cannotSell() {
        PortfolioItem stock = stockItemKrw(10);
        stock.closeItem();
        given(portfolioItemRepository.findById(STOCK_ITEM_ID)).willReturn(Optional.of(stock));

        assertThatThrownBy(() -> portfolioService.addStockSale(USER_ID, STOCK_ITEM_ID,
                param(1, BigDecimal.valueOf(80_000), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("마감");
    }

    @Test
    @DisplayName("비-STOCK 항목 매도 시도 → IllegalArgumentException")
    void nonStock_cannotSell() {
        PortfolioItem cash = cashItem(BigDecimal.valueOf(100_000));
        given(portfolioItemRepository.findById(CASH_ITEM_ID)).willReturn(Optional.of(cash));

        assertThatThrownBy(() -> portfolioService.addStockSale(USER_ID, CASH_ITEM_ID,
                param(1, BigDecimal.valueOf(80_000), null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("주식");
    }
}