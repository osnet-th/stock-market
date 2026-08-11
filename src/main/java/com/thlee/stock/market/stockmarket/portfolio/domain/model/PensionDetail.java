package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PensionSubType;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 연금 계좌 상세 (#110)
 *
 * 시세 연동 대상이 아니라 평가액을 사용자가 직접 갱신한다.
 * evaluatedAmount 가 null 이면 평가 시 원금(investedAmount)으로 대체한다.
 */
@Getter
public class PensionDetail {
    private final PensionSubType subType;
    private final String provider;
    private final BigDecimal evaluatedAmount;
    private final BigDecimal monthlyDepositAmount;
    private final Integer depositDay;

    public PensionDetail(PensionSubType subType,
                         String provider,
                         BigDecimal evaluatedAmount,
                         BigDecimal monthlyDepositAmount,
                         Integer depositDay) {
        if (evaluatedAmount != null && evaluatedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("연금 평가액은 0 이상이어야 합니다.");
        }
        this.subType = subType;
        this.provider = provider;
        this.evaluatedAmount = evaluatedAmount;
        this.monthlyDepositAmount = monthlyDepositAmount;
        this.depositDay = depositDay;
    }
}
