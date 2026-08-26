package com.thlee.stock.market.stockmarket.glossary.application.exception;

/**
 * 카테고리가 없거나 현재 사용자 소유가 아닐 때 발생.
 * HTTP 404 로 매핑 (IDOR 노출 방지 위해 403 대신 404 통일 — newsjournal 컨벤션 답습).
 */
public class GlossaryCategoryNotFoundException extends RuntimeException {

    public GlossaryCategoryNotFoundException(Long categoryId) {
        super("GlossaryCategory not found: id=" + categoryId);
    }
}