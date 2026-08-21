package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventKeyword;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventKeywordRepository;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventKeywordRow;
import com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence.mapper.NewsEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NewsEventKeyword 포트 어댑터.
 *
 * <p>{@link #replaceAll} 은 {@code deleteByEventId} 후 일괄 저장하는 단순한 정책.
 * 동일 트랜잭션 안에서 호출되어야 하며, 호출 측은 application 계층의
 * {@code @Transactional} 경계에서 본체 갱신과 묶어서 사용한다.
 */
@Repository
@RequiredArgsConstructor
public class NewsEventKeywordRepositoryImpl implements NewsEventKeywordRepository {

    private final NewsEventKeywordJpaRepository jpaRepository;

    @Override
    public List<NewsEventKeyword> findByEventId(Long eventId) {
        return jpaRepository.findByEventIdOrderByDisplayOrderAsc(eventId)
                .stream()
                .map(NewsEventMapper::toDomain)
                .toList();
    }

    @Override
    public Map<Long, List<NewsEventKeyword>> findAllByEventIds(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<NewsEventKeyword>> grouped = new LinkedHashMap<>(eventIds.size());
        for (NewsEventKeywordEntity e : jpaRepository.findByEventIdInOrderByEventIdAscDisplayOrderAsc(eventIds)) {
            grouped.computeIfAbsent(e.getEventId(), k -> new ArrayList<>()).add(NewsEventMapper.toDomain(e));
        }
        return grouped;
    }

    @Override
    public List<NewsEventKeywordRow> findRowsByUserId(Long userId) {
        return jpaRepository.findRowsByUserId(userId);
    }

    @Override
    public void replaceAll(Long eventId, List<NewsEventKeyword> keywords) {
        jpaRepository.deleteByEventId(eventId);
        if (keywords == null || keywords.isEmpty()) {
            return;
        }
        List<NewsEventKeywordEntity> entities = new ArrayList<>(keywords.size());
        for (NewsEventKeyword keyword : keywords) {
            entities.add(NewsEventMapper.toEntity(keyword));
        }
        List<NewsEventKeywordEntity> saved = jpaRepository.saveAll(entities);
        for (int i = 0; i < keywords.size(); i++) {
            NewsEventKeyword keyword = keywords.get(i);
            if (keyword.getId() == null) {
                keyword.assignId(saved.get(i).getId());
            }
        }
    }

    @Override
    public void deleteByEventId(Long eventId) {
        jpaRepository.deleteByEventId(eventId);
    }
}
