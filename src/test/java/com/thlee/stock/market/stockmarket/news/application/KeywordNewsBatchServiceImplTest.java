package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.KeywordRegion;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeywordNewsBatchServiceImplTest {

    @Test
    void returns_zero_when_no_active_keywords() {
        KeywordService keywordService = mock(KeywordService.class);
        NewsSearchService newsSearchService = mock(NewsSearchService.class);
        NewsSaveService newsSaveService = mock(NewsSaveService.class);
        NewsQueryService newsQueryService = mock(NewsQueryService.class);

        KeywordNewsBatchService service = new KeywordNewsBatchServiceImpl(
                keywordService, newsSearchService, newsSaveService, newsQueryService
        );

        when(keywordService.getAllActiveKeywords()).thenReturn(Collections.emptyList());

        int result = service.executeKeywordNewsBatch();

        assertEquals(0, result);
    }

    @Test
    void returns_zero_when_no_search_results() {
        KeywordService keywordService = mock(KeywordService.class);
        NewsSearchService newsSearchService = mock(NewsSearchService.class);
        NewsSaveService newsSaveService = mock(NewsSaveService.class);
        NewsQueryService newsQueryService = mock(NewsQueryService.class);

        KeywordNewsBatchService service = new KeywordNewsBatchServiceImpl(
                keywordService, newsSearchService, newsSaveService, newsQueryService
        );

        Keyword keyword = new Keyword(1L, "삼성전자", 1L, true, KeywordRegion.DOMESTIC, LocalDateTime.now());
        when(keywordService.getAllActiveKeywords()).thenReturn(List.of(keyword));
        when(newsSearchService.search("삼성전자")).thenReturn(Collections.emptyList());

        int result = service.executeKeywordNewsBatch();

        assertEquals(0, result);
    }

    @Test
    void returns_saved_count_when_new_news_exist() {
        KeywordService keywordService = mock(KeywordService.class);
        NewsSearchService newsSearchService = mock(NewsSearchService.class);
        NewsSaveService newsSaveService = mock(NewsSaveService.class);
        NewsQueryService newsQueryService = mock(NewsQueryService.class);

        KeywordNewsBatchService service = new KeywordNewsBatchServiceImpl(
                keywordService, newsSearchService, newsSaveService, newsQueryService
        );

        Keyword keyword = new Keyword(1L, "삼성전자", 1L, true, KeywordRegion.DOMESTIC, LocalDateTime.now());
        NewsResultDto newsResult = new NewsResultDto(
                "삼성전자 뉴스",
                "http://example.com/news1",
                "내용",
                LocalDateTime.now()
        );

        when(keywordService.getAllActiveKeywords()).thenReturn(List.of(keyword));
        when(newsSearchService.search("삼성전자")).thenReturn(List.of(newsResult));
        when(newsQueryService.findExistingUrls(anyList())).thenReturn(Collections.emptyList());
        when(newsSaveService.saveBatch(anyList(), eq(NewsPurpose.KEYWORD)))
                .thenReturn(new NewsBatchSaveResult(1, 0, 0));

        int result = service.executeKeywordNewsBatch();

        assertEquals(1, result);
    }

    @Test
    void returns_zero_when_all_news_already_exist() {
        KeywordService keywordService = mock(KeywordService.class);
        NewsSearchService newsSearchService = mock(NewsSearchService.class);
        NewsSaveService newsSaveService = mock(NewsSaveService.class);
        NewsQueryService newsQueryService = mock(NewsQueryService.class);

        KeywordNewsBatchService service = new KeywordNewsBatchServiceImpl(
                keywordService, newsSearchService, newsSaveService, newsQueryService
        );

        Keyword keyword = new Keyword(1L, "삼성전자", 1L, true, KeywordRegion.DOMESTIC, LocalDateTime.now());
        NewsResultDto newsResult = new NewsResultDto(
                "삼성전자 뉴스",
                "http://example.com/news1",
                "내용",
                LocalDateTime.now()
        );

        when(keywordService.getAllActiveKeywords()).thenReturn(List.of(keyword));
        when(newsSearchService.search("삼성전자")).thenReturn(List.of(newsResult));
        when(newsQueryService.findExistingUrls(List.of("http://example.com/news1")))
                .thenReturn(List.of("http://example.com/news1"));

        int result = service.executeKeywordNewsBatch();

        assertEquals(0, result);
    }
}