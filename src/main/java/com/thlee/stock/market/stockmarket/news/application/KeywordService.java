package com.thlee.stock.market.stockmarket.news.application;

import com.thlee.stock.market.stockmarket.news.application.dto.KeywordResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.RegisterKeywordRequest;
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
}
