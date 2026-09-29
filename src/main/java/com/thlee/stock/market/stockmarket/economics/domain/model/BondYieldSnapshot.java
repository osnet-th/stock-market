package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * 기준일 하루치 기준수익률.
 */
public record BondYieldSnapshot(LocalDate baseDate, List<BondYield> yields) {

    public BondYieldSnapshot {
        Objects.requireNonNull(baseDate, "baseDate");
        yields = yields == null ? List.of() : List.copyOf(yields);
    }

    public static BondYieldSnapshot empty(LocalDate baseDate) {
        return new BondYieldSnapshot(baseDate, List.of());
    }

    /**
     * 날짜 전체 데이터 없음(빈 날짜). 제공된 수익률이 하나도 없을 때만 해당한다.
     * 일부 등급·만기만 비어 있는 부분 결측은 빈 날짜가 아니므로 날짜 폴백 사유가 아니다.
     */
    public boolean isEmpty() {
        return yields.stream().noneMatch(BondYield::isProvided);
    }
}
