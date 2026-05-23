package com.thlee.stock.market.stockmarket.realestate.domain.repository;

import com.thlee.stock.market.stockmarket.realestate.domain.model.UserFavoriteRealEstateRegion;

import java.util.List;
import java.util.Optional;

public interface UserFavoriteRealEstateRegionRepository {

    List<UserFavoriteRealEstateRegion> findByUserId(Long userId);

    Optional<UserFavoriteRealEstateRegion> findOne(Long userId, String regionCode, String emdCode);

    UserFavoriteRealEstateRegion save(UserFavoriteRealEstateRegion favorite);

    int delete(Long userId, String regionCode, String emdCode);

    int countByUser(Long userId);
}
