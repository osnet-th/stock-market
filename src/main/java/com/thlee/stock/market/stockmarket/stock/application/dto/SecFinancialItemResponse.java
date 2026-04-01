package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class SecFinancialItemResponse {
    private final String label;
    private final String labelEn;
    private final Map<Integer, Long> values;

    public static SecFinancialItemResponse from(SecFinancialItem item) {
        return new SecFinancialItemResponse(
                item.label(),
                item.labelEn(),
                item.values()
        );
    }
}