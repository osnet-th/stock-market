package com.thlee.stock.market.stockmarket.glossary.domain.repository;

/**
 * 용어 목록 정렬 키.
 *
 * <p>secondary tie-breaker 는 {@code id} (등록일순은 id DESC, 가나다순은 id ASC) 로
 * 안정 정렬을 보장한다. 정렬 키 변경 시 {@link GlossaryTermListFilter} 의 default 도 확인.
 */
public enum GlossaryTermSort {
    /** 등록일 최신순 (created_at DESC, id DESC) — 기본값 */
    REGISTERED_DESC,
    /** 등록일 오래된순 (created_at ASC, id ASC) */
    REGISTERED_ASC,
    /** 가나다순 (name ASC, id ASC) */
    ALPHA_ASC
}
