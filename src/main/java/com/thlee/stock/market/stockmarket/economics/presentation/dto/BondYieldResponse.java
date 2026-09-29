package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 날짜별 채권 기준수익률 응답.
 * status: FOUND(요청일 데이터) / FALLBACK(이전 날짜 적용) / NOT_FOUND(폴백 한도 안에 데이터 없음).
 * 수익률은 % 단위이고, null이면 출처가 제공하지 않은 값(미제공)이다.
 */
public record BondYieldResponse(
        String source,
        LocalDate requestedDate,
        LocalDate baseDate,
        String status,
        int maxFallbackDays,
        List<Item> yields
) {

    /** type: TREASURY / CORPORATE_PUBLIC_UNSECURED, grade: 회사채 등급 표기(국고채는 null) */
    public record Item(String type, String grade, int maturityMonths, BigDecimal rate) {
    }

    public static BondYieldResponse from(BondYieldLookup lookup, int maxFallbackDays) {
        List<Item> items = lookup.yields().stream()
                .map(y -> new Item(y.type().name(), y.grade() == null ? null : y.grade().getLabel(),
                        y.maturityMonths(), y.rate()))
                .toList();
        return new BondYieldResponse(lookup.source(), lookup.requestedDate(), lookup.baseDate(),
                lookup.status().name(), maxFallbackDays, items);
    }
}
