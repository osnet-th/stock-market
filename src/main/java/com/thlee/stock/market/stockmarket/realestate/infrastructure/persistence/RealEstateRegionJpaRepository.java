package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RealEstateRegionJpaRepository
        extends JpaRepository<RealEstateRegionEntity, RealEstateRegionEntity.RegionId> {

    /**
     * 시군구 단위 행만 조회 (emd_code = "" sentinel).
     */
    List<RealEstateRegionEntity> findAllByEmdCodeOrderByDisplayOrderAsc(String emdCode);

    List<RealEstateRegionEntity> findAllByRegionCodeOrderByDisplayOrderAsc(String regionCode);

    Optional<RealEstateRegionEntity> findByRegionCodeAndEmdCode(String regionCode, String emdCode);
}
