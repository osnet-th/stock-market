package com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFavoriteIndicatorJpaRepository
    extends JpaRepository<UserFavoriteIndicatorEntity, Long>, UserFavoriteIndicatorJpaRepositoryCustom {

    @Query("SELECT e FROM UserFavoriteIndicatorEntity e " +
           "WHERE e.userId = :userId " +
           "ORDER BY e.priority ASC NULLS LAST, e.id ASC")
    List<UserFavoriteIndicatorEntity> findByUserId(@Param("userId") Long userId);

    @Query("SELECT e FROM UserFavoriteIndicatorEntity e " +
           "WHERE e.userId = :userId AND e.sourceType = :sourceType " +
           "ORDER BY e.priority ASC NULLS LAST, e.id ASC")
    List<UserFavoriteIndicatorEntity> findByUserIdAndSourceType(@Param("userId") Long userId,
                                                                @Param("sourceType") FavoriteIndicatorSourceType sourceType);

    /**
     * 일괄 reorder를 위한 잠금 조회. 트랜잭션 안에서 (user_id, source_type) 그룹을 PESSIMISTIC_WRITE로 잠근다.
     * 후속 bulk update의 UNIQUE DEFERRABLE 충돌을 막고 동시 reorder를 직렬화한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM UserFavoriteIndicatorEntity e " +
           "WHERE e.userId = :userId AND e.sourceType = :sourceType " +
           "ORDER BY e.priority ASC NULLS LAST, e.id ASC")
    List<UserFavoriteIndicatorEntity> findForReorderUpdate(@Param("userId") Long userId,
                                                           @Param("sourceType") FavoriteIndicatorSourceType sourceType);

    /**
     * Post-write invariant 검증용 priority 목록 projection. lock 미사용.
     * 정렬 일관성을 위해 NULLS LAST를 명시한다(H2 등 다른 RDBMS의 NULLS-FIRST 기본값과 차이 회피).
     */
    @Query("SELECT e.priority FROM UserFavoriteIndicatorEntity e " +
           "WHERE e.userId = :userId AND e.sourceType = :sourceType " +
           "ORDER BY e.priority ASC NULLS LAST, e.id ASC")
    List<Integer> findPrioritiesByUserIdAndSourceType(@Param("userId") Long userId,
                                                     @Param("sourceType") FavoriteIndicatorSourceType sourceType);

    @Modifying
    @Query("DELETE FROM UserFavoriteIndicatorEntity e " +
           "WHERE e.userId = :userId AND e.sourceType = :sourceType AND e.indicatorCode = :indicatorCode")
    int deleteByUserIdAndSourceTypeAndIndicatorCode(@Param("userId") Long userId,
                                                     @Param("sourceType") FavoriteIndicatorSourceType sourceType,
                                                     @Param("indicatorCode") String indicatorCode);

    /**
     * 신규 관심지표를 단일 SQL로 INSERT — priority는 동일 (user_id, source_type) 그룹의 MAX(priority)+1로 산출.
     * READ_COMMITTED isolation에서 SELECT MAX → 별도 INSERT는 race를 못 막으므로 단일 statement로 atomicity 확보.
     * 동시 트랜잭션이 같은 priority를 도출하면 DEFERRABLE UNIQUE가 commit 시 SQLState 23505로 거부 → 호출자가 1회 retry.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT INTO user_favorite_indicator " +
                   "(user_id, source_type, indicator_code, display_mode, priority, created_at) " +
                   "SELECT :userId, :sourceType, :indicatorCode, 'INDICATOR', " +
                   "       COALESCE(MAX(priority), -1) + 1, NOW() " +
                   "FROM user_favorite_indicator " +
                   "WHERE user_id = :userId AND source_type = :sourceType",
           nativeQuery = true)
    int insertWithNextPriority(@Param("userId") Long userId,
                               @Param("sourceType") String sourceType,
                               @Param("indicatorCode") String indicatorCode);
}
