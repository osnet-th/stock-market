package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.ValuationInputs;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportValuation.DcfValuation;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * DCF 간이 모형.
 * 보수: 순현금 + FCF/r (성장 없음)
 * 낙관: 순현금 + Σ_{t=1..n} FCF·(1+g)^t/(1+r)^t + [FCF·(1+g)^n / r]/(1+r)^n (n년 이후 무성장 영구가치)
 */
@Component
public class DcfCalculator {

    private static final MathContext MC = MathContext.DECIMAL64;

    public DcfValuation calculate(ValuationInputs inputs, ValuationParams params, BigDecimal marketCap) {
        if (inputs == null || inputs.fcf() == null) {
            return notCalculable(inputs, marketCap, "잉여현금흐름(FCF) 데이터 없음");
        }
        if (inputs.fcf().signum() <= 0) {
            return notCalculable(inputs, marketCap, "FCF가 0 이하라 영구가치 계산 불가");
        }
        return calculateValues(inputs, params, marketCap);
    }

    private DcfValuation calculateValues(ValuationInputs inputs, ValuationParams params, BigDecimal marketCap) {
        BigDecimal netCash = orZero(inputs.netCash());
        BigDecimal conservative = netCash.add(inputs.fcf().divide(params.discountRate(), MC));
        BigDecimal optimistic = netCash.add(optimisticFirmValue(inputs.fcf(), params));
        return new DcfValuation(true, null, inputs.netCash(), inputs.fcf(),
                round(conservative), round(optimistic), marketCap,
                ratio(marketCap, conservative), ratio(marketCap, optimistic));
    }

    private BigDecimal optimisticFirmValue(BigDecimal fcf, ValuationParams params) {
        BigDecimal growth = BigDecimal.ONE.add(params.optimisticGrowthRate());
        BigDecimal discount = BigDecimal.ONE.add(params.discountRate());
        BigDecimal sum = BigDecimal.ZERO;
        for (int t = 1; t <= params.optimisticGrowthYears(); t++) {
            sum = sum.add(fcf.multiply(growth.pow(t)).divide(discount.pow(t), MC));
        }
        return sum.add(terminalValue(fcf, params, growth, discount));
    }

    private BigDecimal terminalValue(BigDecimal fcf, ValuationParams params, BigDecimal growth, BigDecimal discount) {
        int n = params.optimisticGrowthYears();
        BigDecimal terminalFcf = fcf.multiply(growth.pow(n));
        return terminalFcf.divide(params.discountRate(), MC).divide(discount.pow(n), MC);
    }

    private DcfValuation notCalculable(ValuationInputs inputs, BigDecimal marketCap, String reason) {
        BigDecimal netCash = inputs == null ? null : inputs.netCash();
        BigDecimal fcf = inputs == null ? null : inputs.fcf();
        return new DcfValuation(false, reason, netCash, fcf, null, null, marketCap, null, null);
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() <= 0) {
            return null;
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
