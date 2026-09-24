package com.thlee.stock.market.stockmarket.newsjournal.application;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventDetailResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventListItemResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventListResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsJournalStatsResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsJournalSummaryResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.exception.NewsEventNotFoundException;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventKeyword;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryCount;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryRepository;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventImpactCount;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventKeywordRepository;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventKeywordRow;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventLinkRepository;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventListFilter;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final NewsEventKeywordRepository keywordRepository;
    private final NewsEventCategoryRepository categoryRepository;

    public NewsEventDetailResult findById(Long id, Long userId) {
        NewsEvent event = eventRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NewsEventNotFoundException(id));
        NewsEventCategory category = event.getCategoryId() == null
                ? null
                : categoryRepository.findByIdAndUserId(event.getCategoryId(), userId).orElse(null);
        List<NewsEventLink> links = linkRepository.findByEventId(id);
        List<NewsEventKeyword> keywords = keywordRepository.findByEventId(id);
        return new NewsEventDetailResult(event, category, links, keywords);
    }

    /**
     * 대시보드 요약(최근 등록 N건 + 카테고리별 사건 건수).
     *
     * <p>카테고리명은 사용자 카테고리 목록 1회 조회 후 메모리 join 으로 채운다.
     * 카테고리가 삭제된 후 사건만 남은 그룹은 결과에서 제외(이름 매핑 실패 시 skip).
     */
    public NewsJournalSummaryResult findSummary(Long userId, int recentLimit) {
        List<NewsEvent> recent = eventRepository.findRecentByUserId(userId, recentLimit);
        List<NewsEventCategoryCount> rawCounts = eventRepository.countByCategoryGroupedByCategoryId(userId);

        Map<Long, NewsEventCategory> categoryById = new HashMap<>();
        for (NewsEventCategory c : categoryRepository.findByUserIdOrderByNameAsc(userId)) {
            categoryById.put(c.getId(), c);
        }

        List<NewsJournalSummaryResult.CategoryCountItem> items = new ArrayList<>(rawCounts.size());
        for (NewsEventCategoryCount rc : rawCounts) {
            NewsEventCategory category = categoryById.get(rc.categoryId());
            if (category == null) {
                continue;
            }
            items.add(new NewsJournalSummaryResult.CategoryCountItem(
                    rc.categoryId(), category.getName(), rc.count()));
        }
        return new NewsJournalSummaryResult(recent, items);
    }

    /**
     * 뉴스 기록 화면 통계 — 임팩트별/분류별 건수 + 사건별 키워드 목록.
     */
    public NewsJournalStatsResult findStats(Long userId) {
        Map<EventImpact, Long> impactCounts = impactCountsOf(userId);
        long totalCount = impactCounts.values().stream().mapToLong(Long::longValue).sum();
        return new NewsJournalStatsResult(totalCount, impactCounts,
                categoryCountsOf(userId), keywordEventsOf(userId));
    }

    /** 임팩트별 건수. 0건 임팩트도 키를 채워 프론트 세그먼트가 뱃지를 항상 그릴 수 있게 한다. */
    private Map<EventImpact, Long> impactCountsOf(Long userId) {
        Map<EventImpact, Long> counts = new EnumMap<>(EventImpact.class);
        for (EventImpact impact : EventImpact.values()) {
            counts.put(impact, 0L);
        }
        for (NewsEventImpactCount ic : eventRepository.countByImpactGroupedByImpact(userId)) {
            counts.put(ic.impact(), ic.count());
        }
        return counts;
    }

    /** 분류별 건수 — 이름순 전체 목록에 건수를 메모리 join (0건 분류 포함, 편집 폼 칩과 공유). */
    private List<NewsJournalStatsResult.CategoryCountItem> categoryCountsOf(Long userId) {
        Map<Long, Long> countByCategoryId = new HashMap<>();
        for (NewsEventCategoryCount cc : eventRepository.countByCategoryGroupedByCategoryId(userId)) {
            countByCategoryId.put(cc.categoryId(), cc.count());
        }
        List<NewsJournalStatsResult.CategoryCountItem> items = new ArrayList<>();
        for (NewsEventCategory c : categoryRepository.findByUserIdOrderByNameAsc(userId)) {
            items.add(new NewsJournalStatsResult.CategoryCountItem(
                    c.getId(), c.getName(), countByCategoryId.getOrDefault(c.getId(), 0L)));
        }
        return items;
    }

    /** 사건별 키워드 목록 — 평탄화 행을 사건 단위로 재그룹. 행 정렬이 발생일 내림차순이라 그룹 순서도 최근 우선. */
    private List<NewsJournalStatsResult.KeywordEventItem> keywordEventsOf(Long userId) {
        Map<Long, LocalDate> dateByEventId = new LinkedHashMap<>();
        Map<Long, List<String>> keywordsByEventId = new HashMap<>();
        for (NewsEventKeywordRow row : keywordRepository.findRowsByUserId(userId)) {
            dateByEventId.putIfAbsent(row.eventId(), row.occurredDate());
            keywordsByEventId.computeIfAbsent(row.eventId(), k -> new ArrayList<>()).add(row.keyword());
        }
        List<NewsJournalStatsResult.KeywordEventItem> items = new ArrayList<>(dateByEventId.size());
        for (Map.Entry<Long, LocalDate> entry : dateByEventId.entrySet()) {
            items.add(new NewsJournalStatsResult.KeywordEventItem(
                    entry.getKey(), entry.getValue(), List.copyOf(keywordsByEventId.get(entry.getKey()))));
        }
        return items;
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
        Map<Long, List<NewsEventKeyword>> keywordsByEventId = keywordRepository.findAllByEventIds(eventIds);

        Map<Long, NewsEventCategory> categoryById = new HashMap<>();
        for (NewsEventCategory c : categoryRepository.findByUserIdOrderByNameAsc(userId)) {
            categoryById.put(c.getId(), c);
        }

        List<NewsEventListItemResult> items = new ArrayList<>(events.size());
        for (NewsEvent e : events) {
            List<NewsEventLink> links = linksByEventId.getOrDefault(e.getId(), List.of());
            List<NewsEventKeyword> keywords = keywordsByEventId.getOrDefault(e.getId(), List.of());
            NewsEventCategory category = e.getCategoryId() == null ? null : categoryById.get(e.getCategoryId());
            items.add(new NewsEventListItemResult(e, category, links, keywords));
        }
        return new NewsEventListResult(items, totalCount, filter.page(), filter.size());
    }
}