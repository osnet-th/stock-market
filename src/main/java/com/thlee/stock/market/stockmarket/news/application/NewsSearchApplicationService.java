package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;
import com.thlee.stock.market.stockmarket.news.domain.model.UserNewsState;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.domain.repository.UserNewsStateRepository;
import com.thlee.stock.market.stockmarket.news.domain.service.NewsFullTextSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 뉴스 전문 검색 유스케이스 서비스
 *
 * <p>#115 부터 <b>두 경로</b>로 갈린다.
 * <ul>
 *   <li>검색어 없음 → DB 최신순 조회. 목업 기본 화면("저장된 전체 뉴스")이 여기다.
 *       ES 를 거치지 않아 정렬이 안정적이고 {@code 안 읽은 것만} 을 SQL 로 정확히 건다.</li>
 *   <li>검색어 있음 → ES 전문 검색 후 {@code originalUrl} 로 DB 를 다시 조회해 보강.
 *       ES 문서는 {@code _id} 가 originalUrl 이라 뉴스 id 를 갖고 있지 않고 언론사도 미색인이다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsSearchApplicationService {

    /**
     * ES 경로에서 "안 읽은 것만" 을 적용할 때 제외 목록으로 넘기는 읽은 기사 수 상한.
     *
     * <p>제외를 쿼리에 넣어야 전체 건수가 정확해지는데, 목록이 무한정 커지면 요청마다
     * URL 을 그만큼 실어 보내게 된다. 최근에 읽은 것부터 이 개수까지만 제외하며,
     * 이를 넘게 읽은 사용자는 아주 오래전에 읽은 기사가 검색 결과에 다시 보일 수 있다.
     */
    private static final int READ_EXCLUSION_LIMIT = 2000;

    private final NewsFullTextSearchPort newsFullTextSearchPort;
    private final NewsRepository newsRepository;
    private final UserNewsStateRepository userNewsStateRepository;

    public PageResult<NewsDto> search(NewsSearchCriteria criteria, Long userId) {
        // 구독 키워드가 하나도 없으면 볼 수 있는 뉴스가 없다 — 조회 자체를 생략한다
        if (criteria.hasNoKeywordScope()) {
            return new PageResult<>(List.of(), criteria.page(), criteria.size(), 0);
        }

        PageResult<News> result = criteria.hasQuery()
                ? enrichFromDatabase(searchFullText(criteria, userId))
                : newsRepository.findLatestByScope(criteria, userId);

        return attachUserState(result, userId);
    }

    private PageResult<News> searchFullText(NewsSearchCriteria criteria, Long userId) {
        if (!criteria.unreadOnly()) {
            return newsFullTextSearchPort.search(criteria);
        }
        List<String> readUrls = userNewsStateRepository
                .findReadOriginalUrls(userId, criteria.keywordIds(), READ_EXCLUSION_LIMIT);
        return newsFullTextSearchPort.search(criteria, readUrls);
    }

    /**
     * ES 결과에 DB 행의 id·언론사를 채워 넣는다 (#115).
     *
     * <p>ES 가 정한 순서(관련도 또는 최신순)를 유지해야 하므로 DB 결과로 대체하지 않고
     * URL 로 매칭해 행을 교체한다. DB 에 없는 URL(색인은 남았는데 행이 지워진 경우)은 ES 값을 그대로 쓴다.
     * 페이지당 조회 1회라 페이지 크기와 무관하게 쿼리 수가 늘지 않는다.
     */
    private PageResult<News> enrichFromDatabase(PageResult<News> esResult) {
        List<News> hits = esResult.getContent();
        if (hits.isEmpty()) {
            return esResult;
        }

        List<String> urls = hits.stream()
                .map(News::getOriginalUrl)
                .filter(Objects::nonNull)
                .toList();
        Map<String, News> stored = newsRepository.findByOriginalUrls(urls).stream()
                .collect(Collectors.toMap(News::getOriginalUrl, Function.identity(), (a, b) -> a));

        List<News> merged = hits.stream()
                .map(hit -> stored.getOrDefault(hit.getOriginalUrl(), hit))
                .toList();

        return new PageResult<>(merged, esResult.getPage(), esResult.getSize(), esResult.getTotalElements());
    }

    /**
     * 읽음/저장 상태를 붙인다 (#115). 상태 행이 없는 기사는 "안 읽음 · 미저장"이다.
     * 페이지당 조회 1회.
     */
    private PageResult<NewsDto> attachUserState(PageResult<News> result, Long userId) {
        List<News> content = result.getContent();
        List<Long> newsIds = content.stream()
                .map(News::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, UserNewsState> stateById = userNewsStateRepository
                .findByUserIdAndNewsIds(userId, newsIds).stream()
                .collect(Collectors.toMap(UserNewsState::getNewsId, Function.identity(), (a, b) -> a));

        List<NewsDto> dtoList = content.stream()
                .map(news -> {
                    UserNewsState state = news.getId() == null ? null : stateById.get(news.getId());
                    return state == null
                            ? NewsDto.from(news)
                            : NewsDto.from(news, state.isRead(), state.isSaved());
                })
                .toList();

        return new PageResult<>(dtoList, result.getPage(), result.getSize(), result.getTotalElements());
    }
}
