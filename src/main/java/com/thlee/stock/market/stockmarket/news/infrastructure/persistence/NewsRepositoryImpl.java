package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordDailyNewsCount;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordNewsCount;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper.NewsMapper;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository.NewsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getKeywordId(),
                news.getRegion() != null ? news.getRegion().name() : null,
                news.getSource()
        );

        NewsEntity savedEntity = newsJpaRepository.findByOriginalUrl(news.getOriginalUrl())
                .orElseThrow(() -> new IllegalStateException("저장된 뉴스를 찾을 수 없습니다."));
        return NewsMapper.toDomain(savedEntity);
    }

    @Override
    public boolean insertIgnoreDuplicate(News news) {
        int inserted = newsJpaRepository.insertIgnoreDuplicate(
                news.getOriginalUrl(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getKeywordId(),
                news.getRegion() != null ? news.getRegion().name() : null,
                news.getSource()
        );
        return inserted > 0;
    }

    @Override
    public Optional<News> findByOriginalUrl(String originalUrl) {
        return newsJpaRepository.findByOriginalUrl(originalUrl)
                .map(NewsMapper::toDomain);
    }

    @Override
    public PageResult<News> findByKeywordId(Long keywordId, int page, int size) {
        Page<NewsEntity> entityPage = newsJpaRepository
                .findByKeywordIdOrderByPublishedAtDesc(keywordId, PageRequest.of(page, size));

        List<News> newsList = entityPage.getContent().stream()
                .map(NewsMapper::toDomain)
                .collect(Collectors.toList());

        return new PageResult<>(newsList, page, size, entityPage.getTotalElements());
    }

    @Override
    public List<News> findLatestByKeywordIds(List<Long> keywordIds, int size) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return List.of();
        }
        return newsJpaRepository
                .findByKeywordIdInOrderByPublishedAtDesc(keywordIds, PageRequest.of(0, size))
                .stream()
                .map(NewsMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long countByKeywordIdsSince(List<Long> keywordIds, LocalDateTime since) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return 0L;
        }
        return newsJpaRepository.countByKeywordIdInAndCreatedAtGreaterThanEqual(keywordIds, since);
    }

    @Override
    public PageResult<News> findLatestByScope(NewsSearchCriteria criteria, Long userId) {
        if (criteria.hasNoKeywordScope()) {
            return new PageResult<>(List.of(), criteria.page(), criteria.size(), 0);
        }

        // 기간은 날짜로 받고 저장은 시각이라 하루의 시작/끝으로 넓힌다 (ES 경로와 같은 규칙)
        LocalDateTime startAt = criteria.startDate() != null
                ? criteria.startDate().atStartOfDay()
                : null;
        LocalDateTime endAt = criteria.endDate() != null
                ? criteria.endDate().atTime(LocalTime.MAX)
                : null;

        Page<NewsEntity> entityPage = newsJpaRepository.findLatestByScope(
                criteria.keywordIds(),
                startAt,
                endAt,
                criteria.region(),
                criteria.unreadOnly(),
                userId,
                PageRequest.of(criteria.page(), criteria.size())
        );

        List<News> newsList = entityPage.getContent().stream()
                .map(NewsMapper::toDomain)
                .collect(Collectors.toList());

        return new PageResult<>(newsList, criteria.page(), criteria.size(), entityPage.getTotalElements());
    }

    @Override
    public List<News> findByOriginalUrls(List<String> originalUrls) {
        if (originalUrls == null || originalUrls.isEmpty()) {
            return List.of();
        }
        return newsJpaRepository.findByOriginalUrlIn(originalUrls).stream()
                .map(NewsMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<KeywordNewsCount> aggregateCountsByKeywordIds(List<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return List.of();
        }
        return newsJpaRepository.aggregateCountsByKeywordIds(keywordIds);
    }

    @Override
    public List<KeywordDailyNewsCount> aggregateDailyCountsByKeywordIds(List<Long> keywordIds, LocalDateTime since) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return List.of();
        }
        return newsJpaRepository.aggregateDailyCountsByKeywordIds(keywordIds, since);
    }

    @Override
    public PageResult<News> findAll(int page, int size) {
        Page<NewsEntity> entityPage = newsJpaRepository.findAll(PageRequest.of(page, size));

        List<News> newsList = entityPage.getContent().stream()
                .map(NewsMapper::toDomain)
                .collect(Collectors.toList());

        return new PageResult<>(newsList, page, size, entityPage.getTotalElements());
    }

    @Override
    public int reassignKeywordId(Long oldKeywordId, Long newKeywordId) {
        return newsJpaRepository.reassignKeywordId(oldKeywordId, newKeywordId);
    }

    @Override
    public List<News> findAllByKeywordId(Long keywordId) {
        return newsJpaRepository.findAllByKeywordId(keywordId).stream()
                .map(NewsMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByKeywordId(Long keywordId) {
        newsJpaRepository.deleteByKeywordId(keywordId);
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        newsJpaRepository.deleteAllByIdInBatch(ids);
    }
}
