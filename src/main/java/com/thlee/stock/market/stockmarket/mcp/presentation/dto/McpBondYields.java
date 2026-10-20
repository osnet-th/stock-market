package com.thlee.stock.market.stockmarket.mcp.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 채권 기준수익률 (S-RIM 요구수익률 후보). 금리는 % 단위다.
 *
 * @param status FOUND(요청일 값) / FALLBACK(이전 날짜 값 적용) / NOT_FOUND(최근 10일 안에 값 없음)
 */
public record McpBondYields(
        LocalDate requestedDate,
        LocalDate baseDate,
        String status,
        String source,
        String rateUnit,
        List<Item> yields
) {

    /** type: TREASURY(국고채) / CORPORATE_PUBLIC_UNSECURED(회사채 공모 무보증), grade: 회사채 등급(국고채는 null) */
    public record Item(String type, String grade, int maturityMonths, BigDecimal ratePercent) {
    }

    public static McpBondYields from(BondYieldLookup lookup) {
        List<Item> items = lookup.yields().stream()
                .map(y -> new Item(y.type().name(), y.grade() == null ? null : y.grade().getLabel(),
                        y.maturityMonths(), y.rate()))
                .toList();
        return new McpBondYields(lookup.requestedDate(), lookup.baseDate(), lookup.status().name(),
                lookup.source(), "%", items);
    }
}
