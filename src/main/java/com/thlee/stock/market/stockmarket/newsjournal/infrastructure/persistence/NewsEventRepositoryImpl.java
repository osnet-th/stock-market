package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
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
                .findList(userId, filter.category(), filter.fromDate(), filter.toDate(), pageable)
                .stream()
                .map(NewsEventMapper::toDomain)
                .toList();
    }

    @Override
    public long countList(Long userId, NewsEventListFilter filter) {
        return jpaRepository.countList(userId, filter.category(), filter.fromDate(), filter.toDate());
    }

    @Override
    public void deleteByIdAndUserId(Long id, Long userId) {
        jpaRepository.deleteByIdAndUserId(id, userId);
    }
}