package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioIncomeResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 배당 · 이자 집계 (#110)
 *
 * 집계 기준:
 * - 주식 배당: 사용자가 입력한 시가배당률(dividendYield) × 평가액 → 연 예상
 * - 예적금·CMA 이자: 원금 × 금리 → 연 이자
 * - 채권 이자: 원금 × 표면금리 → 연 이자
 * - 이달 금액은 연 예상액의 1/12 (월 평균 환산). 실제 지급일 기준이 아니다.
 *
 * 배당률·금리가 입력되지 않은 항목은 집계에서 빠지고 excludedCount 로 노출된다.
 */
@Service
@Transactional(readOnly = true)
public class PortfolioIncomeService {

    private static final String BASIS_MONTHLY_AVERAGE = "ESTIMATED_MONTHLY_AVERAGE";
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    /**
     * 요약 API 가 이미 조회한 항목·평가 결과를 그대로 넘겨준다.
     * 평가는 시세 조회를 동반하므로 이 서비스가 따로 다시 수행하지 않는다 (#110 review M1).
     */
    public PortfolioIncomeResponse summarize(List<PortfolioItem> items, PortfolioEvaluation evaluation) {
        Map<Long, BigDecimal> evaluatedById = evaluatedAmountsById(evaluation);

        BigDecimal yearDividend = BigDecimal.ZERO;
        BigDecimal yearInterest = BigDecimal.ZERO;
        BigDecimal dividendBase = BigDecimal.ZERO;
        int excluded = 0;

        for (PortfolioItem item : items) {
            BigDecimal evaluated = evaluatedById.getOrDefault(item.getId(), item.getInvestedAmount());

            if (item.getAssetType() == AssetType.STOCK && item.getStockDetail() != null) {
                BigDecimal yield = item.getStockDetail().getDividendYield();
                if (isPositive(yield)) {
                    yearDividend = yearDividend.add(evaluated.multiply(yield).divide(PERCENT, 2, RoundingMode.HALF_UP));
                    dividendBase = dividendBase.add(evaluated);
                } else {
                    excluded++;
                }
                continue;
            }

            BigDecimal rate = resolveInterestRate(item);
            if (rate == null) {
                continue;
            }
            if (isPositive(rate)) {
                yearInterest = yearInterest.add(
                        item.getInvestedAmount().multiply(rate).divide(PERCENT, 2, RoundingMode.HALF_UP));
            } else {
                excluded++;
            }
        }

        BigDecimal yearEstimate = yearDividend.add(yearInterest);
        BigDecimal monthAmount = yearEstimate.divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP);
        BigDecimal dividendYield = dividendBase.compareTo(BigDecimal.ZERO) > 0
                ? yearDividend.multiply(PERCENT).divide(dividendBase, 2, RoundingMode.HALF_UP)
                : null;

        return new PortfolioIncomeResponse(
                monthAmount, yearEstimate, dividendYield, BASIS_MONTHLY_AVERAGE, excluded);
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

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
