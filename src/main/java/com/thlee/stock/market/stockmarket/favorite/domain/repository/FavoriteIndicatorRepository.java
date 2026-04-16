package com.thlee.stock.market.stockmarket.favorite.domain.repository;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;

import java.util.List;

/**
 * 관심 지표 저장소 (도메인 포트)
 */
public interface FavoriteIndicatorRepository {

    void save(FavoriteIndicator favoriteIndicator);

    int deleteByUserIdAndSourceTypeAndIndicatorCode(Long userId,
                                                     FavoriteIndicatorSourceType sourceType,
                                                     String indicatorCode);

    List<FavoriteIndicator> findByUserId(Long userId);

    List<FavoriteIndicator> findByUserIdAndSourceType(Long userId, FavoriteIndicatorSourceType sourceType);
}