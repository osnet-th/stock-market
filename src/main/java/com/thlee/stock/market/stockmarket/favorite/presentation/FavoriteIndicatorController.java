package com.thlee.stock.market.stockmarket.favorite.presentation;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedFavorites;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedGlobalFavorite;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.EnrichedFavoriteResponse;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.FavoriteDisplayModeRequest;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.FavoriteIndicatorResponse;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.FavoriteToggleRequest;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.GlobalRefreshResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 관심 지표 API
 */
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteIndicatorController {

    private final FavoriteIndicatorService favoriteIndicatorService;

    /**
     * 관심 지표 등록 (이미 존재하면 200 OK)
     */
    @PostMapping
    public ResponseEntity<Map<String, Boolean>> addFavorite(@Valid @RequestBody FavoriteToggleRequest request) {
        Long userId = getCurrentUserId();
        boolean added = favoriteIndicatorService.toggle(userId, request.sourceType(), request.indicatorCode());
        return ResponseEntity.ok(Map.of("favorited", added));
    }

    /**
     * 관심 지표 해제
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Boolean>> removeFavorite(
            @RequestParam FavoriteIndicatorSourceType sourceType,
            @RequestParam String indicatorCode) {
        Long userId = getCurrentUserId();
        favoriteIndicatorService.toggle(userId, sourceType, indicatorCode);
        return ResponseEntity.ok(Map.of("favorited", false));
    }

    /**
     * 사용자 관심 지표 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<FavoriteIndicatorResponse>> getFavorites() {
        Long userId = getCurrentUserId();
        List<FavoriteIndicator> favorites = favoriteIndicatorService.findByUserId(userId);
        List<FavoriteIndicatorResponse> response = favorites.stream()
            .map(FavoriteIndicatorResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * 관심 지표 + Latest 데이터 통합 조회 (대시보드용)
     */
    @GetMapping("/enriched")
    public ResponseEntity<EnrichedFavoriteResponse> getEnrichedFavorites() {
        Long userId = getCurrentUserId();
        EnrichedFavorites enriched = favoriteIndicatorService.findEnrichedByUserId(userId);
        return ResponseEntity.ok(EnrichedFavoriteResponse.from(enriched));
    }

    /**
     * 글로벌 관심 지표 단일 타입 실시간 재조회.
     * 본인이 관심 등록한 지표에 한해 허용되며, 60초에 1회 레이트리밋을 적용한다.
     */
    @PostMapping("/global/refresh/{indicatorType}")
    public ResponseEntity<GlobalRefreshResponse> refreshGlobal(@PathVariable String indicatorType) {
        Long userId = getCurrentUserId();
        GlobalEconomicIndicatorType type = GlobalEconomicIndicatorType.valueOf(indicatorType);
        List<EnrichedGlobalFavorite> refreshed = favoriteIndicatorService.refreshGlobalIndicator(userId, type);
        return ResponseEntity.ok(GlobalRefreshResponse.of(type, refreshed));
    }

    /**
     * 관심 지표 표시 모드 변경 (INDICATOR ↔ GRAPH)
     */
    @PutMapping("/display-mode")
    public ResponseEntity<Void> changeDisplayMode(@Valid @RequestBody FavoriteDisplayModeRequest request) {
        Long userId = getCurrentUserId();
        favoriteIndicatorService.changeDisplayMode(
            userId, request.sourceType(), request.indicatorCode(), request.displayMode());
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}