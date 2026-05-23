package com.thlee.stock.market.stockmarket.realestate.domain.repository;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;

import java.util.List;
import java.util.Optional;

public interface RealEstateRegionRepository {

    List<RealEstateRegion> findAll();

    /**
     * 시군구 단위만 조회 — emdCode 빈 문자열인 행만 반환.
     */
    List<RealEstateRegion> findAllSigunguOnly();

    /**
     * 시군구 하위 읍면동 목록.
     */
    List<RealEstateRegion> findEmdsByRegionCode(String regionCode);

    Optional<RealEstateRegion> findByRegionCodeAndEmdCode(String regionCode, String emdCode);

    void saveAll(List<RealEstateRegion> regions);

    long count();
}
