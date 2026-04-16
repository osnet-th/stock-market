package com.thlee.stock.market.stockmarket.favorite.presentation;

import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedFavorites;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.EnrichedFavoriteResponse;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.FavoriteIndicatorResponse;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.FavoriteToggleRequest;
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

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}