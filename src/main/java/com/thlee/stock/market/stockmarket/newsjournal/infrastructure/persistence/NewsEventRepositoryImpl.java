package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryCount;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventImpactCount;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventListFilter;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventRepository;
import com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence.mapper.NewsEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * NewsEvent 포트 어댑터.
 *
 * <p>{@link #findList} / {@link #countList} 는 {@link NewsEventJpaRepository}의
 * JPQL 옵셔널 필터 쿼리를 호출하며, 정렬은 {@code occurred_date DESC, id DESC} 로 안정 정렬.
 */
@Repository
@RequiredArgsConstructor
public class NewsEventRepositoryImpl implements NewsEventRepository {

    private final NewsEventJpaRepository jpaRepository;

    @Override
    public NewsEvent save(NewsEvent event) {
        NewsEventEntity entity = NewsEventMapper.toEntity(event);
        NewsEventEntity saved = jpaRepository.save(entity);
        if (event.getId() == null) {
            event.assignId(saved.getId());
        }
        return NewsEventMapper.toDomain(saved);
    }

    @Override
    public Optional<NewsEvent> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(NewsEventMapper::toDomain);
    }

    @Override
    public List<NewsEvent> findList(Long userId, NewsEventListFilter filter) {
        Pageable pageable = PageRequest.of(filter.page(), filter.size());
        return jpaRepository
                .findList(userId, filter.impact(), filter.categoryId(),
                        filter.fromDate(), filter.toDate(),
                        toLikePattern(filter.query()), toKeywordsParam(filter.keywords()),
                        filter.keywords().size(), pageable)
                .stream()
                .map(NewsEventMapper::toDomain)
                .toList();
    }

    @Override
    public long countList(Long userId, NewsEventListFilter filter) {
        return jpaRepository.countList(userId, filter.impact(), filter.categoryId(),
                filter.fromDate(), filter.toDate(),
                toLikePattern(filter.query()), toKeywordsParam(filter.keywords()),
                filter.keywords().size());
    }

    /** 통합 검색어 → 소문자 부분 일치 LIKE 패턴. 와일드카드는 `!` 로 escape. null = 비활성. */
    private static String toLikePattern(String query) {
        if (query == null) {
            return null;
        }
        String escaped = query.toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    /**
     * 키워드 필터 비활성 시 빈 IN 리스트가 JPQL 에 렌더링되지 않도록 절대 매칭 불가
     * sentinel(빈 문자열 — 키워드는 NotBlank) 1건으로 치환. {@code keywordCount=0} 가드가
     * 논리를 이미 무효화하므로 결과에는 영향 없다.
     */
    private static List<String> toKeywordsParam(List<String> keywords) {
        return keywords.isEmpty() ? List.of("") : keywords;
    }

    @Override
    public void deleteByIdAndUserId(Long id, Long userId) {
        jpaRepository.deleteByIdAndUserId(id, userId);
    }

    @Override
    public List<NewsEvent> findRecentByUserId(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return jpaRepository.findRecentByUserId(userId, pageable)
                .stream()
                .map(NewsEventMapper::toDomain)
                .toList();
    }

    @Override
    public List<NewsEventCategoryCount> countByCategoryGroupedByCategoryId(Long userId) {
        return jpaRepository.countByCategoryGroupedByCategoryId(userId);
    }

    @Override
    public List<NewsEventImpactCount> countByImpactGroupedByImpact(Long userId) {
        return jpaRepository.countByImpactGroupedByImpact(userId);
    }
}