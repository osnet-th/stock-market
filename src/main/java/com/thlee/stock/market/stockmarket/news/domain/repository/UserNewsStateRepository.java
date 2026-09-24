package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.UserNewsState;

import java.util.List;
import java.util.Optional;

public interface UserNewsStateRepository {

    UserNewsState save(UserNewsState state);

    Optional<UserNewsState> findByUserIdAndNewsId(Long userId, Long newsId);

    /**
     * 주어진 뉴스들에 대한 내 상태 (#115) — 조회 결과에 읽음/저장을 붙일 때 쓴다. 쿼리 1회.
     */
    List<UserNewsState> findByUserIdAndNewsIds(Long userId, List<Long> newsIds);

    /**
     * 내가 <b>읽은</b> 뉴스의 originalUrl (#115).
     *
     * <p>ES 검색에 "안 읽은 것만"을 적용할 때 제외 대상으로 쓴다. ES 문서의 {@code _id} 가
     * originalUrl 이라 id 대신 URL 이 필요하다.
     */
    List<String> findReadOriginalUrls(Long userId, List<Long> keywordIds, int limit);

    /**
     * 주어진 뉴스들을 모두 읽음 처리한다 (#115 `모두 읽음`).
     *
     * @return 새로 읽음이 된 건수
     */
    int markAllRead(Long userId, List<Long> newsIds);
}
