package com.thlee.stock.market.stockmarket.news.presentation.dto;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import lombok.Getter;

@Getter
public class NewsCollectResponse {
    private final int successCount;
    private final int ignoredCount;
    private final int failedCount;

    private NewsCollectResponse(int successCount, int ignoredCount, int failedCount) {
        this.successCount = successCount;
        this.ignoredCount = ignoredCount;
        this.failedCount = failedCount;
    }

    public static NewsCollectResponse from(NewsBatchSaveResult result) {
        return new NewsCollectResponse(
                result.getSuccessCount(),
                result.getIgnoredCount(),
                result.getFailedCount()
        );
    }
}