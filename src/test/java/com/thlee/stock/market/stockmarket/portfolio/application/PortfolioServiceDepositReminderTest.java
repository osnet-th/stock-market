package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.DepositHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.FundDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.DepositMode;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.FundSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioService — 만기된 현금성 항목의 납입일 당일·미납 판정")
class PortfolioServiceDepositReminderTest {

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

    private static final BigDecimal MONTHLY = BigDecimal.valueOf(300_000);
    private static final int DEPOSIT_DAY = 25;
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 10, 25);
    private static final LocalDate DAY_AFTER_DUE = LocalDate.of(2026, 10, 26);
    private static final List<DepositHistory> NO_DEPOSITS = List.of();

    @Test
    @DisplayName("R1 만기 전 항목은 지금처럼 납입일 당일과 다음 날 미납으로 본다")
    void beforeMaturity_keepsReminder() {
        // given
        PortfolioItem savings = savings(LocalDate.of(2027, 9, 1));

        // when, then
        assertThat(portfolioService.isDepositDueToday(savings, NO_DEPOSITS, DUE_DATE)).isTrue();
        assertThat(portfolioService.isDepositOverdue(savings, NO_DEPOSITS, DAY_AFTER_DUE)).isTrue();
    }

    @Test
    @DisplayName("R2 만기일 당일에는 납입일 당일·미납으로 보지 않는다")
    void onMaturityDate_noReminder() {
        // given
        PortfolioItem maturesOnDueDate = savings(DUE_DATE);
        PortfolioItem maturesDayAfterDue = savings(DAY_AFTER_DUE);

        // when, then
        assertThat(portfolioService.isDepositDueToday(maturesOnDueDate, NO_DEPOSITS, DUE_DATE)).isFalse();
        assertThat(portfolioService.isDepositOverdue(maturesDayAfterDue, NO_DEPOSITS, DAY_AFTER_DUE)).isFalse();
    }

    @Test
    @DisplayName("R3 만기가 지난 항목은 납입일 당일·미납으로 보지 않는다")
    void afterMaturity_noReminder() {
        // given
        PortfolioItem savings = savings(LocalDate.of(2026, 9, 30));

        // when, then
        assertThat(portfolioService.isDepositDueToday(savings, NO_DEPOSITS, DUE_DATE)).isFalse();
        assertThat(portfolioService.isDepositOverdue(savings, NO_DEPOSITS, DAY_AFTER_DUE)).isFalse();
    }

    @Test
    @DisplayName("R4 만기일이 없는 현금성 항목은 지금처럼 판정한다")
    void withoutMaturityDate_keepsReminder() {
        // given
        PortfolioItem savings = savings(null);

        // when, then
        assertThat(portfolioService.isDepositDueToday(savings, NO_DEPOSITS, DUE_DATE)).isTrue();
        assertThat(portfolioService.isDepositOverdue(savings, NO_DEPOSITS, DAY_AFTER_DUE)).isTrue();
    }

    @Test
    @DisplayName("R5 펀드는 지금처럼 미납으로 판정한다")
    void fund_keepsReminder() {
        // given
        PortfolioItem fund = fund();

        // when, then
        assertThat(portfolioService.isDepositOverdue(fund, NO_DEPOSITS, DAY_AFTER_DUE)).isTrue();
    }

    private static PortfolioItem savings(LocalDate maturityDate) {
        CashDetail detail = new CashDetail(CashSubType.SAVINGS, BigDecimal.valueOf(3), LocalDate.of(2026, 1, 1),
                maturityDate, TaxType.GENERAL, MONTHLY, DEPOSIT_DAY, DepositMode.NOTIFY);
        return item(AssetType.CASH, detail, null);
    }

    private static PortfolioItem fund() {
        FundDetail detail = new FundDetail(FundSubType.EQUITY_FUND, BigDecimal.ONE, MONTHLY, DEPOSIT_DAY);
        return item(AssetType.FUND, null, detail);
    }

    private static PortfolioItem item(AssetType assetType, CashDetail cashDetail, FundDetail fundDetail) {
        LocalDateTime now = LocalDateTime.now();
        return new PortfolioItem(10L, 1L, "납입 항목", assetType, BigDecimal.valueOf(1_000_000), false, Region.DOMESTIC,
                null, null, PortfolioItemStatus.ACTIVE, 0L, now, now, null, null, null, fundDetail, cashDetail, null, null);
    }
}
