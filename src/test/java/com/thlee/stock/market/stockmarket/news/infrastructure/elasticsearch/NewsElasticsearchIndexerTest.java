package com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.infrastructure.elasticsearch.document.NewsDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsElasticsearchIndexerTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private NewsElasticsearchIndexer indexer;

    @Test
    @DisplayName("News 리스트를 전달하면 ES에 인덱싱된다")
    void indexAll_withNewsList_shouldSaveDocuments() {
        // given
        News news = new News(
                1L, "https://example.com/1", "삼성전자 실적 발표",
                "삼성전자가 분기 실적을 발표했다.",
                LocalDateTime.now(), LocalDateTime.now(), 100L, Region.DOMESTIC
        );
        when(elasticsearchOperations.save(anyList())).thenReturn(Collections.emptyList());

        // when
        indexer.indexAll(List.of(news));

        // then
        verify(elasticsearchOperations).save(anyList());
    }

    @Test
    @DisplayName("빈 리스트 전달 시 인덱싱 호출 없이 정상 종료")
    void indexAll_withEmptyList_shouldNotCallSave() {
        // when
        indexer.indexAll(Collections.emptyList());

        // then
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    @DisplayName("null 전달 시 인덱싱 호출 없이 정상 종료")
    void indexAll_withNull_shouldNotCallSave() {
        // when
        indexer.indexAll(null);

        // then
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    @DisplayName("content가 null인 News 포함 시 정상 인덱싱")
    void indexAll_withNullContent_shouldSaveSuccessfully() {
        // given
        News news = new News(
                2L, "https://example.com/2", "뉴스 제목",
                null,
                LocalDateTime.now(), LocalDateTime.now(), 100L, Region.INTERNATIONAL
        );
        when(elasticsearchOperations.save(anyList())).thenReturn(Collections.emptyList());

        // when
        indexer.indexAll(List.of(news));

        // then
        verify(elasticsearchOperations).save(anyList());
    }

    @Test
    @DisplayName("ES 장애 시 예외를 전파하지 않고 로그만 남긴다")
    void indexAll_whenEsFails_shouldNotPropagateException() {
        // given
        News news = new News(
                3L, "https://example.com/3", "뉴스 제목",
                "본문 내용",
                LocalDateTime.now(), LocalDateTime.now(), 100L, Region.DOMESTIC
        );
        when(elasticsearchOperations.save(anyList()))
                .thenThrow(new RuntimeException("ES connection refused"));

        // when — 예외가 전파되지 않아야 한다
        indexer.indexAll(List.of(news));

        // then — 정상 종료 (예외 미전파)
        verify(elasticsearchOperations).save(anyList());
    }
}