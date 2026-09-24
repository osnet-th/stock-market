package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchField;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchSort;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.model.UserNewsState;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserNewsStateRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsSearchApplicationServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private NewsFullTextSearchPort newsFullTextSearchPort;

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private UserNewsStateRepository userNewsStateRepository;

    @InjectMocks
    private NewsSearchApplicationService service;

    private static NewsSearchCriteria criteria(String query, List<Long> keywordIds,
                                               LocalDate startDate, LocalDate endDate,
                                               Region region, boolean unreadOnly) {
        return new NewsSearchCriteria(query, keywordIds, startDate, endDate, region,
                NewsSearchField.TITLE_CONTENT, NewsSearchSort.LATEST, unreadOnly, 0, 20);
    }

    private static News news(Long id, String url, String title, Long keywordId, Region region, String source) {
        return new News(id, url, title, "본문", LocalDateTime.now(), LocalDateTime.now(),
                keywordId, region, source);
    }

    @Test
    @DisplayName("검색어로 검색 시 ES 결과를 DB 행으로 보강해 반환한다")
    void search_withQuery_shouldReturnEnrichedNewsList() {
        // given — ES 결과에는 id·언론사가 없다 (문서 _id 가 originalUrl 이라서)
        News esHit = news(null, "https://example.com/1", "삼성전자 실적 발표", 100L, Region.DOMESTIC, null);
        News stored = news(1L, "https://example.com/1", "삼성전자 실적 발표", 100L, Region.DOMESTIC, "한국경제");

        when(newsFullTextSearchPort.search(any(NewsSearchCriteria.class)))
                .thenReturn(new PageResult<>(List.of(esHit), 0, 20, 1));
        when(newsRepository.findByOriginalUrls(anyList())).thenReturn(List.of(stored));
        when(userNewsStateRepository.findByUserIdAndNewsIds(eq(USER_ID), anyList()))
                .thenReturn(List.of());

        // when
        PageResult<NewsDto> result = service.search(
                criteria("삼성전자", List.of(100L), null, null, null, false), USER_ID);

        // then — id·언론사가 DB 행에서 채워져야 한다
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).getSource()).isEqualTo("한국경제");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("검색어가 없으면 ES 를 거치지 않고 DB 최신순으로 조회한다")
    void search_withoutQuery_shouldUseDatabaseLatestPath() {
        // given
        News stored = news(2L, "https://example.com/2", "저장된 기사", 100L, Region.DOMESTIC, "매일경제");
        when(newsRepository.findLatestByScope(any(NewsSearchCriteria.class), eq(USER_ID)))
                .thenReturn(new PageResult<>(List.of(stored), 0, 20, 1));
        when(userNewsStateRepository.findByUserIdAndNewsIds(eq(USER_ID), anyList()))
                .thenReturn(List.of());

        // when
        PageResult<NewsDto> result = service.search(
                criteria(null, List.of(100L), null, null, null, false), USER_ID);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSource()).isEqualTo("매일경제");
        verifyNoInteractions(newsFullTextSearchPort);
    }

    @Test
    @DisplayName("구독 키워드가 없으면 조회 없이 빈 결과를 반환한다")
    void search_withNoKeywordScope_shouldReturnEmptyWithoutQuerying() {
        // when
        PageResult<NewsDto> result = service.search(
                criteria("삼성전자", List.of(), null, null, null, false), USER_ID);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
        verifyNoInteractions(newsFullTextSearchPort);
        verifyNoInteractions(newsRepository);
        verifyNoInteractions(userNewsStateRepository);
    }

    @Test
    @DisplayName("DB 에 없는 URL 은 ES 값을 그대로 유지한다")
    void search_whenStoredRowMissing_shouldKeepEsHit() {
        // given — 색인은 남았지만 행이 지워진 경우
        News esHit = news(null, "https://example.com/gone", "지워진 기사", 100L, Region.INTERNATIONAL, null);
        when(newsFullTextSearchPort.search(any(NewsSearchCriteria.class)))
                .thenReturn(new PageResult<>(List.of(esHit), 0, 20, 1));
        when(newsRepository.findByOriginalUrls(anyList())).thenReturn(Collections.emptyList());
        when(userNewsStateRepository.findByUserIdAndNewsIds(eq(USER_ID), anyList()))
                .thenReturn(List.of());

        // when
        PageResult<NewsDto> result = service.search(
                criteria("지워진", List.of(100L), null, null, null, false), USER_ID);

        // then — 결과가 사라지지 않고 언론사만 비어 있어야 한다
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("지워진 기사");
        assertThat(result.getContent().get(0).getSource()).isNull();
    }

    @Test
    @DisplayName("읽음/저장 상태가 결과에 결합된다")
    void search_shouldAttachUserState() {
        // given
        News stored = news(3L, "https://example.com/3", "읽은 기사", 100L, Region.DOMESTIC, "연합뉴스");
        when(newsRepository.findLatestByScope(any(NewsSearchCriteria.class), eq(USER_ID)))
                .thenReturn(new PageResult<>(List.of(stored), 0, 20, 1));
        when(userNewsStateRepository.findByUserIdAndNewsIds(eq(USER_ID), anyList()))
                .thenReturn(List.of(new UserNewsState(1L, USER_ID, 3L, true, true, LocalDateTime.now())));

        // when
        PageResult<NewsDto> result = service.search(
                criteria(null, List.of(100L), null, null, null, false), USER_ID);

        // then
        assertThat(result.getContent().get(0).isRead()).isTrue();
        assertThat(result.getContent().get(0).isSaved()).isTrue();
    }

    @Test
    @DisplayName("상태 행이 없는 기사는 안 읽음·미저장으로 본다")
    void search_withoutStateRow_shouldDefaultToUnreadUnsaved() {
        // given
        News stored = news(4L, "https://example.com/4", "새 기사", 100L, Region.DOMESTIC, "뉴시스");
        when(newsRepository.findLatestByScope(any(NewsSearchCriteria.class), eq(USER_ID)))
                .thenReturn(new PageResult<>(List.of(stored), 0, 20, 1));
        when(userNewsStateRepository.findByUserIdAndNewsIds(eq(USER_ID), anyList()))
                .thenReturn(List.of());

        // when
        PageResult<NewsDto> result = service.search(
                criteria(null, List.of(100L), null, null, null, false), USER_ID);

        // then
        assertThat(result.getContent().get(0).isRead()).isFalse();
        assertThat(result.getContent().get(0).isSaved()).isFalse();
    }

    @Test
    @DisplayName("안 읽은 것만 + 검색어면 읽은 기사를 제외한 ES 검색을 쓴다")
    void search_withUnreadOnlyAndQuery_shouldExcludeReadUrls() {
        // given
        News esHit = news(null, "https://example.com/5", "안 읽은 기사", 100L, Region.DOMESTIC, null);
        News stored = news(5L, "https://example.com/5", "안 읽은 기사", 100L, Region.DOMESTIC, "한국경제");
        List<String> readUrls = List.of("https://example.com/read-1");

        when(userNewsStateRepository.findReadOriginalUrls(eq(USER_ID), anyList(), anyInt()))
                .thenReturn(readUrls);
        when(newsFullTextSearchPort.search(any(NewsSearchCriteria.class), eq(readUrls)))
                .thenReturn(new PageResult<>(List.of(esHit), 0, 20, 1));
        when(newsRepository.findByOriginalUrls(anyList())).thenReturn(List.of(stored));
        when(userNewsStateRepository.findByUserIdAndNewsIds(eq(USER_ID), anyList()))
                .thenReturn(List.of());

        // when
        PageResult<NewsDto> result = service.search(
                criteria("기사", List.of(100L), null, null, null, true), USER_ID);

        // then — 제외 목록을 넘긴 검색 결과가 쓰여야 한다
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(5L);
        assertThat(result.getContent().get(0).isRead()).isFalse();
    }

    @Test
    @DisplayName("날짜 범위 필터는 조건 그대로 조회 계층에 전달된다")
    void search_withDateRange_shouldPassCriteriaThrough() {
        // given
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 10);
        when(newsRepository.findLatestByScope(any(NewsSearchCriteria.class), eq(USER_ID)))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0, 20, 0));

        // when
        PageResult<NewsDto> result = service.search(
                criteria(null, List.of(100L), start, end, null, false), USER_ID);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}
