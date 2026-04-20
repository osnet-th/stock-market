package com.thlee.stock.market.stockmarket.logging.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 로그 검색 응답. Document 필드를 그대로 누출하지 않고 선별해 제공.
 * {@code nextSearchAfter} 가 null 이면 마지막 페이지.
 */
public record LogSearchResponse(
        List<Item> items,
        long total,
        List<Object> nextSearchAfter
) {

    public record Item(
            String id,
            Instant timestamp,
            String domain,
            Long userId,
            String requestId,
            Integer status,
            String exceptionClass,
            Map<String, Object> payload,
            boolean truncated,
            Integer originalSize
    ) {
    }
}