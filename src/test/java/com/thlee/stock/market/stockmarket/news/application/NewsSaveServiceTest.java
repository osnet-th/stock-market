package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsSaveRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsIndexPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsSaveServiceTest {

    @Test
    @DisplayName("saveBatch 호출 시 DB 저장 성공한 뉴스가 NewsIndexPort에 전달된다")
    void saveBatch_withSuccessfulInserts_shouldIndexToEs() {
        // given
        NewsRepository newsRepository = mock(NewsRepository.class);
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        NewsIndexPort newsIndexPort = mock(NewsIndexPort.class);

        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        NewsSaveService service = new NewsSaveService(newsRepository, txManager, newsIndexPort);

        when(newsRepository.insertIgnoreDuplicate(any(News.class))).thenReturn(true);

        NewsSaveRequest request = new NewsSaveRequest(
                "https://example.com/1", "제목", "본문",
                LocalDateTime.now(), 1L, Region.DOMESTIC
        );

        // when
        NewsBatchSaveResult result = service.saveBatch(List.of(request));

        // then
        assertThat(result.getSuccessCount()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<News>> captor = ArgumentCaptor.forClass(List.class);
        verify(newsIndexPort).indexAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getOriginalUrl()).isEqualTo("https://example.com/1");
    }

    @Test
    @DisplayName("모든 뉴스가 중복인 경우 indexAll이 호출되지 않는다")
    void saveBatch_allDuplicates_shouldNotCallIndex() {
        // given
        NewsRepository newsRepository = mock(NewsRepository.class);
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        NewsIndexPort newsIndexPort = mock(NewsIndexPort.class);

        when(txManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        NewsSaveService service = new NewsSaveService(newsRepository, txManager, newsIndexPort);

        when(newsRepository.insertIgnoreDuplicate(any(News.class))).thenReturn(false);

        NewsSaveRequest request = new NewsSaveRequest(
                "https://example.com/dup", "중복 제목", "중복 본문",
                LocalDateTime.now(), 1L, Region.DOMESTIC
        );

        // when
        NewsBatchSaveResult result = service.saveBatch(List.of(request));

        // then
        assertThat(result.getIgnoredCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(0);
        verifyNoInteractions(newsIndexPort);
    }

    @Test
    @DisplayName("빈 요청 리스트일 경우 아무 작업도 하지 않는다")
    void saveBatch_emptyList_shouldReturnZeroCounts() {
        // given
        NewsRepository newsRepository = mock(NewsRepository.class);
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        NewsIndexPort newsIndexPort = mock(NewsIndexPort.class);

        NewsSaveService service = new NewsSaveService(newsRepository, txManager, newsIndexPort);

        // when
        NewsBatchSaveResult result = service.saveBatch(List.of());

        // then
        assertThat(result.getSuccessCount()).isEqualTo(0);
        verifyNoInteractions(newsRepository);
        verifyNoInteractions(newsIndexPort);
    }
}