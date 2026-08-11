package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioIncomeResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioSnapshotResponse;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioSummaryResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioSnapshot;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockPurchaseHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioSnapshotRepository;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockPurchaseHistoryRepository;
import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 포트폴리오 상단 요약 + 일자별 스냅샷 (#110)
 *
 * - 스냅샷: 사용자가 [스냅샷 저장]을 누른 시점의 총평가/총원금을 하루 한 건 기록한다 (같은 날은 덮어쓰기).
 * - 요약: 누적 수익률·보유일수·CAGR·환차손익 + 배당/이자.
 *
 * 평가(evaluatePortfolios)는 시세 조회를 동반하므로 요약 1회 요청당 한 번만 수행하고,
 * 배당·이자 집계에도 그 결과를 넘겨 재사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioSummaryService {

    /** CAGR 은 보유기간이 짧으면 값이 폭주해 의미가 없다. 최소 보유일수. */
    private static final long MIN_CAGR_HOLDING_DAYS = 30L;
    private static final int SNAPSHOT_MAX_MONTHS = 60;

    private final PortfolioEvaluationService portfolioEvaluationService;
    private final PortfolioIncomeService portfolioIncomeService;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioItemRepository portfolioItemRepository;
    private final StockPurchaseHistoryRepository purchaseHistoryRepository;
    private final StockPriceService stockPriceService;

    /**
     * 오늘자 스냅샷 저장. 같은 날 이미 있으면 금액만 갱신한다.
     */
    @Transactional
    public PortfolioSnapshotResponse saveTodaySnapshot(Long userId) {
        PortfolioEvaluation evaluation = evaluate(userId);
        LocalDate today = LocalDate.now();

        PortfolioSnapshot snapshot = snapshotRepository
                .findByUserIdAndSnapshotDate(userId, today)
                .map(existing -> {
                    existing.refresh(evaluation.getTotalEvaluated(), evaluation.getTotalInvested());
                    return existing;
                })
                .orElseGet(() -> PortfolioSnapshot.create(
                        userId, today, evaluation.getTotalEvaluated(), evaluation.getTotalInvested()));

        return PortfolioSnapshotResponse.from(snapshotRepository.save(snapshot));
    }

    /**
     * 최근 N개월 스냅샷 (날짜 오름차순)
     */
    public List<PortfolioSnapshotResponse> getSnapshots(Long userId, int months) {
        int safeMonths = Math.max(1, Math.min(months, SNAPSHOT_MAX_MONTHS));
        LocalDate from = LocalDate.now().minusMonths(safeMonths);
        return snapshotRepository.findByUserIdSince(userId, from).stream()
                .map(PortfolioSnapshotResponse::from)
                .collect(Collectors.toList());
    }

    public PortfolioSummaryResponse getSummary(Long userId) {
        PortfolioEvaluation evaluation = evaluate(userId);
        BigDecimal totalEvaluated = evaluation.getTotalEvaluated();
        BigDecimal totalInvested = evaluation.getTotalInvested();
        BigDecimal profit = totalEvaluated.subtract(totalInvested);

        BigDecimal profitRate = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? profit.multiply(BigDecimal.valueOf(100)).divide(totalInvested, 2, RoundingMode.HALF_UP)
                : null;

        List<PortfolioItem> items = portfolioItemRepository.findByUserId(userId);
        Map<Long, List<StockPurchaseHistory>> historiesByItem = loadPurchaseHistories(items);

        Long holdingDays = resolveHoldingDays(historiesByItem);
        BigDecimal cagr = calculateCagr(totalEvaluated, totalInvested, holdingDays);
        FxProfit fx = calculateFxProfit(items, historiesByItem);
        PortfolioIncomeResponse income = portfolioIncomeService.summarize(items, evaluation);

        return new PortfolioSummaryResponse(
                totalEvaluated, totalInvested, profit, profitRate,
                holdingDays, cagr, fx.amount(), fx.unknownCount(), income
        );
    }

    private PortfolioEvaluation evaluate(Long userId) {
        PortfolioEvaluation evaluation = portfolioEvaluationService.evaluatePortfolios(List.of(userId)).get(userId);
        if (evaluation != null) {
            return evaluation;
        }
        return new PortfolioEvaluation(userId, List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * 주식 항목의 매수 이력을 한 번에 조회해 itemId 로 그룹핑 (N+1 방지)
     */
    private Map<Long, List<StockPurchaseHistory>> loadPurchaseHistories(List<PortfolioItem> items) {
        List<Long> stockItemIds = items.stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK)
                .map(PortfolioItem::getId)
                .collect(Collectors.toList());
        if (stockItemIds.isEmpty()) {
            return Map.of();
        }
        return purchaseHistoryRepository.findByPortfolioItemIdIn(stockItemIds).stream()
                .collect(Collectors.groupingBy(StockPurchaseHistory::getPortfolioItemId));
    }

    /**
     * 최초 매수일부터 오늘까지의 일수. 매수 이력이 없는 수기 자산만 있으면 null.
     */
    private Long resolveHoldingDays(Map<Long, List<StockPurchaseHistory>> historiesByItem) {
        Optional<LocalDate> firstPurchase = historiesByItem.values().stream()
                .flatMap(List::stream)
                .map(StockPurchaseHistory::getPurchasedAt)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo);

        return firstPurchase
                .map(date -> ChronoUnit.DAYS.between(date, LocalDate.now()))
                .filter(days -> days > 0)
                .orElse(null);
    }

    /**
     * CAGR = (평가액 / 원금)^(365 / 보유일수) − 1
     * 보유기간이 짧거나 원금이 0이면 null.
     */
    private BigDecimal calculateCagr(BigDecimal totalEvaluated, BigDecimal totalInvested, Long holdingDays) {
        if (holdingDays == null || holdingDays < MIN_CAGR_HOLDING_DAYS) {
            return null;
        }
        if (totalInvested.compareTo(BigDecimal.ZERO) <= 0 || totalEvaluated.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        double ratio = totalEvaluated.divide(totalInvested, 10, RoundingMode.HALF_UP).doubleValue();
        double years = holdingDays / 365.0;
        double cagr = (Math.pow(ratio, 1.0 / years) - 1.0) * 100.0;
        if (Double.isNaN(cagr) || Double.isInfinite(cagr)) {
            return null;
        }
        return BigDecimal.valueOf(cagr).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 환차손익 = Σ 수량 × 매수단가 × (현재환율 − 매수환율)
     * 매수 환율이 기록되지 않은 이력(#110 이전 데이터)은 제외하고 건수만 센다.
     */
    private FxProfit calculateFxProfit(List<PortfolioItem> items,
                                       Map<Long, List<StockPurchaseHistory>> historiesByItem) {
        List<PortfolioItem> overseasItems = items.stream()
                .filter(item -> item.getAssetType() == AssetType.STOCK && item.getStockDetail() != null)
                .filter(item -> {
                    PriceCurrency currency = item.getStockDetail().getPriceCurrency();
                    return currency != null && currency != PriceCurrency.KRW;
                })
                .collect(Collectors.toList());
        if (overseasItems.isEmpty()) {
            return new FxProfit(BigDecimal.ZERO, 0);
        }

        Map<PriceCurrency, BigDecimal> fxByCurrency = fetchFxRatesByCurrency(overseasItems);

        BigDecimal total = BigDecimal.ZERO;
        int unknown = 0;
        for (PortfolioItem item : overseasItems) {
            BigDecimal currentFx = fxByCurrency.get(item.getStockDetail().getPriceCurrency());
            for (StockPurchaseHistory history : historiesByItem.getOrDefault(item.getId(), List.of())) {
                if (history.getFxRate() == null) {
                    unknown++;
                    continue;
                }
                if (currentFx == null) {
                    continue;
                }
                BigDecimal foreignCost = history.getPurchasePrice()
                        .multiply(BigDecimal.valueOf(history.getQuantity()));
                total = total.add(foreignCost.multiply(currentFx.subtract(history.getFxRate())));
            }
        }
        return new FxProfit(total.setScale(2, RoundingMode.HALF_UP), unknown);
    }

    /**
     * 환율은 종목이 아니라 통화 단위 값이다.
     * 통화별로 종목 하나만 시세를 조회해 환율을 얻고 같은 통화의 나머지 항목에 재사용한다.
     *
     * 대표 종목 하나만 시도하면 그 종목 조회가 실패했을 때 해당 통화 전체가 환차손익에서 빠지므로,
     * 성공할 때까지 같은 통화의 다음 종목으로 넘어간다 (#110 review R4).
     */
    private Map<PriceCurrency, BigDecimal> fetchFxRatesByCurrency(List<PortfolioItem> overseasItems) {
        Map<PriceCurrency, List<StockDetail>> byCurrency = new LinkedHashMap<>();
        for (PortfolioItem item : overseasItems) {
            byCurrency.computeIfAbsent(item.getStockDetail().getPriceCurrency(), key -> new ArrayList<>())
                    .add(item.getStockDetail());
        }

        Map<PriceCurrency, BigDecimal> rates = new HashMap<>();
        byCurrency.forEach((currency, details) -> {
            for (StockDetail detail : details) {
                BigDecimal rate = fetchCurrentFxRate(detail);
                if (rate != null) {
                    rates.put(currency, rate);
                    return;
                }
            }
        });
        return rates;
    }

    private BigDecimal fetchCurrentFxRate(StockDetail detail) {
        try {
            StockPriceResponse price = stockPriceService.getPrice(
                    detail.getStockCode(),
                    MarketType.valueOf(detail.getMarket()),
                    ExchangeCode.valueOf(detail.getExchangeCode()));
            return price != null ? price.getExchangeRateValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private record FxProfit(BigDecimal amount, int unknownCount) {
    }
}
