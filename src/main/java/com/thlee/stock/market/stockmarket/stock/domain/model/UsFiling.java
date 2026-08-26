package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * SEC 제출 서식 1건 (submissions recent 목록의 병렬 배열을 행으로 변환한 것)
 *
 * @param viewerUrl EDGAR 원문 문서 URL (구성 불가 시 null)
 */
public record UsFiling(
        String form,
        String filingDate,
        String accessionNumber,
        String primaryDocument,
        String description,
        String viewerUrl
) {}
