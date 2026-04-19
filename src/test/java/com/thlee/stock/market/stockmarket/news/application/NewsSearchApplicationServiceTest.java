package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsFullTextSearchPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsSearchApplicationServiceTest {

    @Mock
    private NewsFullTextSearchPort newsFullTextSearchPort;

    @InjectMocks
    private NewsSearchApplicationService service;

    @Test
    @DisplayName("검색어로 검색 시 관련 뉴스 목록을 반환한다")
    void search_withQuery_shouldReturnNewsList() {
        // given
        News news = new News(1L, "https://example.com/1", "삼성전자 실적 발표",
                "삼성전자가 분기 실적을 발표했다.",
                LocalDateTime.now(), LocalDateTime.now(), 100L, Region.DOMESTIC);
        PageResult<News> pageResult = new PageResult<>(List.of(news), 0, 20, 1);

        when(newsFullTextSearchPort.search(eq("삼성전자"), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(pageResult);

        // when
        PageResult<NewsDto> result = service.search("삼성전자", null, null, null, 0, 20);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("삼성전자 실적 발표");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("날짜 범위 필터 적용 시 해당 기간 뉴스만 반환한다")
    void search_withDateRange_shouldFilterByDate() {
        // given
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 10);
        PageResult<News> pageResult = new PageResult<>(Collections.emptyList(), 0, 20, 0);

        when(newsFullTextSearchPort.search(eq("경제"), eq(start), eq(end), isNull(), eq(0), eq(20)))
                .thenReturn(pageResult);

        // when
        PageResult<NewsDto> result = service.search("경제", start, end, null, 0, 20);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("지역 필터 적용 시 해당 지역 뉴스만 반환한다")
    void search_withRegion_shouldFilterByRegion() {
        // given
        News news = new News(2L, "https://example.com/2", "Apple earnings report",
                "Apple reported quarterly earnings.",
                LocalDateTime.now(), LocalDateTime.now(), 200L, Region.INTERNATIONAL);
        PageResult<News> pageResult = new PageResult<>(List.of(news), 0, 20, 1);

        when(newsFullTextSearchPort.search(eq("Apple"), isNull(), isNull(), eq(Region.INTERNATIONAL), eq(0), eq(20)))
                .thenReturn(pageResult);

        // when
        PageResult<NewsDto> result = service.search("Apple", null, null, Region.INTERNATIONAL, 0, 20);

        // then
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("검색 결과 없을 시 빈 리스트를 반환한다")
    void search_noResults_shouldReturnEmptyList() {
        // given
        PageResult<News> pageResult = new PageResult<>(Collections.emptyList(), 0, 20, 0);

        when(newsFullTextSearchPort.search(eq("없는키워드"), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(pageResult);

        // when
        PageResult<NewsDto> result = service.search("없는키워드", null, null, null, 0, 20);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지네이션이 올바르게 전달된다")
    void search_withPagination_shouldPassPageParams() {
        // given
        PageResult<News> pageResult = new PageResult<>(Collections.emptyList(), 2, 10, 50);

        when(newsFullTextSearchPort.search(eq("테스트"), isNull(), isNull(), isNull(), eq(2), eq(10)))
                .thenReturn(pageResult);

        // when
        PageResult<NewsDto> result = service.search("테스트", null, null, null, 2, 10);

        // then
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(50);
        assertThat(result.getTotalPages()).isEqualTo(5);
    }
}