package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventLinkRepository;
import com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence.mapper.NewsEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NewsEventLink 포트 어댑터.
 *
 * <p>{@link #replaceAll} 은 {@code deleteByEventId} 후 일괄 저장하는 단순한 정책.
 * 동일 트랜잭션 안에서 호출되어야 하며, 호출 측은 application 계층의
 * {@code @Transactional} 경계에서 본체 갱신과 묶어서 사용한다.
 */
@Repository
@RequiredArgsConstructor
public class NewsEventLinkRepositoryImpl implements NewsEventLinkRepository {

    private final NewsEventLinkJpaRepository jpaRepository;

    @Override
    public List<NewsEventLink> findByEventId(Long eventId) {
        return jpaRepository.findByEventIdOrderByDisplayOrderAsc(eventId)
                .stream()
                .map(NewsEventMapper::toDomain)
                .toList();
    }

    @Override
    public Map<Long, List<NewsEventLink>> findAllByEventIds(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<NewsEventLink>> grouped = new LinkedHashMap<>(eventIds.size());
        for (NewsEventLinkEntity e : jpaRepository.findByEventIdInOrderByEventIdAscDisplayOrderAsc(eventIds)) {
            grouped.computeIfAbsent(e.getEventId(), k -> new ArrayList<>()).add(NewsEventMapper.toDomain(e));
        }
        return grouped;
    }

    @Override
    public void replaceAll(Long eventId, List<NewsEventLink> links) {
        jpaRepository.deleteByEventId(eventId);
        if (links == null || links.isEmpty()) {
            return;
        }
        List<NewsEventLinkEntity> entities = new ArrayList<>(links.size());
        for (NewsEventLink link : links) {
            entities.add(NewsEventMapper.toEntity(link));
        }
        List<NewsEventLinkEntity> saved = jpaRepository.saveAll(entities);
        for (int i = 0; i < links.size(); i++) {
            NewsEventLink link = links.get(i);
            if (link.getId() == null) {
                link.assignId(saved.get(i).getId());
            }
        }
    }

    @Override
    public void deleteByEventId(Long eventId) {
        jpaRepository.deleteByEventId(eventId);
    }
}