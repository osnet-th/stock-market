package com.thlee.stock.market.stockmarket.news.application;

public interface KeywordNewsBatchService {
    /**
     * 활성화된 키워드 기반으로 뉴스를 조회하고 저장
     * @return 저장된 신규 뉴스 건수
     */
    int executeKeywordNewsBatch();
}