package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.KeywordResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordStatsResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.RegisterKeywordRequest;
import com.thlee.stock.market.stockmarket.news.application.dto.UpdateKeywordRequest;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;

import java.util.List;

public interface KeywordService {

    /**
     * 키워드 등록 (기존 키워드 재사용 + UserKeyword 구독 생성)
     */
    Keyword registerKeyword(RegisterKeywordRequest request);

    /**
     * 키워드 텍스트와 Region으로 등록 (포트폴리오 연동용)
     */
    Keyword registerKeyword(String keyword, Region region, Long userId);

    /**
     * 사용자별 키워드 목록 조회 (user_keyword 기반, active 포함)
     */
    List<KeywordResponse> getKeywordsByUser(Long userId);

    /**
     * 사용자별 활성화된 키워드 목록 조회 (user_keyword.active 기반)
     */
    List<KeywordResponse> getActiveKeywordsByUser(Long userId);

    /**
     * 사용자가 구독 중인 키워드 id 목록 (#115).
     *
     * <p>뉴스 조회·검색의 스코프이자 소유권 검증 기준이다. 활성/비활성을 구분하지 않는다 —
     * 수집이 멈춘 키워드라도 이미 모아둔 기사는 계속 볼 수 있어야 한다.
     */
    List<Long> getSubscribedKeywordIds(Long userId);

    /**
     * 키워드 레일 통계 (#115) — 총 건수 · 오늘 수집 · 마지막 성공 · 7일 스파크라인.
     *
     * <p>집계 쿼리 2회로 전 키워드를 한 번에 만든다.
     */
    KeywordStatsResponse getKeywordStats(Long userId);

    /**
     * 모든 키워드 조회 (스케줄러용)
     */
    List<Keyword> getAllKeywords();

    /**
     * 사용자의 키워드 구독 활성화
     */
    void activateUserKeyword(Long userId, Long keywordId);

    /**
     * 사용자의 키워드 구독 비활성화
     */
    void deactivateUserKeyword(Long userId, Long keywordId);

    /**
     * 사용자의 키워드 구독 해제 (구독자 0명이면 keyword + news 삭제)
     */
    void unsubscribeKeyword(Long userId, Long keywordId);

    /**
     * 키워드 수정 (#115) — 이름·수집 범위·활성 상태.
     *
     * <p>{@code Keyword} 는 공유 리소스이자 불변이라 UPDATE 하지 않는다.
     * 새 {@code (이름, 지역)} 을 구독하고 기존 구독을 해제하는 <b>재구독</b> 방식이며,
     * 단독 구독자일 때는 기존 기사를 새 키워드로 이관해 목업의 "기사 유지" 약속을 지킨다.
     *
     * <p>이름·지역이 그대로면 활성 토글만 반영하고 재구독을 건너뛴다.
     *
     * @return 수정 결과 구독 정보. 재구독이 일어나면 <b>keywordId 가 바뀐다</b>
     */
    KeywordResponse updateKeyword(Long userId, Long keywordId, UpdateKeywordRequest request);

    /**
     * 선택한 키워드 일괄 비활성화 (#115 레일 벌크 `중단`).
     */
    void deactivateUserKeywords(Long userId, List<Long> keywordIds);

    /**
     * 선택한 키워드 일괄 구독 해제 (#115 레일 벌크 `삭제`).
     */
    void unsubscribeKeywords(Long userId, List<Long> keywordIds);
}
