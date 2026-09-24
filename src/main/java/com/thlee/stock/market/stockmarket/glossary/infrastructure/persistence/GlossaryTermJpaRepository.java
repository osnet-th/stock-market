package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GlossaryTermJpaRepository extends JpaRepository<GlossaryTermEntity, Long> {

    Optional<GlossaryTermEntity> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM GlossaryTermEntity t WHERE t.id = :id AND t.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 함께 볼 용어 소유권 검증용 — 후보 id 중 해당 사용자 소유 용어 id 만 반환.
     * 호출자는 빈 컬렉션 전달 금지 (Hibernate 빈 IN 렌더링 회피 — 어댑터에서 guard).
     */
    @Query("SELECT t.id FROM GlossaryTermEntity t WHERE t.userId = :userId AND t.id IN :ids")
    List<Long> findOwnedIds(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    /**
     * 용어 삭제 시 함께 볼 용어 관계 양방향 정리.
     *
     * <p>@ElementCollection 은 JPQL 대상이 아니고, {@link #deleteByIdAndUserId} 의 JPQL 벌크
     * DELETE 는 컬렉션 행을 지우지 않으므로 native 로 직접 정리한다.
     * 참조받는 쪽(related_term_id) 행도 같은 사용자 용어에서만 생길 수 있어
     * (저장 시 소유권 검증) userId 조건 없이 안전하다. 단, 소유권이 확인된 삭제
     * 성공 이후에만 호출한다 (어댑터 계약).
     *
     * <p>{@code clearAutomatically=true} — 같은 트랜잭션에서 관계 보유 용어를 재조회할 때
     * L1 캐시의 stale 컬렉션 반환 방지.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM glossary_term_related WHERE term_id = :termId OR related_term_id = :termId",
            nativeQuery = true)
    int deleteRelationsForTerm(@Param("termId") Long termId);

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
     *   <li>{@code q} (용어명) — null/blank 면 비활성, 아니면 {@code LOWER(name) LIKE :q ESCAPE '\\'}
     *       ({@code :q} 는 application 계층에서 이미 소문자/와일드카드 escape된 패턴)</li>
     *   <li>{@code definitionQ} — 동일 패턴</li>
     *   <li>{@code categoryId} — null = 비활성, 값 = 일치</li>
     *   <li>{@code uncategorizedOnly} — true 면 categoryId IS NULL</li>
     * </ul>
     *
     * <p>주의: 패턴 측에 {@code LOWER(:q)} 를 호출하지 않는다. PostgreSQL prepared statement 가
     * null 파라미터를 {@code bytea} 로 추론해 {@code lower(?)} 가 실패하기 때문.
     * 대소문자 무시는 application 계층 {@code LikeEscaper.toContainsPattern()} 에서 이미 보장.
     *
     * <p>정렬은 호출자가 Pageable.Sort 로 주입 (REGISTERED_DESC/REGISTERED_ASC/ALPHA_ASC).
     */
    @Query("""
            SELECT t FROM GlossaryTermEntity t
             WHERE t.userId = :userId
               AND (:q IS NULL OR LOWER(t.name) LIKE :q ESCAPE '\\')
               AND (:definitionQ IS NULL OR LOWER(t.definition) LIKE :definitionQ ESCAPE '\\')
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
               AND (:q IS NULL OR LOWER(t.name) LIKE :q ESCAPE '\\')
               AND (:definitionQ IS NULL OR LOWER(t.definition) LIKE :definitionQ ESCAPE '\\')
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