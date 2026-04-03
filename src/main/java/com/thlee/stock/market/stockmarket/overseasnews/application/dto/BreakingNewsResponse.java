package com.thlee.stock.market.stockmarket.overseasnews.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 해외속보 응답 DTO.
 * 프론트엔드에 노출되는 정제된 뉴스 항목.
 */
@Getter
@Builder
public class BreakingNewsResponse {

    private final String dateTime;       // 작성일시 (YYYY-MM-DD HH:MM:SS 형식)
    private final String title;          // 제목
    private final String source;         // 자료원
}