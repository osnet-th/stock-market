package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpendingItemJpaRepository extends JpaRepository<SpendingItemEntity, Long> {

    List<SpendingItemEntity> findBySetIdOrderBySortOrderAsc(Long setId);

    /** 세트 스냅샷 교체용 벌크 삭제 */
    @Modifying
    @Query("DELETE FROM SpendingItemEntity i WHERE i.setId = :setId")
    void deleteBySetId(@Param("setId") Long setId);
}
