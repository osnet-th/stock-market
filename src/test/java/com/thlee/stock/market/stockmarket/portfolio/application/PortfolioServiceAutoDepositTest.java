package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserKeywordRepository;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.DepositHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.DepositMode;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PortfolioService — 자동납입 처리 방식과 자동 기록")
class PortfolioServiceAutoDepositTest {

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
    private static final Long ITEM_ID = 10L;
    private static final Long SAVED_ID = 900L;
    private static final String REGION = "DOMESTIC";
    private static final BigDecimal PRINCIPAL = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal MONTHLY = BigDecimal.valueOf(300_000);
    private static final int DEPOSIT_DAY = 25;
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 10, 25);

    private final List<PortfolioItem> savedItems = new ArrayList<>();
    private final List<DepositHistory> savedDeposits = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 저장 요청을 목록에 모아 상태로 확인한다. 새 항목은 저장소처럼 id를 붙인다.
        given(portfolioItemRepository.save(any(PortfolioItem.class))).willAnswer(invocation -> {
            PortfolioItem item = invocation.getArgument(0);
            savedItems.add(item);
            return item.getId() != null ? item : withId(item, SAVED_ID);
        });
        given(depositHistoryRepository.save(any(DepositHistory.class))).willAnswer(invocation -> {
            DepositHistory history = invocation.getArgument(0);
            savedDeposits.add(history);
            return new DepositHistory(500L, history.getPortfolioItemId(), history.getDepositDate(),
                    history.getAmount(), history.getUnits(), history.getMemo(), history.getCreatedAt());
        });
    }

    @Test
    @DisplayName("S1 등록 시 요청한 처리 방식을 응답에 담고, 처리 방식이 없으면 알림 확인이다")
    void add_reflectsDepositMode() {
        PortfolioItemResponse auto = addSavings("자동 적금", MONTHLY, "AUTO");
        PortfolioItemResponse none = addSavings("알림 적금", MONTHLY, null);

        assertThat(auto.getCashDetail().getDepositMode()).isEqualTo("AUTO");
        assertThat(none.getCashDetail().getDepositMode()).isEqualTo("NOTIFY");
    }

    @Test
    @DisplayName("S2 자동 반영인데 월 납입액이 없으면 등록을 거부하고 저장하지 않는다")
    void add_autoWithoutMonthlyAmount_throws() {
        assertThatThrownBy(() -> addSavings("자동 적금", null, "AUTO"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자동 납입은 월 납입액과 납입일(1~31일)을 입력해야 합니다.");
        assertThat(savedItems).isEmpty();
    }

    @Test
    @DisplayName("S3 수정 요청에 처리 방식이 없으면 알림 확인으로 바뀐다")
    void update_withoutDepositMode_becomesNotify() {
        givenItem(savings(PortfolioItemStatus.ACTIVE, DepositMode.AUTO));

        PortfolioItemResponse response = portfolioService.updateCashItem(USER_ID, ITEM_ID, "적금", PRINCIPAL,
                null, null, BigDecimal.valueOf(3), null, null, "GENERAL", MONTHLY, DEPOSIT_DAY, null);

        assertThat(response.getCashDetail().getDepositMode()).isEqualTo("NOTIFY");
    }

    @Test
    @DisplayName("S4 납입일이고 이번 달 기록이 없으면 월 납입액을 '자동 납입'으로 기록하고 원금에 더한다")
    void record_dueWithoutDepositThisMonth_records() {
        givenItem(savings(PortfolioItemStatus.ACTIVE, DepositMode.AUTO));
        givenDeposits();

        boolean recorded = portfolioService.recordAutoDeposit(ITEM_ID, DUE_DATE);

        assertThat(recorded).isTrue();
        assertThat(savedDeposits).singleElement().satisfies(history -> {
            assertThat(history.getPortfolioItemId()).isEqualTo(ITEM_ID);
            assertThat(history.getDepositDate()).isEqualTo(DUE_DATE);
            assertThat(history.getAmount()).isEqualByComparingTo(MONTHLY);
            assertThat(history.getUnits()).isNull();
            assertThat(history.getMemo()).isEqualTo("자동 납입");
        });
        assertThat(savedItems).singleElement().satisfies(item ->
                assertThat(item.getInvestedAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_300_000)));
    }

    @Test
    @DisplayName("S5 이번 달에 이미 납입 기록이 있으면 건너뛴다")
    void record_depositThisMonth_skips() {
        PortfolioItem item = savings(PortfolioItemStatus.ACTIVE, DepositMode.AUTO);
        givenItem(item);
        givenDeposits(LocalDate.of(2026, 10, 3));

        boolean recorded = portfolioService.recordAutoDeposit(ITEM_ID, DUE_DATE);

        assertThat(recorded).isFalse();
        assertThat(savedDeposits).isEmpty();
        assertThat(savedItems).isEmpty();
        assertThat(item.getInvestedAmount()).isEqualByComparingTo(PRINCIPAL);
    }

    @Test
    @DisplayName("S6 지난달 기록만 있으면 이번 달 몫을 기록한다")
    void record_onlyLastMonthDeposit_records() {
        givenItem(savings(PortfolioItemStatus.ACTIVE, DepositMode.AUTO));
        givenDeposits(LocalDate.of(2026, 9, 25));

        boolean recorded = portfolioService.recordAutoDeposit(ITEM_ID, DUE_DATE);

        assertThat(recorded).isTrue();
        assertThat(savedDeposits).hasSize(1);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("skippedCases")
    @DisplayName("S7 알림 확인 항목, 납입일이 아닌 날, 보유 종료 항목은 기록하지 않는다")
    void record_notDue_skips(String label, PortfolioItemStatus status, DepositMode mode, LocalDate date) {
        givenItem(savings(status, mode));
        givenDeposits();

        boolean recorded = portfolioService.recordAutoDeposit(ITEM_ID, date);

        assertThat(recorded).isFalse();
        assertThat(savedDeposits).isEmpty();
    }

    @Test
    @DisplayName("S8 수정 요청에 자동 반영을 보내면 자동 반영으로 저장한다")
    void update_withAuto_becomesAuto() {
        givenItem(savings(PortfolioItemStatus.ACTIVE, DepositMode.NOTIFY));

        PortfolioItemResponse response = portfolioService.updateCashItem(USER_ID, ITEM_ID, "적금", PRINCIPAL,
                null, null, BigDecimal.valueOf(3), null, null, "GENERAL", MONTHLY, DEPOSIT_DAY, "AUTO");

        assertThat(response.getCashDetail().getDepositMode()).isEqualTo("AUTO");
    }

    static Stream<Arguments> skippedCases() {
        return Stream.of(
                Arguments.of("알림 확인 항목", PortfolioItemStatus.ACTIVE, DepositMode.NOTIFY, LocalDate.of(2026, 10, 25)),
                Arguments.of("납입일이 아닌 날", PortfolioItemStatus.ACTIVE, DepositMode.AUTO, LocalDate.of(2026, 10, 24)),
                Arguments.of("보유 종료 항목", PortfolioItemStatus.CLOSED, DepositMode.AUTO, LocalDate.of(2026, 10, 25)));
    }

    private PortfolioItemResponse addSavings(String itemName, BigDecimal monthly, String depositMode) {
        return portfolioService.addCashItem(USER_ID, itemName, PRINCIPAL, REGION, null, null,
                "SAVINGS", BigDecimal.valueOf(3), null, null, "GENERAL", monthly, DEPOSIT_DAY, depositMode);
    }

    private void givenItem(PortfolioItem item) {
        given(portfolioItemRepository.findById(ITEM_ID)).willReturn(Optional.of(item));
    }

    private void givenDeposits(LocalDate... dates) {
        List<DepositHistory> histories = Arrays.stream(dates)
                .map(date -> new DepositHistory((long) date.getDayOfYear(), ITEM_ID, date, MONTHLY,
                        null, null, LocalDateTime.now()))
                .toList();
        given(depositHistoryRepository.findByPortfolioItemId(ITEM_ID)).willReturn(histories);
    }

    private static PortfolioItem savings(PortfolioItemStatus status, DepositMode mode) {
        CashDetail detail = new CashDetail(CashSubType.SAVINGS, BigDecimal.valueOf(3), null, null, TaxType.GENERAL,
                MONTHLY, DEPOSIT_DAY, mode);
        LocalDateTime now = LocalDateTime.now();
        return new PortfolioItem(ITEM_ID, USER_ID, "적금", AssetType.CASH, PRINCIPAL, false, Region.DOMESTIC,
                null, null, status, 0L, now, now, null, null, null, null, detail, null, null);
    }

    private static PortfolioItem withId(PortfolioItem origin, Long id) {
        return new PortfolioItem(
                id, origin.getUserId(), origin.getItemName(), origin.getAssetType(),
                origin.getInvestedAmount(), origin.isNewsEnabled(), origin.getRegion(),
                origin.getMemo(), origin.getInstitution(), origin.getStatus(), origin.getVersion(),
                origin.getCreatedAt(), origin.getUpdatedAt(),
                origin.getStockDetail(), origin.getBondDetail(), origin.getRealEstateDetail(),
                origin.getFundDetail(), origin.getCashDetail(), origin.getGoldDetail(), origin.getPensionDetail());
    }
}
