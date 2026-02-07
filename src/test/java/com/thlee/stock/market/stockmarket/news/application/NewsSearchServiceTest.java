package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NewsSearchServiceTest {

    @Test
    void search_uses_first_port_with_results() {
        NewsSearchPort first = mock(NewsSearchPort.class);
        NewsSearchPort second = mock(NewsSearchPort.class);

        when(first.search(eq("keyword"), any(LocalDateTime.class)))
                .thenReturn(List.of(new NewsSearchResult("t", "u", "c", LocalDateTime.now())));

        NewsSearchService service = new NewsSearchService(List.of(first, second));
        List<NewsResultDto> results = service.search("keyword");

        assertEquals(1, results.size());
        verify(first, times(1)).search(eq("keyword"), any(LocalDateTime.class));
        verifyNoInteractions(second);
    }

    @Test
    void search_falls_back_on_empty_or_error() {
        NewsSearchPort first = mock(NewsSearchPort.class);
        NewsSearchPort second = mock(NewsSearchPort.class);

        when(first.search(eq("keyword"), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("fail"));
        when(second.search(eq("keyword"), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        NewsSearchService service = new NewsSearchService(List.of(first, second));
        List<NewsResultDto> results = service.search("keyword");

        assertTrue(results.isEmpty());
        verify(first, times(1)).search(eq("keyword"), any(LocalDateTime.class));
        verify(second, times(1)).search(eq("keyword"), any(LocalDateTime.class));
    }
}
