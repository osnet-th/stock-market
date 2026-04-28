package com.thlee.stock.market.stockmarket.newsjournal.application.exception;

/**
 * 사건이 없거나 현재 사용자 소유가 아닐 때 발생. HTTP 상태는 404 로 매핑 (security 리뷰 권고:
 * IDOR 노출 방지 위해 403 대신 404 통일).
 */
public class NewsEventNotFoundException extends RuntimeException {

    public NewsEventNotFoundException(Long eventId) {
        super("NewsEvent not found: id=" + eventId);
    }
}