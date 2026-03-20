package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;

import java.util.List;
import java.util.Optional;

/**
 * 키워드 Repository 인터페이스
 * 도메인 계층에 정의하여 인프라 계층이 구현
 */
public interface KeywordRepository {

    Keyword save(Keyword keyword);

    Optional<Keyword> findById(Long id);

    List<Keyword> findAll();

    Optional<Keyword> findByKeywordAndRegion(String keyword, Region region);

    boolean existsByKeywordAndRegion(String keyword, Region region);

    void delete(Keyword keyword);

    void deleteById(Long id);
}
