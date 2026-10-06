package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.DepositMode;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PortfolioAutoDepositBatchService — 자동 납입 일배치")
class PortfolioAutoDepositBatchServiceTest {

    /** 2026-10-26 00:10 KST. UTC 기준으로는 아직 10-25다. */
    private static final Clock BATCH_CLOCK =
            Clock.fixed(Instant.parse("2026-10-25T15:10:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDate KST_TODAY = LocalDate.of(2026, 10, 26);
    private static final Long ITEM_A = 1L;
    private static final Long ITEM_B = 2L;

    @Mock PortfolioItemRepository portfolioItemRepository;
    @Mock PortfolioService portfolioService;

    private PortfolioAutoDepositBatchService batchService;

    @BeforeEach
    void setUp() {
        batchService = new PortfolioAutoDepositBatchService(portfolioItemRepository, portfolioService, BATCH_CLOCK);
    }

    @Test
    @DisplayName("B1 KST 오늘 날짜로 납입일인 항목만 기록한다")
    void recordsOnlyItemsDueOnKstToday() {
        given(portfolioItemRepository.findActiveAutoDepositCashItems())
                .willReturn(List.of(autoSavings(ITEM_A, 26), autoSavings(ITEM_B, 25)));
        given(portfolioService.recordAutoDeposit(ITEM_A, KST_TODAY)).willReturn(true);

        int recorded = batchService.recordDueAutoDeposits();

        assertThat(recorded).isEqualTo(1);
    }

    @Test
    @DisplayName("B2 한 항목이 실패해도 다른 항목은 기록한다")
    void isolatesFailurePerItem() {
        given(portfolioItemRepository.findActiveAutoDepositCashItems())
                .willReturn(List.of(autoSavings(ITEM_A, 26), autoSavings(ITEM_B, 26)));
        given(portfolioService.recordAutoDeposit(ITEM_A, KST_TODAY)).willThrow(new IllegalStateException("저장 실패"));
        given(portfolioService.recordAutoDeposit(ITEM_B, KST_TODAY)).willReturn(true);

        int recorded = batchService.recordDueAutoDeposits();

        assertThat(recorded).isEqualTo(1);
    }

    @Test
    @DisplayName("B3 대상이 없으면 0건이다")
    void noTargets_recordsNothing() {
        given(portfolioItemRepository.findActiveAutoDepositCashItems()).willReturn(List.of());

        assertThat(batchService.recordDueAutoDeposits()).isZero();
    }

    private static PortfolioItem autoSavings(Long id, int depositDay) {
        CashDetail detail = new CashDetail(CashSubType.SAVINGS, BigDecimal.valueOf(3), null, null, TaxType.GENERAL,
                BigDecimal.valueOf(300_000), depositDay, DepositMode.AUTO);
        LocalDateTime now = LocalDateTime.now();
        return new PortfolioItem(id, 1L, "적금 " + id, AssetType.CASH, BigDecimal.valueOf(1_000_000), false,
                Region.DOMESTIC, null, null, PortfolioItemStatus.ACTIVE, 0L, now, now,
                null, null, null, null, detail, null, null);
    }
}
