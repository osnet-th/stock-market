package com.thlee.stock.market.stockmarket.newsjournal.application;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.CreateNewsEventCommand;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventLinkCommand;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.UpdateNewsEventCommand;
import com.thlee.stock.market.stockmarket.newsjournal.application.exception.NewsEventNotFoundException;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventLinkRepository;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 사건 생성/수정/삭제 유스케이스.
 *
 * <p>트랜잭션 경계는 본 서비스가 소유한다 (ARCHITECTURE.md 규칙). 자식 링크는
 * {@link NewsEventLinkRepository#replaceAll} 정책으로 본체 갱신과 같은 트랜잭션에서 일괄 교체한다.
 * 카테고리는 {@link NewsEventCategoryService#resolve} 로 find-or-create 한 뒤 categoryId 를 주입한다.
 */
@Service
@RequiredArgsConstructor
public class NewsEventWriteService {

    private final NewsEventRepository eventRepository;
    private final NewsEventLinkRepository linkRepository;
    private final NewsEventCategoryService categoryService;

    @Transactional
    public Long create(CreateNewsEventCommand cmd) {
        NewsEventCategory category = categoryService.resolve(cmd.userId(), cmd.categoryName());
        LocalDate today = LocalDate.now();
        NewsEvent event = NewsEvent.create(
                cmd.userId(), cmd.title(), cmd.occurredDate(), today, cmd.impact(),
                category.getId(), cmd.what(), cmd.why(), cmd.how()
        );
        NewsEvent saved = eventRepository.save(event);
        Long eventId = saved.getId();

        linkRepository.replaceAll(eventId, toLinks(eventId, cmd.links()));
        return eventId;
    }

    @Transactional
    public void update(UpdateNewsEventCommand cmd) {
        NewsEvent event = eventRepository.findByIdAndUserId(cmd.id(), cmd.userId())
                .orElseThrow(() -> new NewsEventNotFoundException(cmd.id()));
        NewsEventCategory category = categoryService.resolve(cmd.userId(), cmd.categoryName());
        LocalDate today = LocalDate.now();
        event.updateBody(cmd.title(), cmd.occurredDate(), today, cmd.impact(),
                category.getId(), cmd.what(), cmd.why(), cmd.how());
        eventRepository.save(event);

        linkRepository.replaceAll(cmd.id(), toLinks(cmd.id(), cmd.links()));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        eventRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NewsEventNotFoundException(id));
        linkRepository.deleteByEventId(id);
        eventRepository.deleteByIdAndUserId(id, userId);
    }

    private static List<NewsEventLink> toLinks(Long eventId, List<NewsEventLinkCommand> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<NewsEventLink> result = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            NewsEventLinkCommand l = source.get(i);
            result.add(NewsEventLink.create(eventId, l.title(), l.url(), i));
        }
        return result;
    }
}