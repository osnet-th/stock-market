package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioIncomeResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.DividendSchedule;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.service.DividendSchedulePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 배당 · 이자 집계
 *
 * 집계 기준:
 * - 국내 주식(개별 종목·ETF): KSD 배당일정의 지급일 기준. 이달은 지급일이 이번 달(KST)인 현금배당 × 현재 수량,
 *   연 예상은 이번 달을 포함한 최근 12개월에 지급된 현금배당 × 현재 수량이다.
 * - 해외 주식, KSD 배당 기록이 없는 ETF: 사용자가 입력한 시가배당률(dividendYield) × 평가액 → 연 예상, 이달은 1/12
 * - 예적금·CMA 이자: 원금 × 금리, 채권 이자: 원금 × 표면금리 → 연 이자, 이달은 1/12
 *
 * 국내 종목 중 하나라도 배당일정 조회에 실패하면 국내 주식 전부를 입력 배당률 기준으로 계산하고
 * basis 를 ESTIMATED_MONTHLY_AVERAGE 로 돌려준다. 한 숫자에 실지급과 추정이 섞이지 않게 하기 위해서다.
 *
 * 배당률·금리가 입력되지 않아 계산하지 못한 항목은 집계에서 빠지고 excludedCount 로 노출된다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class PortfolioIncomeService {

    private static final String BASIS_ACTUAL_PAYMENT_DATE = "ACTUAL_PAYMENT_DATE";
    private static final String BASIS_MONTHLY_AVERAGE = "ESTIMATED_MONTHLY_AVERAGE";
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    /** 연 예상에 넣는 지급 기간(이번 달 포함, 개월) */
    private static final int YEAR_PERIOD_MONTHS = 12;
    /**
     * 조회 시작을 연 예상 기간보다 앞당기는 개월 수. 출처의 기간 필터가 기준일 기준이어도
     * 기준일보다 최대 이만큼 늦게 지급된 배당이 연 예상에서 빠지지 않는다.
     */
    private static final int QUERY_LEAD_MONTHS = 6;

    private final DividendSchedulePort dividendSchedulePort;
    private final Clock clock;

    @Autowired
    public PortfolioIncomeService(DividendSchedulePort dividendSchedulePort) {
        this(dividendSchedulePort, Clock.system(KST));
    }

    PortfolioIncomeService(DividendSchedulePort dividendSchedulePort, Clock clock) {
        this.dividendSchedulePort = dividendSchedulePort;
        this.clock = clock;
    }

    /**
     * 요약 API 가 이미 조회한 항목·평가 결과를 그대로 넘겨준다.
     * 평가는 시세 조회를 동반하므로 이 서비스가 따로 다시 수행하지 않는다 (#110 review M1).
     */
    public PortfolioIncomeResponse summarize(List<PortfolioItem> items, PortfolioEvaluation evaluation) {
        Map<Long, BigDecimal> evaluatedById = evaluatedAmountsById(evaluation);
        IncomePeriod period = IncomePeriod.of(YearMonth.now(clock));
        DomesticSchedules schedules = loadDomesticSchedules(items, period);
        Totals totals = new Totals();

        for (PortfolioItem item : items) {
            BigDecimal evaluated = evaluatedById.getOrDefault(item.getId(), item.getInvestedAmount());

            if (item.getAssetType() == AssetType.STOCK && item.getStockDetail() != null) {
                addStockDividend(item, evaluated, schedules, period, totals);
                continue;
            }

            BigDecimal rate = resolveInterestRate(item);
            if (rate == null) {
                continue;
            }
            if (isPositive(rate)) {
                totals.addInterest(item.getInvestedAmount().multiply(rate).divide(PERCENT, 2, RoundingMode.HALF_UP));
            } else {
                totals.exclude();
            }
        }

        return totals.toResponse(schedules.failed() ? BASIS_MONTHLY_AVERAGE : BASIS_ACTUAL_PAYMENT_DATE);
    }

    /**
     * 국내 주식의 배당 일정을 종목코드마다 한 번씩 조회한다.
     * 하나라도 실패하면 남은 조회를 멈추고 실패로 돌려준다. 국내 주식 전부가 입력 배당률 기준으로 바뀌므로
     * 더 조회해도 쓰이지 않는다.
     */
    private DomesticSchedules loadDomesticSchedules(List<PortfolioItem> items, IncomePeriod period) {
        Map<String, List<DividendSchedule>> byStockCode = new HashMap<>();
        for (PortfolioItem item : items) {
            StockDetail detail = domesticStockDetail(item);
            if (detail == null || byStockCode.containsKey(detail.getStockCode())) {
                continue;
            }
            try {
                byStockCode.put(detail.getStockCode(), dividendSchedulePort.findCashDividends(
                        detail.getStockCode(), period.queryFrom(), period.monthEnd()));
            } catch (Exception e) {
                log.warn("배당 일정 조회에 실패해 국내 주식 배당을 입력 배당률 기준으로 계산합니다: stockCode={}",
                        detail.getStockCode(), e);
                return DomesticSchedules.failure();
            }
        }
        return new DomesticSchedules(byStockCode, false);
    }

    /**
     * 지급일 기준으로 계산할 수 있으면 실지급 배당을, 아니면 입력 배당률 기준 배당을 더한다.
     * KSD 배당 기록이 없는 ETF 는 분배금이 KSD 에 나오지 않을 수 있어 입력 배당률 기준으로 계산한다.
     */
    private void addStockDividend(PortfolioItem item, BigDecimal evaluated, DomesticSchedules schedules,
                                  IncomePeriod period, Totals totals) {
        StockDetail detail = item.getStockDetail();
        List<DividendSchedule> paid = domesticStockDetail(item) != null ? schedules.of(detail.getStockCode()) : null;
        boolean etfWithoutRecords = paid != null && paid.isEmpty() && detail.getSubType() == StockSubType.ETF;

        if (paid != null && !etfWithoutRecords) {
            int quantity = detail.getQuantity() != null ? detail.getQuantity() : 0;
            totals.addPaidDividend(
                    sumPaid(paid, quantity, period.monthStart(), period.monthEnd()),
                    sumPaid(paid, quantity, period.yearStart(), period.monthEnd()),
                    evaluated);
            return;
        }

        BigDecimal yield = detail.getDividendYield();
        if (isPositive(yield)) {
            totals.addEstimatedDividend(evaluated.multiply(yield).divide(PERCENT, 2, RoundingMode.HALF_UP), evaluated);
        } else {
            totals.exclude();
        }
    }

    private BigDecimal sumPaid(List<DividendSchedule> schedules, int quantity, LocalDate from, LocalDate to) {
        return schedules.stream()
                .filter(schedule -> schedule.isPaidBetween(from, to))
                .map(schedule -> schedule.amountFor(quantity))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * KSD 로 조회할 국내 주식이면 주식 상세를, 아니면 null 을 돌려준다.
     * 시장 값이 없거나 해석할 수 없으면 해외 주식과 같이 본다. 종목코드가 비어 있으면 조회하지 않는다.
     * 빈 종목코드로 조회하면 출처가 전체 종목 일정을 돌려준다.
     */
    private StockDetail domesticStockDetail(PortfolioItem item) {
        StockDetail detail = item.getStockDetail();
        if (item.getAssetType() != AssetType.STOCK || detail == null) {
            return null;
        }
        if (detail.getStockCode() == null || detail.getStockCode().isBlank()) {
            return null;
        }
        return isDomesticMarket(detail.getMarket()) ? detail : null;
    }

    private boolean isDomesticMarket(String market) {
        if (market == null) {
            return false;
        }
        try {
            return MarketType.valueOf(market).isDomestic();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 이자율이 존재할 수 있는 자산군만 값을 돌려준다.
     * 대상이 아닌 자산군(부동산·금 등)은 null 을 돌려 excludedCount 에서 제외한다.
     */
    private BigDecimal resolveInterestRate(PortfolioItem item) {
        if (item.getAssetType() == AssetType.CASH) {
            return item.getCashDetail() != null ? nullToZero(item.getCashDetail().getInterestRate()) : BigDecimal.ZERO;
        }
        if (item.getAssetType() == AssetType.BOND) {
            return item.getBondDetail() != null ? nullToZero(item.getBondDetail().getCouponRate()) : BigDecimal.ZERO;
        }
        return null;
    }

    private Map<Long, BigDecimal> evaluatedAmountsById(PortfolioEvaluation evaluation) {
        if (evaluation == null) {
            return Map.of();
        }
        return evaluation.getItems().stream()
                .collect(Collectors.toMap(ItemEvaluation::getPortfolioItemId,
                        ItemEvaluation::getEvaluatedAmount,
                        (a, b) -> a));
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 지급일 기준 집계 기간. 모두 KST 달력 기준이다.
     * 조회는 연 예상 기간 시작보다 앞에서 시작하고, 이달·연 예상은 지급일로 다시 거른다.
     */
    private record IncomePeriod(LocalDate queryFrom, LocalDate yearStart, LocalDate monthStart, LocalDate monthEnd) {

        static IncomePeriod of(YearMonth month) {
            LocalDate yearStart = month.minusMonths(YEAR_PERIOD_MONTHS - 1).atDay(1);
            return new IncomePeriod(yearStart.minusMonths(QUERY_LEAD_MONTHS), yearStart,
                    month.atDay(1), month.atEndOfMonth());
        }
    }

    /** 국내 주식 배당 일정 조회 결과. 실패했으면 어떤 종목도 지급일 기준으로 계산하지 않는다. */
    private record DomesticSchedules(Map<String, List<DividendSchedule>> byStockCode, boolean failed) {

        static DomesticSchedules failure() {
            return new DomesticSchedules(Map.of(), true);
        }

        /** 지급일 기준으로 계산할 일정. 실패했으면 null */
        List<DividendSchedule> of(String stockCode) {
            return failed ? null : byStockCode.get(stockCode);
        }
    }

    /**
     * 집계 중간값. 지급일 기준 배당은 이달 금액을 그대로 쓰고,
     * 입력 배당률 기준 배당과 이자는 연 금액을 12로 나눠 이달에 더한다.
     */
    private static final class Totals {
        private BigDecimal monthPaidDividend = BigDecimal.ZERO;
        private BigDecimal yearPaidDividend = BigDecimal.ZERO;
        private BigDecimal yearEstimatedDividend = BigDecimal.ZERO;
        private BigDecimal yearInterest = BigDecimal.ZERO;
        /** 연 배당이 0보다 큰 주식 항목의 평가액 합 (시가배당률 분모) */
        private BigDecimal dividendBase = BigDecimal.ZERO;
        private int excluded;

        void addPaidDividend(BigDecimal month, BigDecimal year, BigDecimal evaluated) {
            monthPaidDividend = monthPaidDividend.add(month);
            yearPaidDividend = yearPaidDividend.add(year);
            if (isPositive(year)) {
                dividendBase = dividendBase.add(evaluated);
            }
        }

        void addEstimatedDividend(BigDecimal year, BigDecimal evaluated) {
            yearEstimatedDividend = yearEstimatedDividend.add(year);
            dividendBase = dividendBase.add(evaluated);
        }

        void addInterest(BigDecimal year) {
            yearInterest = yearInterest.add(year);
        }

        void exclude() {
            excluded++;
        }

        PortfolioIncomeResponse toResponse(String basis) {
            BigDecimal yearDividend = yearPaidDividend.add(yearEstimatedDividend);
            BigDecimal yearEstimate = yearDividend.add(yearInterest);
            BigDecimal monthAverage = yearEstimatedDividend.add(yearInterest)
                    .divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP);
            BigDecimal monthAmount = monthPaidDividend.add(monthAverage).setScale(2, RoundingMode.HALF_UP);
            BigDecimal dividendYield = dividendBase.compareTo(BigDecimal.ZERO) > 0
                    ? yearDividend.multiply(PERCENT).divide(dividendBase, 2, RoundingMode.HALF_UP)
                    : null;
            return new PortfolioIncomeResponse(monthAmount, yearEstimate, dividendYield, basis, excluded);
        }
    }
}
