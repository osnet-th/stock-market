package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * NewsRepository 구현체 (Adapter)
 */
@Repository
@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepository {

    private final NewsJpaRepository newsJpaRepository;

    @Override
    public News save(News news) {
        newsJpaRepository.insertIgnoreDuplicate(
                news.getOriginalUrl(),
                news.getUserId(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getPurpose().name(),
                news.getSearchKeyword()
        );

        NewsEntity savedEntity = newsJpaRepository.findByOriginalUrl(news.getOriginalUrl())
                .orElseThrow(() -> new IllegalStateException("저장된 뉴스를 찾을 수 없습니다."));
        return NewsMapper.toDomain(savedEntity);
    }

    @Override
    public boolean insertIgnoreDuplicate(News news) {
        int inserted = newsJpaRepository.insertIgnoreDuplicate(
                news.getOriginalUrl(),
                news.getUserId(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getPurpose().name(),
                news.getSearchKeyword()
        );
        return inserted > 0;
    }

    @Override
    public Optional<News> findByOriginalUrl(String originalUrl) {
        return newsJpaRepository.findByOriginalUrl(originalUrl)
                .map(NewsMapper::toDomain);
    }

    @Override
    public List<News> findByPurpose(NewsPurpose purpose) {
        return newsJpaRepository.findByPurpose(purpose)
                .stream()
                .map(NewsMapper::toDomain)
                .collect(Collectors.toList());
    }
}
