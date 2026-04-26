package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockNoteCustomTagJpaRepository extends JpaRepository<StockNoteCustomTagEntity, Long> {

    Optional<StockNoteCustomTagEntity> findByUserIdAndTagValue(Long userId, String tagValue);

    long countByUserId(Long userId);

    /**
     * 원자적 usage_count 증가. 존재하지 않으면 0 row updated.
     */
    @Modifying
    @Query("UPDATE StockNoteCustomTagEntity t "
            + "SET t.usageCount = t.usageCount + 1 "
            + "WHERE t.userId = :userId AND t.tagValue = :tagValue")
    int incrementUsage(@Param("userId") Long userId, @Param("tagValue") String tagValue);

    /**
     * 접두어 기반 자동완성. tag_value LIKE :prefix% 로 스캔, usage_count DESC 우선.
     */
    @Query("SELECT t FROM StockNoteCustomTagEntity t "
            + "WHERE t.userId = :userId AND t.tagValue LIKE CONCAT(:prefix, '%') "
            + "ORDER BY t.usageCount DESC, t.tagValue ASC")
    List<StockNoteCustomTagEntity> findTopByPrefix(@Param("userId") Long userId,
                                                   @Param("prefix") String prefix,
                                                   Pageable pageable);
}