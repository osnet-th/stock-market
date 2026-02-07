package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;

import java.util.List;
import java.util.Optional;

public interface NewsRepository {
    News save(News news);

    boolean insertIgnoreDuplicate(News news);

    Optional<News> findByOriginalUrl(String originalUrl);

    List<News> findByPurpose(NewsPurpose purpose);
}
