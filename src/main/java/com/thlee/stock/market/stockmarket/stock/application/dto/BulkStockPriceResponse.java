package com.thlee.stock.market.stockmarket.stock.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class BulkStockPriceResponse {

    private final Map<String, StockPriceResponse> prices;
}