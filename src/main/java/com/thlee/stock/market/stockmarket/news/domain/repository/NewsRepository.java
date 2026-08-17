package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsRepository {

    News save(News news);

    boolean insertIgnoreDuplicate(News news);

    Optional<News> findByOriginalUrl(String originalUrl);

    PageResult<News> findByKeywordId(Long keywordId, int page, int size);

    PageResult<News> findAll(int page, int size);

    /**
     * 여러 키워드의 최신 뉴스를 합쳐 size 건까지 반환 (#114 홈 키워드 피드).
     */
    List<News> findLatestByKeywordIds(List<Long> keywordIds, int size);

    /**
     * 여러 키워드의 since 이후 수집 건수 (#114).
     */
    long countByKeywordIdsSince(List<Long> keywordIds, LocalDateTime since);

    void deleteByKeywordId(Long keywordId);

    void deleteByIds(List<Long> ids);
}
