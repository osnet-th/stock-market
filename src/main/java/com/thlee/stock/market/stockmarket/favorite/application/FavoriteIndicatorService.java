package com.thlee.stock.market.stockmarket.favorite.application;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;
import com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorQueryService;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import com.thlee.stock.market.stockmarket.favorite.domain.repository.FavoriteIndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteIndicatorService {

    private final FavoriteIndicatorRepository favoriteIndicatorRepository;
    private final EcosIndicatorService ecosIndicatorService;
    private final GlobalIndicatorQueryService globalIndicatorQueryService;

    /**
     * 관심 지표 토글 (등록/해제)
     * @return true: 등록됨, false: 해제됨
     */
    @Transactional
    public boolean toggle(Long userId, FavoriteIndicatorSourceType sourceType, String indicatorCode) {
        int deleted = favoriteIndicatorRepository.deleteByUserIdAndSourceTypeAndIndicatorCode(
            userId, sourceType, indicatorCode);
        if (deleted > 0) {
            return false;
        }
        try {
            favoriteIndicatorRepository.save(FavoriteIndicator.create(userId, sourceType, indicatorCode));
            return true;
        } catch (DataIntegrityViolationException e) {
            return true;
        }
    }

    @Transactional(readOnly = true)
    public List<FavoriteIndicator> findByUserId(Long userId) {
        return favoriteIndicatorRepository.findByUserId(userId);
    }

    /**
     * 관심 지표 + Latest 데이터 통합 조회 (대시보드용)
     */
    @Transactional(readOnly = true)
    public EnrichedFavorites findEnrichedByUserId(Long userId) {
        List<FavoriteIndicator> favorites = favoriteIndicatorRepository.findByUserId(userId);

        List<FavoriteIndicator> ecosFavorites = favorites.stream()
            .filter(f -> f.getSourceType() == FavoriteIndicatorSourceType.ECOS)
            .toList();

        List<FavoriteIndicator> globalFavorites = favorites.stream()
            .filter(f -> f.getSourceType() == FavoriteIndicatorSourceType.GLOBAL)
            .toList();

        // ECOS Latest 배치 조회 + in-memory join
        List<EnrichedEcosFavorite> enrichedEcos = List.of();
        if (!ecosFavorites.isEmpty()) {
            Map<String, EcosIndicatorLatest> latestMap = ecosIndicatorService.findAllLatest().stream()
                .collect(Collectors.toMap(EcosIndicatorLatest::toCompareKey, l -> l, (a, b) -> a));

            enrichedEcos = ecosFavorites.stream()
                .map(fav -> {
                    EcosIndicatorLatest latest = latestMap.get(fav.getIndicatorCode());
                    return new EnrichedEcosFavorite(fav, latest);
                })
                .toList();
        }

        // Global Latest 배치 조회 + in-memory join
        List<EnrichedGlobalFavorite> enrichedGlobal = List.of();
        if (!globalFavorites.isEmpty()) {
            Map<String, GlobalIndicatorLatest> latestMap = globalIndicatorQueryService.findAllLatest().stream()
                .collect(Collectors.toMap(GlobalIndicatorLatest::toCompareKey, l -> l, (a, b) -> a));

            enrichedGlobal = globalFavorites.stream()
                .map(fav -> {
                    GlobalIndicatorLatest latest = latestMap.get(fav.getIndicatorCode());
                    return new EnrichedGlobalFavorite(fav, latest);
                })
                .toList();
        }

        return new EnrichedFavorites(enrichedEcos, enrichedGlobal);
    }

    public record EnrichedEcosFavorite(FavoriteIndicator favorite, EcosIndicatorLatest latest) {}
    public record EnrichedGlobalFavorite(FavoriteIndicator favorite, GlobalIndicatorLatest latest) {}
    public record EnrichedFavorites(List<EnrichedEcosFavorite> ecos, List<EnrichedGlobalFavorite> global) {}
}