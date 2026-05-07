package com.thlee.stock.market.stockmarket.realestate.application;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateRegionRepository;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.RegionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 시도/시군구/읍면동 region 메타 조회.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RealEstateRegionService {

    private final RealEstateRegionRepository regionRepository;

    public RegionResponse listSidoGrouped() {
        Map<String, RegionResponse.SidoGroup> grouped = new LinkedHashMap<>();
        for (RealEstateRegion region : regionRepository.findAllSigunguOnly()) {
            grouped.computeIfAbsent(region.getSidoCode(), code ->
                    new RegionResponse.SidoGroup(code, region.getSidoName(), new java.util.ArrayList<>()));
            grouped.get(region.getSidoCode()).sigungus().add(
                    new RegionResponse.Sigungu(region.getRegionCode(), region.getSigunguName()));
        }
        return new RegionResponse(List.copyOf(grouped.values()));
    }

    public List<RegionResponse.Emd> listEmds(String regionCode) {
        return regionRepository.findEmdsByRegionCode(regionCode).stream()
                .map(r -> new RegionResponse.Emd(r.getEmdCode(), r.getEmdName()))
                .toList();
    }

    public Optional<RealEstateRegion> findRegion(String regionCode) {
        return regionRepository.findByRegionCodeAndEmdCode(regionCode, "");
    }

    public String regionLabel(String regionCode) {
        return findRegion(regionCode)
                .map(r -> r.getSidoName() + " " + r.getSigunguName())
                .orElse(regionCode);
    }
}