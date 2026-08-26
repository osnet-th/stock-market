package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import java.util.List;

/**
 * 탭 단위 응답. 누락 시 cards 빈 컬렉션 + lastUpdatedAt = null + nextScheduledAt 노출.
 */
public record TabResponse(String regionCode,
                          String category,
                          String period,
                          List<IndicatorCard> cards,
                          String lastUpdatedAt,
                          String nextScheduledAt,
                          List<String> sources) {
}