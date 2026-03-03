package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;

public interface KeywordNewsBatchService {
    /**
     * 활성화된 키워드 기반으로 뉴스를 조회하고 저장
     * @return 저장된 신규 뉴스 건수
     */
    int executeKeywordNewsBatch();

    /**
     * 단건 키워드 즉시 뉴스 수집
     * @param keywordId keyword.id (news 저장 시 source_id로 사용)
     * @param keyword   외부 뉴스 API 검색용 텍스트
     */
    NewsBatchSaveResult collectByKeyword(Long keywordId, String keyword, Long userId, Region region);
}