package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteRealEstateRegionJpaRepository
        extends JpaRepository<UserFavoriteRealEstateRegionEntity, Long> {

    List<UserFavoriteRealEstateRegionEntity> findByUserIdOrderByDisplayOrderAsc(Long userId);

    Optional<UserFavoriteRealEstateRegionEntity> findByUserIdAndRegionCodeAndEmdCode(
            Long userId, String regionCode, String emdCode);

    @Modifying
    @Query("delete from UserFavoriteRealEstateRegionEntity f " +
            "where f.userId = :userId and f.regionCode = :regionCode and f.emdCode = :emdCode")
    int deleteByUserAndRegion(@Param("userId") Long userId,
                              @Param("regionCode") String regionCode,
                              @Param("emdCode") String emdCode);

    int countByUserId(Long userId);
}
