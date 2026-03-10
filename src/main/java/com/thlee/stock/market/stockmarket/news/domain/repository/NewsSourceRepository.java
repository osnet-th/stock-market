package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSource;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;

import java.util.List;

public interface NewsSourceRepository {

    boolean insertIgnoreDuplicate(NewsSource newsSource);

    /**
     * 특정 사용자가 특정 URL 목록 중 이미 수집한 URL 조회
     */
    List<String> findExistingUrlsByUser(List<String> urls, Long userId);

    /**
     * purpose + sourceId 기반 뉴스 ID 목록 조회 (페이징)
     */
    PageResult<Long> findNewsIdsByPurposeAndSourceId(NewsPurpose purpose, Long sourceId, int page, int size);

    void deleteByPurposeAndSourceId(NewsPurpose purpose, Long sourceId);
}