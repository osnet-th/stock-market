package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NewsSaveServiceTest {

    @Test
    void save_returns_news_dto() {
        NewsRepository repository = mock(NewsRepository.class);
        NewsSaveService service = new NewsSaveService(repository, new NoOpTransactionManager());

        News saved = new News(
                1L,
                "url",
                1L,
                "title",
                "content",
                LocalDateTime.now(),
                LocalDateTime.now(),
                NewsPurpose.KEYWORD,
                "keyword",
                Region.DOMESTIC
        );

        when(repository.save(any(News.class))).thenReturn(saved);

        NewsSaveRequest request = new NewsSaveRequest(
                "url",
                1L,
                "title",
                "content",
                LocalDateTime.now(),
                "keyword",
                Region.DOMESTIC
        );

        NewsDto result = service.save(request, NewsPurpose.KEYWORD);

        assertNotNull(result);
        assertEquals("url", result.getOriginalUrl());
        verify(repository, times(1)).save(any(News.class));
    }

    private static class NoOpTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return mock(TransactionStatus.class);
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
