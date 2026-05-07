package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.UserFavoriteRealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.UserFavoriteRealEstateRegionRepository;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper.UserFavoriteRealEstateRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserFavoriteRealEstateRegionRepositoryImpl
        implements UserFavoriteRealEstateRegionRepository {

    private static final String SIGUNGU_SENTINEL = "";

    private final UserFavoriteRealEstateRegionJpaRepository jpaRepository;
    private final UserFavoriteRealEstateRegionMapper mapper;

    @Override
    public List<UserFavoriteRealEstateRegion> findByUserId(Long userId) {
        return jpaRepository.findByUserIdOrderByDisplayOrderAsc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<UserFavoriteRealEstateRegion> findOne(Long userId, String regionCode, String emdCode) {
        return jpaRepository
                .findByUserIdAndRegionCodeAndEmdCode(userId, regionCode, normalize(emdCode))
                .map(mapper::toDomain);
    }

    @Override
    public UserFavoriteRealEstateRegion save(UserFavoriteRealEstateRegion favorite) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(favorite)));
    }

    @Override
    public int delete(Long userId, String regionCode, String emdCode) {
        return jpaRepository.deleteByUserAndRegion(userId, regionCode, normalize(emdCode));
    }

    @Override
    public int countByUser(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    private String normalize(String emdCode) {
        return emdCode == null ? SIGUNGU_SENTINEL : emdCode;
    }
}
