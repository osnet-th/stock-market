package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.ValuationInputs;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportValuation.CategoryLine;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportValuation.LiquidationValuation;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams.AdjustmentRatios;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 청산가치 = Σ(카테고리 장부가 × 조정비율) − 총부채.
 * 미분류 계정(otherNonCurrent 포함)은 0%로 이미 valuationInputs에 반영되어 있다.
 */
@Component
public class LiquidationValueCalculator {

    public LiquidationValuation calculate(ValuationInputs inputs, AdjustmentRatios ratios, BigDecimal marketCap) {
        if (inputs == null) {
            return null;
        }
        List<CategoryLine> lines = buildLines(inputs, ratios);
        BigDecimal adjustedTotal = sumAdjusted(lines);
        BigDecimal liquidationValue = subtractLiabilities(adjustedTotal, inputs.totalLiabilities());
        return new LiquidationValuation(
                inputs.baseYear(), lines, adjustedTotal, inputs.totalLiabilities(),
                liquidationValue, marketCap, ratio(marketCap, liquidationValue));
    }

    private List<CategoryLine> buildLines(ValuationInputs inputs, AdjustmentRatios ratios) {
        List<CategoryLine> lines = new ArrayList<>();
        lines.add(line("cash", "현금 및 현금성 자산", inputs.cash(), ratios.cash()));
        lines.add(line("securities", "유가증권(단기금융상품 등)", inputs.securities(), ratios.securities()));
        lines.add(line("receivables", "매출채권", inputs.receivables(), ratios.receivables()));
        lines.add(line("inventory", "재고자산", inputs.inventory(), ratios.inventory()));
        lines.add(line("investments", "투자 등", inputs.investments(), ratios.investments()));
        lines.add(line("tangible", "유형자산", inputs.tangible(), ratios.tangible()));
        lines.add(line("intangible", "무형자산", inputs.intangible(), ratios.intangible()));
        lines.add(line("otherCurrent", "기타 유동자산", inputs.otherCurrent(), ratios.otherCurrent()));
        lines.add(line("otherNonCurrent", "기타 비유동자산(미분류)", inputs.otherNonCurrent(), BigDecimal.ZERO));
        return lines;
    }

    private CategoryLine line(String key, String name, BigDecimal bookValue, BigDecimal ratio) {
        BigDecimal adjusted = bookValue == null ? null
                : bookValue.multiply(ratio).setScale(0, RoundingMode.HALF_UP);
        return new CategoryLine(key, name, bookValue, ratio, adjusted);
    }

    private BigDecimal sumAdjusted(List<CategoryLine> lines) {
        return lines.stream()
                .map(CategoryLine::adjustedValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal subtractLiabilities(BigDecimal adjustedTotal, BigDecimal totalLiabilities) {
        if (totalLiabilities == null) {
            return null;
        }
        return adjustedTotal.subtract(totalLiabilities);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() <= 0) {
            return null;
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
