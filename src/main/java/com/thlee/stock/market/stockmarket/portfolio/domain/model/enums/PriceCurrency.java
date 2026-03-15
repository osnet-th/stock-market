package com.thlee.stock.market.stockmarket.portfolio.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PriceCurrency {
    KRW("원"),
    USD("달러"),
    JPY("엔"),
    CNY("위안"),
    HKD("홍콩달러"),
    VND("동");

    private final String label;
}