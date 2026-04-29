package com.thlee.stock.market.stockmarket.newsjournal.application;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventDetailResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventListItemResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventListResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.exception.NewsEventNotFoundException;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryRepository;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventLinkRepository;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventListFilter;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사건 조회 유스케이스.
 *
 * <p>{@link #findList} 는 본체 페이지를 가져온 뒤 자식 링크를 {@code IN(eventIds)} 일괄 조회로
 * 매핑하여 N+1 을 회피한다. 카테고리는 사용자별 전체 목록 1회 조회 후 메모리 매핑으로 동봉한다.
 * {@link #findById} 는 권한 검증을 위해 {@code findByIdAndUserId} 패턴을 강제하며,
 * 미존재/타사용자 모두 {@link NewsEventNotFoundException} 으로 통일 (404 매핑).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsEventReadService {

    private final NewsEventRepository eventRepository;
    private final NewsEventLinkRepository linkRepository;
    private final NewsEventCategoryRepository categoryRepository;

    public NewsEventDetailResult findById(Long id, Long userId) {
        NewsEvent event = eventRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NewsEventNotFoundException(id));
        NewsEventCategory category = event.getCategoryId() == null
                ? null
                : categoryRepository.findByIdAndUserId(event.getCategoryId(), userId).orElse(null);
        List<NewsEventLink> links = linkRepository.findByEventId(id);
        return new NewsEventDetailResult(event, category, links);
    }

    public NewsEventListResult findList(Long userId, NewsEventListFilter filter) {
        List<NewsEvent> events = eventRepository.findList(userId, filter);
        long totalCount = eventRepository.countList(userId, filter);
        if (events.isEmpty()) {
            return new NewsEventListResult(List.of(), totalCount, filter.page(), filter.size());
        }
        List<Long> eventIds = new ArrayList<>(events.size());
        for (NewsEvent e : events) {
            eventIds.add(e.getId());
        }
        Map<Long, List<NewsEventLink>> linksByEventId = linkRepository.findAllByEventIds(eventIds);

        Map<Long, NewsEventCategory> categoryById = new HashMap<>();
        for (NewsEventCategory c : categoryRepository.findByUserIdOrderByNameAsc(userId)) {
            categoryById.put(c.getId(), c);
        }

        List<NewsEventListItemResult> items = new ArrayList<>(events.size());
        for (NewsEvent e : events) {
            List<NewsEventLink> links = linksByEventId.getOrDefault(e.getId(), List.of());
            NewsEventCategory category = e.getCategoryId() == null ? null : categoryById.get(e.getCategoryId());
            items.add(new NewsEventListItemResult(e, category, links));
        }
        return new NewsEventListResult(items, totalCount, filter.page(), filter.size());
    }
}