package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import java.util.List;

/**
 * 요약 영역 응답: 핵심 지표 카드만 모은 경량 응답.
 */
public record SummaryResponse(String regionCode,
                              List<IndicatorCard> headlineCards,
                              String lastUpdatedAt,
                              String nextScheduledAt,
                              List<String> sources) {
}