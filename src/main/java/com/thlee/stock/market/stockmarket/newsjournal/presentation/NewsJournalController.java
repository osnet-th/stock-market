package com.thlee.stock.market.stockmarket.newsjournal.presentation;

import com.thlee.stock.market.stockmarket.newsjournal.application.NewsEventReadService;
import com.thlee.stock.market.stockmarket.newsjournal.application.NewsEventWriteService;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventDetailResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventListResult;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventListFilter;
import com.thlee.stock.market.stockmarket.newsjournal.presentation.dto.CreateNewsEventRequest;
import com.thlee.stock.market.stockmarket.newsjournal.presentation.dto.NewsEventDetailResponse;
import com.thlee.stock.market.stockmarket.newsjournal.presentation.dto.NewsEventListResponse;
import com.thlee.stock.market.stockmarket.newsjournal.presentation.dto.UpdateNewsEventRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

/**
 * 뉴스 저널 사건 REST API.
 *
 * <p>모든 엔드포인트는 {@link NewsJournalSecurityContext#currentUserId()} 로 안전하게 추출한 userId 로 스코프된다.
 * 권한이 없거나 존재하지 않는 사건 접근은 404 로 통일 (security 권고: IDOR 방지).
 */
@RestController
@RequestMapping("/api/news-journal/events")
@RequiredArgsConstructor
public class NewsJournalController {

    private final NewsEventWriteService writeService;
    private final NewsEventReadService readService;

    @PostMapping
    public ResponseEntity<Map<String, Long>> create(@Valid @RequestBody CreateNewsEventRequest request) {
        Long userId = NewsJournalSecurityContext.currentUserId();
        Long eventId = writeService.create(request.toCommand(userId));
        URI location = UriComponentsBuilder.fromPath("/api/news-journal/events/{id}").buildAndExpand(eventId).toUri();
        return ResponseEntity.created(location).body(Map.of("id", eventId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsEventDetailResponse> findById(@PathVariable Long id) {
        NewsEventDetailResult result = readService.findById(id, NewsJournalSecurityContext.currentUserId());
        return ResponseEntity.ok(NewsEventDetailResponse.from(result));
    }

    @GetMapping
    public ResponseEntity<NewsEventListResponse> findList(
            @RequestParam(required = false) EventImpact impact,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        NewsEventListFilter filter = new NewsEventListFilter(impact, categoryId, from, to, page, size);
        NewsEventListResult result = readService.findList(NewsJournalSecurityContext.currentUserId(), filter);
        return ResponseEntity.ok(NewsEventListResponse.from(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateNewsEventRequest request) {
        writeService.update(request.toCommand(id, NewsJournalSecurityContext.currentUserId()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        writeService.delete(id, NewsJournalSecurityContext.currentUserId());
        return ResponseEntity.noContent().build();
    }
}