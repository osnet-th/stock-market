package com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteDisplayMode;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFavoriteIndicatorJpaRepository extends JpaRepository<UserFavoriteIndicatorEntity, Long> {

    List<UserFavoriteIndicatorEntity> findByUserId(Long userId);

    List<UserFavoriteIndicatorEntity> findByUserIdAndSourceType(Long userId, FavoriteIndicatorSourceType sourceType);

    @Modifying
    @Query("DELETE FROM UserFavoriteIndicatorEntity e " +
           "WHERE e.userId = :userId AND e.sourceType = :sourceType AND e.indicatorCode = :indicatorCode")
    int deleteByUserIdAndSourceTypeAndIndicatorCode(@Param("userId") Long userId,
                                                     @Param("sourceType") FavoriteIndicatorSourceType sourceType,
                                                     @Param("indicatorCode") String indicatorCode);

    @Modifying
    @Query("UPDATE UserFavoriteIndicatorEntity e " +
           "SET e.displayMode = :displayMode " +
           "WHERE e.userId = :userId AND e.sourceType = :sourceType AND e.indicatorCode = :indicatorCode")
    int updateDisplayMode(@Param("userId") Long userId,
                          @Param("sourceType") FavoriteIndicatorSourceType sourceType,
                          @Param("indicatorCode") String indicatorCode,
                          @Param("displayMode") FavoriteDisplayMode displayMode);
}
