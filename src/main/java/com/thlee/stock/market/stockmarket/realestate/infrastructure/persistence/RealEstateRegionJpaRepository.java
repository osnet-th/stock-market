package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RealEstateRegionJpaRepository
        extends JpaRepository<RealEstateRegionEntity, RealEstateRegionEntity.RegionId> {

    /**
     * region 단위(SIDO/SIGUNGU) 기준 조회. 기존 emd_code="" sentinel 기반 분기를
     * 대체하는 권위 메소드 — SIDO row와 SIGUNGU row 모두 emd_code=""이므로
     * level predicate만이 정확히 구분한다.
     */
    List<RealEstateRegionEntity> findAllByLevelOrderByDisplayOrderAsc(RegionLevel level);

    List<RealEstateRegionEntity> findAllByRegionCodeOrderByDisplayOrderAsc(String regionCode);

    Optional<RealEstateRegionEntity> findByRegionCodeAndEmdCode(String regionCode, String emdCode);

    /**
     * level 컬럼이 NULL인 row 수. 마이그레이션 backfill 검증용 — 부팅 시
     * {@code @PostLoad} fail-fast가 부풀려 던지기 전에 명확한 메시지로 사전 차단.
     * native query로 작성하여 JPA hydration을 우회한다.
     */
    @Query(value = "SELECT COUNT(*) FROM real_estate_region WHERE level IS NULL", nativeQuery = true)
    long countMissingLevel();
}