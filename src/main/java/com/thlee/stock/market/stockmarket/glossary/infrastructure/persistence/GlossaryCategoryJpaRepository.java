package com.thlee.stock.market.stockmarket.glossary.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GlossaryCategoryJpaRepository extends JpaRepository<GlossaryCategoryEntity, Long> {

    Optional<GlossaryCategoryEntity> findByUserIdAndName(Long userId, String name);

    Optional<GlossaryCategoryEntity> findByIdAndUserId(Long id, Long userId);

    List<GlossaryCategoryEntity> findByUserIdOrderByNameAsc(Long userId);

    @Modifying
    @Query("DELETE FROM GlossaryCategoryEntity c WHERE c.id = :id AND c.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}