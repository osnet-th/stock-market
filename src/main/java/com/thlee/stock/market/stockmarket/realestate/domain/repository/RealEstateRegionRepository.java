package com.thlee.stock.market.stockmarket.realestate.domain.repository;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;

import java.util.List;
import java.util.Optional;

public interface RealEstateRegionRepository {

    List<RealEstateRegion> findAll();

    /**
     * 시군구 단위만 조회 — level=SIGUNGU 행만 반환.
     */
    List<RealEstateRegion> findAllSigunguOnly();

    /**
     * 시도(광역) 단위만 조회 — level=SIDO 행만 반환.
     */
    List<RealEstateRegion> findAllSidoOnly();

    /**
     * 시군구 하위 읍면동 목록.
     */
    List<RealEstateRegion> findEmdsByRegionCode(String regionCode);

    Optional<RealEstateRegion> findByRegionCodeAndEmdCode(String regionCode, String emdCode);

    void saveAll(List<RealEstateRegion> regions);

    long count();

    /**
     * level 컬럼이 NULL인 row 수. 마이그레이션 backfill 검증용.
     */
    long countMissingLevel();
}
