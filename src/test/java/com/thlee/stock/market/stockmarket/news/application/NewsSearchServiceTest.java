package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsResultDto;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPort;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsSearchPortFactory;
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
        // Given
        NewsSearchPort first = mock(NewsSearchPort.class);
        NewsSearchPort second = mock(NewsSearchPort.class);
        NewsSearchPortFactory factory = mock(NewsSearchPortFactory.class);

        when(factory.getPorts(Region.DOMESTIC)).thenReturn(List.of(first, second));
        when(first.search(eq("keyword"), any(LocalDateTime.class)))
                .thenReturn(List.of(new NewsSearchResult("t", "u", "c", LocalDateTime.now())));

        NewsSearchService service = new NewsSearchService(factory);

        // When
        List<NewsResultDto> results = service.search("keyword", Region.DOMESTIC);

        // Then
        assertEquals(1, results.size());
        verify(first, times(1)).search(eq("keyword"), any(LocalDateTime.class));
        verifyNoInteractions(second);
    }

    @Test
    void search_falls_back_on_empty_or_error() {
        // Given
        NewsSearchPort first = mock(NewsSearchPort.class);
        NewsSearchPort second = mock(NewsSearchPort.class);
        NewsSearchPortFactory factory = mock(NewsSearchPortFactory.class);

        when(factory.getPorts(Region.INTERNATIONAL)).thenReturn(List.of(first, second));
        when(first.search(eq("keyword"), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("fail"));
        when(second.search(eq("keyword"), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        NewsSearchService service = new NewsSearchService(factory);

        // When
        List<NewsResultDto> results = service.search("keyword", Region.INTERNATIONAL);

        // Then
        assertTrue(results.isEmpty());
        verify(first, times(1)).search(eq("keyword"), any(LocalDateTime.class));
        verify(second, times(1)).search(eq("keyword"), any(LocalDateTime.class));
    }
}
