package com.thlee.stock.market.stockmarket.glossary.domain.repository;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryTerm;

import java.util.List;
import java.util.Optional;

/**
 * 용어 사전 본체 Repository 포트.
 *
 * <p>모든 조회/저장/삭제는 {@code userId} 스코프로 제한된다 (IDOR 방지 — 404 통일).
 */
public interface GlossaryTermRepository {

    GlossaryTerm save(GlossaryTerm term);

    /** 권한 있는 사용자만 조회 가능. 타사용자 ID 도 empty (404 통일). */
    Optional<GlossaryTerm> findByIdAndUserId(Long id, Long userId);

    /** 필터/정렬/페이징 리스트. */
    List<GlossaryTerm> findList(Long userId, GlossaryTermListFilter filter);

    /** 동일 필터 기준 총 건수. 페이지네이션 응답 totalCount 용. */
    long countList(Long userId, GlossaryTermListFilter filter);

    /** 단건 삭제. */
    int deleteByIdAndUserId(Long id, Long userId);

    /** 카테고리 삭제 미리보기 N건 산출용. */
    long countByUserIdAndCategoryId(Long userId, Long categoryId);

    /**
     * R6 cascade: 해당 카테고리에 속한 본인 용어들의 {@code categoryId} 를 null 로 일괄 set.
     * 단일 트랜잭션에서 카테고리 삭제 직전 호출된다.
     *
     * @return 영향받은 row 수
     */
    int reassignCategoryToNull(Long userId, Long categoryId);
}