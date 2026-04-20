package com.thlee.stock.market.stockmarket.logging.presentation.dto;

import java.util.List;

/**
 * 로그 인덱스 디스크 사용량 응답.
 * 플랜 "ES 디스크 사용률 > 85% 시 운영자 페이지 상단 배지 경고" 데이터 소스.
 */
public record LogDiskUsageResponse(
        long totalBytes,
        long totalDocs,
        List<IndexUsage> indices
) {

    public record IndexUsage(String index, long docs, long bytes) {
    }
}