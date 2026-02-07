package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewsQueryServiceTest {

    @Test
    void get_by_purpose_returns_dtos() {
        NewsRepository repository = mock(NewsRepository.class);
        NewsQueryService service = new NewsQueryService(repository);

        News news = new News(
                1L,
                "url",
                1L,
                "title",
                "content",
                LocalDateTime.now(),
                LocalDateTime.now(),
                NewsPurpose.KEYWORD,
                "keyword"
        );

        when(repository.findByPurpose(NewsPurpose.KEYWORD)).thenReturn(List.of(news));

        List<NewsDto> results = service.getByPurpose(NewsPurpose.KEYWORD);

        assertEquals(1, results.size());
        assertEquals("url", results.get(0).getOriginalUrl());
    }
}
