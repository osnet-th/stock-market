package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto;

import java.util.List;
import java.util.Map;

public record ParsedTable(
    Map<String, Integer> headerIndex,
    List<RawTableRow> rows
) {}