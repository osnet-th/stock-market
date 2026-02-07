package com.thlee.stock.market.stockmarket.news.application.dto;

/**
 * 뉴스 배치 저장 결과 DTO
 */
public class NewsBatchSaveResult {
    private final int successCount;
    private final int ignoredCount;
    private final int failedCount;

    public NewsBatchSaveResult(int successCount, int ignoredCount, int failedCount) {
        this.successCount = successCount;
        this.ignoredCount = ignoredCount;
        this.failedCount = failedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getIgnoredCount() {
        return ignoredCount;
    }

    public int getFailedCount() {
        return failedCount;
    }
}
