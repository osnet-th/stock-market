package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;

public interface KeywordNewsBatchService {

    /**
     * 모든 키워드 기반으로 뉴스를 조회하고 저장
     * @return 저장된 신규 뉴스 건수
     */
    int executeKeywordNewsBatch();

    /**
     * 단건 키워드 즉시 뉴스 수집
     */
    NewsBatchSaveResult collectByKeyword(Long keywordId, String keyword, Region region);
}
