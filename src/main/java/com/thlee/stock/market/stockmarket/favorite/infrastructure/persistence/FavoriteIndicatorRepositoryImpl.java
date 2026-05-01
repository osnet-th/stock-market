package com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteDisplayMode;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import com.thlee.stock.market.stockmarket.favorite.domain.repository.FavoriteIndicatorRepository;
import com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence.mapper.FavoriteIndicatorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FavoriteIndicatorRepositoryImpl implements FavoriteIndicatorRepository {

    private final UserFavoriteIndicatorJpaRepository jpaRepository;
    private final FavoriteIndicatorMapper mapper;

    @Override
    public void save(FavoriteIndicator favoriteIndicator) {
        jpaRepository.save(mapper.toEntity(favoriteIndicator));
    }

    @Override
    public int deleteByUserIdAndSourceTypeAndIndicatorCode(Long userId,
                                                            FavoriteIndicatorSourceType sourceType,
                                                            String indicatorCode) {
        return jpaRepository.deleteByUserIdAndSourceTypeAndIndicatorCode(userId, sourceType, indicatorCode);
    }

    @Override
    public List<FavoriteIndicator> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<FavoriteIndicator> findByUserIdAndSourceType(Long userId, FavoriteIndicatorSourceType sourceType) {
        return jpaRepository.findByUserIdAndSourceType(userId, sourceType).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public int updateDisplayMode(Long userId,
                                 FavoriteIndicatorSourceType sourceType,
                                 String indicatorCode,
                                 FavoriteDisplayMode displayMode) {
        return jpaRepository.updateDisplayMode(userId, sourceType, indicatorCode, displayMode);
    }
}