package com.thlee.stock.market.stockmarket.stockevaluation.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 현금배당 일정 한 건. 지급일이 아직 정해지지 않았으면 paymentDate 는 null 이다.
 * 주당 현금배당금은 항상 0보다 크다. 주식배당처럼 현금이 없는 일정은 이 모델로 만들지 않는다.
 */
public record DividendSchedule(LocalDate paymentDate, BigDecimal cashPerShare) {

    public DividendSchedule {
        if (cashPerShare == null || cashPerShare.signum() <= 0) {
            throw new IllegalArgumentException("주당 현금배당금은 0보다 커야 한다: " + cashPerShare);
        }
    }

    /** 지급일이 정해져 있고 from ~ to(양 끝 포함) 안이면 true */
    public boolean isPaidBetween(LocalDate from, LocalDate to) {
        return paymentDate != null && !paymentDate.isBefore(from) && !paymentDate.isAfter(to);
    }

    /** 보유 수량만큼 받는 현금배당 금액 */
    public BigDecimal amountFor(int quantity) {
        return cashPerShare.multiply(BigDecimal.valueOf(quantity));
    }
}
