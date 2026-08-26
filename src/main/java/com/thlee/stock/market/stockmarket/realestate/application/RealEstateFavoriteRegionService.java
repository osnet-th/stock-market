package com.thlee.stock.market.stockmarket.realestate.application;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.model.UserFavoriteRealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateRegionRepository;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.UserFavoriteRealEstateRegionRepository;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.FavoriteRegionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 관심지역 등록/해제/조회.
 * <p>
 * 즐겨찾기 단위는 region_code + emdCode("" sentinel) — 주택유형/면적 미포함.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealEstateFavoriteRegionService {

    private static final String SIGUNGU_SENTINEL = "";

    private final UserFavoriteRealEstateRegionRepository favoriteRepository;
    private final RealEstateRegionRepository regionRepository;

    @Transactional
    public UserFavoriteRealEstateRegion register(Long userId, String regionCode, String emdCode) {
        String normalizedEmd = normalize(emdCode);
        validateRegionExists(regionCode, normalizedEmd);

        return favoriteRepository.findOne(userId, regionCode, normalizedEmd)
                .orElseGet(() -> persistOrRecover(userId, regionCode, normalizedEmd));
    }

    private UserFavoriteRealEstateRegion persistOrRecover(Long userId,
                                                          String regionCode,
                                                          String normalizedEmd) {
        UserFavoriteRealEstateRegion candidate = buildCandidate(userId, regionCode, normalizedEmd);
        try {
            return favoriteRepository.save(candidate);
        } catch (DataIntegrityViolationException e) {
            return recoverDuplicate(userId, regionCode, normalizedEmd, e);
        }
    }

    private UserFavoriteRealEstateRegion buildCandidate(Long userId,
                                                        String regionCode,
                                                        String normalizedEmd) {
        int order = favoriteRepository.countByUser(userId);
        return UserFavoriteRealEstateRegion.builder()
                .userId(userId)
                .sidoCode(RegionCode.of(regionCode).sidoCode())
                .regionCode(regionCode)
                .emdCode(normalizedEmd)
                .displayOrder(order)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserFavoriteRealEstateRegion recoverDuplicate(Long userId,
                                                          String regionCode,
                                                          String normalizedEmd,
                                                          DataIntegrityViolationException cause) {
        return favoriteRepository.findOne(userId, regionCode, normalizedEmd)
                .orElseThrow(() -> cause);
    }

    @Transactional
    public boolean unregister(Long userId, String regionCode, String emdCode) {
        return favoriteRepository.delete(userId, regionCode, normalize(emdCode)) > 0;
    }

    @Transactional(readOnly = true)
    public FavoriteRegionResponse list(Long userId) {
        List<UserFavoriteRealEstateRegion> favorites = favoriteRepository.findByUserId(userId);
        if (favorites.isEmpty()) {
            return new FavoriteRegionResponse(List.of());
        }
        Map<String, RealEstateRegion> regionMap = loadRegionMap(favorites);
        List<FavoriteRegionResponse.Item> items = favorites.stream()
                .map(fav -> {
                    RealEstateRegion region = regionMap.get(fav.getRegionCode() + "::" + fav.getEmdCode());
                    return new FavoriteRegionResponse.Item(
                            fav.getId(),
                            fav.getRegionCode(),
                            region != null ? region.getSidoName() : null,
                            region != null ? region.getSigunguName() : null,
                            SIGUNGU_SENTINEL.equals(fav.getEmdCode()) ? null : fav.getEmdCode(),
                            region != null ? region.getEmdName() : null,
                            fav.getDisplayOrder(),
                            fav.getCreatedAt() == null ? null : fav.getCreatedAt().toString());
                })
                .toList();
        return new FavoriteRegionResponse(items);
    }

    private void validateRegionExists(String regionCode, String emdCode) {
        regionRepository.findByRegionCodeAndEmdCode(regionCode, emdCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown region: " + regionCode + "/" + emdCode));
    }

    private Map<String, RealEstateRegion> loadRegionMap(List<UserFavoriteRealEstateRegion> favorites) {
        Map<String, RealEstateRegion> map = new HashMap<>();
        for (UserFavoriteRealEstateRegion fav : favorites) {
            String key = fav.getRegionCode() + "::" + fav.getEmdCode();
            if (map.containsKey(key)) continue;
            regionRepository.findByRegionCodeAndEmdCode(fav.getRegionCode(), fav.getEmdCode())
                    .ifPresent(region -> map.put(key, region));
        }
        return map;
    }

    private String normalize(String emdCode) {
        return emdCode == null || emdCode.isBlank() ? SIGUNGU_SENTINEL : emdCode;
    }
}