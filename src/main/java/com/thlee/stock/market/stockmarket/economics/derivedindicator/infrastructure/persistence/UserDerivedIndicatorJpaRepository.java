package com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDerivedIndicatorJpaRepository
        extends JpaRepository<UserDerivedIndicatorEntity, Long> {

    @Query("SELECT e FROM UserDerivedIndicatorEntity e " +
           "WHERE e.userId = :userId ORDER BY e.id ASC")
    List<UserDerivedIndicatorEntity> findByUserId(@Param("userId") Long userId);

    Optional<UserDerivedIndicatorEntity> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM UserDerivedIndicatorEntity e WHERE e.id = :id AND e.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
