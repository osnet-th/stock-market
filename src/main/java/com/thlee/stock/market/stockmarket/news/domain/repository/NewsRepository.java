package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsRepository {

    News save(News news);

    boolean insertIgnoreDuplicate(News news);

    Optional<News> findByOriginalUrl(String originalUrl);

    PageResult<News> findByKeywordId(Long keywordId, int page, int size);

    PageResult<News> findAll(int page, int size);

    /**
     * 여러 키워드의 최신 뉴스를 합쳐 size 건까지 반환 (#114 홈 키워드 피드).
     */
    List<News> findLatestByKeywordIds(List<Long> keywordIds, int size);

    /**
     * 여러 키워드의 since 이후 수집 건수 (#114).
     */
    long countByKeywordIdsSince(List<Long> keywordIds, LocalDateTime since);

    /**
     * 검색어 없는 조회 — 키워드 스코프 + 기간·지역·읽음 필터 최신순 페이징 (#115).
     *
     * <p>{@code unreadOnly} 가 사용자별 조건이라 userId 를 함께 받는다.
     */
    PageResult<News> findLatestByScope(NewsSearchCriteria criteria, Long userId);

    /**
     * originalUrl 목록으로 저장된 뉴스를 조회한다 (#115).
     *
     * <p>ES 검색 결과에는 뉴스 id 와 언론사가 없다(문서 _id 가 originalUrl 이고 언론사는 미색인).
     * 검색 결과를 DB 행으로 되짚어 id·언론사를 붙이는 데 쓴다.
     */
    List<News> findByOriginalUrls(List<String> originalUrls);

    /**
     * 키워드별 총 건수 + 마지막 저장 시각 (#115 레일 통계). 쿼리 1회.
     */
    List<KeywordNewsCount> aggregateCountsByKeywordIds(List<Long> keywordIds);

    /**
     * 키워드별·일자별 수집 건수 (#115 스파크라인). 쿼리 1회.
     */
    List<KeywordDailyNewsCount> aggregateDailyCountsByKeywordIds(List<Long> keywordIds, LocalDateTime since);

    /**
     * 기존 기사를 새 키워드로 이관한다 (#115 키워드 수정 = 재구독).
     *
     * @return 이관된 건수
     */
    int reassignKeywordId(Long oldKeywordId, Long newKeywordId);

    /**
     * 특정 키워드의 전체 기사 (#115 — 이관 후 ES 재색인 대상).
     */
    List<News> findAllByKeywordId(Long keywordId);

    void deleteByKeywordId(Long keywordId);

    void deleteByIds(List<Long> ids);
}
