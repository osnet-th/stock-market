package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GlossaryTermJpaRepository extends JpaRepository<GlossaryTermEntity, Long> {

    Optional<GlossaryTermEntity> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM GlossaryTermEntity t WHERE t.id = :id AND t.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    long countByUserIdAndCategoryId(Long userId, Long categoryId);

    /**
     * R6 cascade: 카테고리 삭제 시 자식 용어의 categoryId 를 null 로 일괄 set.
     *
     * <p>{@code clearAutomatically/flushAutomatically=true} 로 Hibernate Persistence Context
     * (L1/Session cache) 무효화 — 같은 트랜잭션에서 동일 term 재조회 시 fresh data 반환.
     *
     * <p>{@code @PreUpdate} 가 JPQL UPDATE 에는 발동하지 않으므로 {@code updatedAt} 을 SET 절에 명시.
     *
     * @return 영향받은 row 수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE GlossaryTermEntity t
               SET t.categoryId = null,
                   t.updatedAt = CURRENT_TIMESTAMP
             WHERE t.userId = :userId
               AND t.categoryId = :categoryId
            """)
    int reassignCategoryToNull(@Param("userId") Long userId,
                                @Param("categoryId") Long categoryId);

    /**
     * 용어 목록 조회. 옵셔널 필터:
     * <ul>
     *   <li>{@code q} (용어명) — null/blank 면 비활성, 아니면 {@code LOWER(name) LIKE LOWER(:q) ESCAPE '\\'}</li>
     *   <li>{@code definitionQ} — 동일 패턴</li>
     *   <li>{@code categoryId} — null = 비활성, 값 = 일치</li>
     *   <li>{@code uncategorizedOnly} — true 면 categoryId IS NULL</li>
     * </ul>
     *
     * <p>정렬은 호출자가 Pageable.Sort 로 주입 (REGISTERED_DESC/REGISTERED_ASC/ALPHA_ASC).
     */
    @Query("""
            SELECT t FROM GlossaryTermEntity t
             WHERE t.userId = :userId
               AND (:q IS NULL OR LOWER(t.name) LIKE LOWER(:q) ESCAPE '\\')
               AND (:definitionQ IS NULL OR LOWER(t.definition) LIKE LOWER(:definitionQ) ESCAPE '\\')
               AND (
                    (:uncategorizedOnly = TRUE AND t.categoryId IS NULL)
                 OR (:uncategorizedOnly = FALSE AND (:categoryId IS NULL OR t.categoryId = :categoryId))
               )
            """)
    List<GlossaryTermEntity> findList(
            @Param("userId") Long userId,
            @Param("q") String q,
            @Param("definitionQ") String definitionQ,
            @Param("categoryId") Long categoryId,
            @Param("uncategorizedOnly") boolean uncategorizedOnly,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(t) FROM GlossaryTermEntity t
             WHERE t.userId = :userId
               AND (:q IS NULL OR LOWER(t.name) LIKE LOWER(:q) ESCAPE '\\')
               AND (:definitionQ IS NULL OR LOWER(t.definition) LIKE LOWER(:definitionQ) ESCAPE '\\')
               AND (
                    (:uncategorizedOnly = TRUE AND t.categoryId IS NULL)
                 OR (:uncategorizedOnly = FALSE AND (:categoryId IS NULL OR t.categoryId = :categoryId))
               )
            """)
    long countList(
            @Param("userId") Long userId,
            @Param("q") String q,
            @Param("definitionQ") String definitionQ,
            @Param("categoryId") Long categoryId,
            @Param("uncategorizedOnly") boolean uncategorizedOnly
    );
}