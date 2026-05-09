package com.thlee.stock.market.stockmarket.favorite.domain.repository;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteDisplayMode;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;

import java.util.List;

/**
 * 관심 지표 저장소 (도메인 포트)
 */
public interface FavoriteIndicatorRepository {

    void save(FavoriteIndicator favoriteIndicator);

    /**
     * 신규 관심지표를 단일 SQL로 INSERT — priority는 그룹의 MAX(priority)+1로 산출된다.
     * 호출자는 SQLState 23505 (UNIQUE 위반) 발생 시 1회 retry해야 한다.
     */
    void insertWithNextPriority(Long userId,
                                FavoriteIndicatorSourceType sourceType,
                                String indicatorCode);

    int deleteByUserIdAndSourceTypeAndIndicatorCode(Long userId,
                                                     FavoriteIndicatorSourceType sourceType,
                                                     String indicatorCode);

    List<FavoriteIndicator> findByUserId(Long userId);

    List<FavoriteIndicator> findByUserIdAndSourceType(Long userId, FavoriteIndicatorSourceType sourceType);

    int updateDisplayMode(Long userId,
                          FavoriteIndicatorSourceType sourceType,
                          String indicatorCode,
                          FavoriteDisplayMode displayMode);

    /**
     * 일괄 reorder를 위한 잠금 조회. 호출 트랜잭션 안에서 (user_id, sourceType) 그룹을 PESSIMISTIC_WRITE로 잠근다.
     */
    List<FavoriteIndicator> findForReorderUpdate(Long userId, FavoriteIndicatorSourceType sourceType);

    /**
     * (id → 새 priority) 맵을 일괄 적용. flush + clear까지 수행해 후속 read가 stale 결과를 반환하지 않는다.
     */
    int bulkUpdatePriority(java.util.Map<Long, Integer> idToPriority);

    /**
     * Post-write invariant 검증용 priority projection. lock 미사용.
     */
    List<Integer> findPriorities(Long userId, FavoriteIndicatorSourceType sourceType);
}