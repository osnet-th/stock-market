package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateRegionRepository;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper.RealEstateRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RealEstateRegionRepositoryImpl implements RealEstateRegionRepository {

    private static final String SIGUNGU_SENTINEL = "";

    private final RealEstateRegionJpaRepository jpaRepository;
    private final RealEstateRegionMapper mapper;

    @Override
    public List<RealEstateRegion> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RealEstateRegion> findAllSigunguOnly() {
        return jpaRepository.findAllByEmdCodeOrderByDisplayOrderAsc(SIGUNGU_SENTINEL).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RealEstateRegion> findEmdsByRegionCode(String regionCode) {
        return jpaRepository.findAllByRegionCodeOrderByDisplayOrderAsc(regionCode).stream()
                .filter(entity -> !SIGUNGU_SENTINEL.equals(entity.getEmdCode()))
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RealEstateRegion> findByRegionCodeAndEmdCode(String regionCode, String emdCode) {
        String normalizedEmd = emdCode == null ? SIGUNGU_SENTINEL : emdCode;
        return jpaRepository
                .findByRegionCodeAndEmdCode(regionCode, normalizedEmd)
                .map(mapper::toDomain);
    }

    @Override
    public void saveAll(List<RealEstateRegion> regions) {
        List<RealEstateRegionEntity> entities = regions.stream()
                .map(mapper::toEntity)
                .toList();
        jpaRepository.saveAll(entities);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }
}
