package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;

import java.util.List;
import java.util.Optional;

public interface NewsRepository {

    News save(News news);

    boolean insertIgnoreDuplicate(News news);

    Optional<News> findByOriginalUrl(String originalUrl);

    PageResult<News> findByKeywordId(Long keywordId, int page, int size);

    PageResult<News> findAll(int page, int size);

    void deleteByKeywordId(Long keywordId);

    void deleteByIds(List<Long> ids);
}
