package com.thlee.stock.market.stockmarket.glossary.domain.repository;

import com.thlee.stock.market.stockmarket.glossary.domain.model.GlossaryCategory;

import java.util.List;
import java.util.Optional;

/**
 * 용어 사전 카테고리 Repository 포트.
 *
 * <p>모든 조회/저장/삭제는 {@code userId} 스코프로 제한된다 (소유권 격리).
 */
public interface GlossaryCategoryRepository {

    GlossaryCategory save(GlossaryCategory category);

    Optional<GlossaryCategory> findByUserIdAndName(Long userId, String name);

    /** 404 통일을 위해 Optional 반환. 타사용자 ID 도 empty. */
    Optional<GlossaryCategory> findByIdAndUserId(Long id, Long userId);

    /** 사용자 카테고리 목록 (가나다순). 사이드바/필터 셀렉트용. */
    List<GlossaryCategory> findByUserIdOrderByNameAsc(Long userId);

    /** 단건 삭제. 영향받은 row 수 반환. */
    int deleteByIdAndUserId(Long id, Long userId);
}