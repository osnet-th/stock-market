package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioIncomeResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.DividendSchedule;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.service.DividendSchedulePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioIncomeService — 배당 지급일 기준 집계")
class PortfolioIncomeServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 2026-10-15 10:00 KST */
    private static final Clock OCT_15 = Clock.fixed(Instant.parse("2026-10-15T01:00:00Z"), KST);
    /** 기준 달 2026-10 의 조회 기간: 17개월 전 1일 ~ 기준 달 말일 */
    private static final LocalDate QUERY_FROM = LocalDate.of(2025, 5, 1);
    private static final LocalDate QUERY_TO = LocalDate.of(2026, 10, 31);
    private static final String ACTUAL = "ACTUAL_PAYMENT_DATE";
    private static final String FALLBACK = "ESTIMATED_MONTHLY_AVERAGE";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Mock
    DividendSchedulePort dividendSchedulePort;

    private final Map<Long, BigDecimal> evaluatedById = new LinkedHashMap<>();
    private long nextId = 1;

    @Test
    @DisplayName("S1 국내 주식은 지급일이 이번 달인 배당 × 수량이 이달, 최근 12개월 지급분이 연 예상이다")
    void domesticStockUsesPaymentDate() {
        PortfolioItem samsung = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", 10, "700000", null);
        givenSchedules("005930", paid("2026-10-20", "361"), paid("2026-04-20", "361"), paid("2025-10-20", "361"));

        PortfolioIncomeResponse result = summarize(OCT_15, samsung);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("3610");
        assertThat(result.getYearEstimate()).isEqualByComparingTo("7220");
        assertThat(result.getDividendYield()).isEqualByComparingTo("1.03");
        assertThat(result.getExcludedCount()).isZero();
        assertThat(result.getBasis()).isEqualTo(ACTUAL);
    }

    @Test
    @DisplayName("S2 지난달 지급분은 연 예상에만, 다음 달·지급일 없음·12개월 이전은 어디에도 넣지 않는다")
    void appliesPeriodBoundaries() {
        PortfolioItem samsung = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", 10, "700000", null);
        givenSchedules("005930",
                paid("2026-09-30", "100"),
                paid("2026-11-02", "200"),
                paid(null, "300"),
                paid("2025-10-31", "400"),
                paid("2025-11-01", "50"));

        PortfolioIncomeResponse result = summarize(OCT_15, samsung);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("0");
        assertThat(result.getYearEstimate()).isEqualByComparingTo("1500");
    }

    @Test
    @DisplayName("S3 해외 주식 배당과 예금 이자는 연 예상 ÷ 12 로 이달 금액에 더한다")
    void addsMonthlyAverageOfOverseasDividendAndInterest() {
        PortfolioItem samsung = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", 10, "700000", null);
        PortfolioItem apple = stock(StockSubType.INDIVIDUAL, "AAPL", "NASDAQ", 5, "1200000", "2.00");
        PortfolioItem deposit = deposit("12000000", "3.00");
        givenSchedules("005930", paid("2026-10-20", "361"), paid("2026-04-20", "361"));

        PortfolioIncomeResponse result = summarize(OCT_15, samsung, apple, deposit);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("35610");
        assertThat(result.getYearEstimate()).isEqualByComparingTo("391220");
        assertThat(result.getDividendYield()).isEqualByComparingTo("1.64");
        assertThat(result.getBasis()).isEqualTo(ACTUAL);
    }

    @Test
    @DisplayName("S4 KSD 배당 기록이 없는 ETF 는 입력 배당률로 계산하고, 배당률도 없으면 제외 건수에 센다")
    void etfWithoutKsdRecordsFallsBackToDividendYield() {
        PortfolioItem kodex = stock(StockSubType.ETF, "069500", "KOSPI", 30, "1000000", "3.00");
        PortfolioItem noYieldEtf = stock(StockSubType.ETF, "102110", "KOSPI", 20, "800000", null);
        givenSchedules("069500");
        givenSchedules("102110");

        PortfolioIncomeResponse result = summarize(OCT_15, kodex, noYieldEtf);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("2500");
        assertThat(result.getYearEstimate()).isEqualByComparingTo("30000");
        assertThat(result.getExcludedCount()).isEqualTo(1);
        assertThat(result.getBasis()).isEqualTo(ACTUAL);
    }

    @Test
    @DisplayName("S5 KSD 배당 기록이 없는 개별 종목은 입력 배당률이 있어도 0 이고 제외 건수에 세지 않는다")
    void individualStockWithoutKsdRecordsIsZero() {
        PortfolioItem hynix = stock(StockSubType.INDIVIDUAL, "000660", "KOSPI", 5, "500000", "2.00");
        givenSchedules("000660");

        PortfolioIncomeResponse result = summarize(OCT_15, hynix);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("0");
        assertThat(result.getYearEstimate()).isEqualByComparingTo("0");
        assertThat(result.getExcludedCount()).isZero();
        assertThat(result.getDividendYield()).isNull();
    }

    @Test
    @DisplayName("S6 국내 종목 조회가 하나라도 실패하면 국내 주식 전부를 입력 배당률로 계산하고 폴백 기준을 돌려준다")
    void fallsBackWhenAnyDomesticLookupFails() {
        PortfolioItem samsung = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", 10, "600000", "2.00");
        PortfolioItem hynix = stock(StockSubType.INDIVIDUAL, "000660", "KOSPI", 5, "300000", "4.00");
        givenSchedules("005930", paid("2026-10-20", "361"));
        given(dividendSchedulePort.findCashDividends("000660", QUERY_FROM, QUERY_TO))
                .willThrow(new RuntimeException("KSD 조회 실패"));

        PortfolioIncomeResponse result = summarize(OCT_15, samsung, hynix);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("2000");
        assertThat(result.getYearEstimate()).isEqualByComparingTo("24000");
        assertThat(result.getBasis()).isEqualTo(FALLBACK);
    }

    @Test
    @DisplayName("S7 시가배당률의 분모는 연 배당이 있는 주식의 평가액만 쓴다")
    void dividendYieldUsesOnlyPayingStocksAsBase() {
        PortfolioItem samsung = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", 10, "700000", null);
        PortfolioItem hynix = stock(StockSubType.INDIVIDUAL, "000660", "KOSPI", 5, "300000", null);
        givenSchedules("005930", paid("2026-04-20", "361"));
        givenSchedules("000660");

        PortfolioIncomeResponse result = summarize(OCT_15, samsung, hynix);

        assertThat(result.getDividendYield()).isEqualByComparingTo("0.52");
    }

    @Test
    @DisplayName("S8 같은 종목을 두 항목으로 보유하면 각 항목 수량으로 더한다")
    void sumsEachItemQuantityForSameStock() {
        PortfolioItem first = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", 10, "700000", null);
        PortfolioItem second = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", 5, "350000", null);
        givenSchedules("005930", paid("2026-10-20", "361"));

        PortfolioIncomeResponse result = summarize(OCT_15, first, second);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("5415");
    }

    @Test
    @DisplayName("S9 이번 달은 KST 기준이다 (UTC 9/30 15:30 = KST 10/1 00:30)")
    void usesKstForCurrentMonth() {
        Clock oct1Kst = Clock.fixed(Instant.parse("2026-09-30T15:30:00Z"), KST);
        PortfolioItem samsung = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", 10, "700000", null);
        givenSchedules("005930", paid("2026-10-01", "100"), paid("2026-09-30", "200"));

        PortfolioIncomeResponse result = summarize(oct1Kst, samsung);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("S10 시장 값이 없거나 해석할 수 없는 주식은 해외 주식처럼 입력 배당률로 계산한다")
    void unknownMarketUsesDividendYield() {
        PortfolioItem noMarket = stock(StockSubType.INDIVIDUAL, "111111", null, 10, "1200000", "2.00");
        PortfolioItem unknownMarket = stock(StockSubType.INDIVIDUAL, "222222", "UNKNOWN", 10, "1200000", "2.00");

        PortfolioIncomeResponse result = summarize(OCT_15, noMarket, unknownMarket);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("4000");
        assertThat(result.getYearEstimate()).isEqualByComparingTo("48000");
        assertThat(result.getBasis()).isEqualTo(ACTUAL);
    }

    @Test
    @DisplayName("S11 수량이 없는 국내 주식은 0 으로 계산한다")
    void missingQuantityCountsAsZero() {
        PortfolioItem samsung = stock(StockSubType.INDIVIDUAL, "005930", "KOSPI", null, "700000", null);
        givenSchedules("005930", paid("2026-10-20", "361"));

        PortfolioIncomeResponse result = summarize(OCT_15, samsung);

        assertThat(result.getMonthAmount()).isEqualByComparingTo("0");
        assertThat(result.getYearEstimate()).isEqualByComparingTo("0");
    }

    private PortfolioIncomeResponse summarize(Clock clock, PortfolioItem... items) {
        PortfolioIncomeService service = new PortfolioIncomeService(dividendSchedulePort, clock);
        return service.summarize(List.of(items), evaluation());
    }

    private PortfolioEvaluation evaluation() {
        List<ItemEvaluation> rows = new ArrayList<>();
        evaluatedById.forEach((id, amount) ->
                rows.add(new ItemEvaluation(id, "항목" + id, "STOCK", null, amount, amount, null, null, null)));
        return new PortfolioEvaluation(1L, rows, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private void givenSchedules(String stockCode, DividendSchedule... schedules) {
        given(dividendSchedulePort.findCashDividends(stockCode, QUERY_FROM, QUERY_TO))
                .willReturn(List.of(schedules));
    }

    private static DividendSchedule paid(String paymentDate, String cashPerShare) {
        return new DividendSchedule(paymentDate == null ? null : LocalDate.parse(paymentDate),
                new BigDecimal(cashPerShare));
    }

    private PortfolioItem stock(StockSubType subType, String stockCode, String market, Integer quantity,
                                String evaluatedAmount, String dividendYield) {
        long id = nextId++;
        BigDecimal evaluated = new BigDecimal(evaluatedAmount);
        evaluatedById.put(id, evaluated);
        StockDetail detail = new StockDetail(subType, stockCode, market, null, null, quantity,
                BigDecimal.valueOf(10_000), dividendYield == null ? null : new BigDecimal(dividendYield),
                PriceCurrency.KRW, null);
        return item(id, AssetType.STOCK, evaluated, detail, null);
    }

    private PortfolioItem deposit(String principal, String interestRate) {
        CashDetail detail = new CashDetail(CashSubType.DEPOSIT, new BigDecimal(interestRate), null, null, null);
        return item(nextId++, AssetType.CASH, new BigDecimal(principal), null, detail);
    }

    private PortfolioItem item(long id, AssetType assetType, BigDecimal invested,
                               StockDetail stockDetail, CashDetail cashDetail) {
        return new PortfolioItem(id, 1L, "항목" + id, assetType, invested, false, Region.DOMESTIC, null,
                PortfolioItemStatus.ACTIVE, 0L, CREATED_AT, CREATED_AT,
                stockDetail, null, null, null, cashDetail, null, null);
    }
}
