package com.thlee.stock.market.stockmarket.realestate.presentation;

import com.thlee.stock.market.stockmarket.realestate.application.RealEstateFavoriteRegionService;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.FavoriteRegionRequest;
import com.thlee.stock.market.stockmarket.realestate.presentation.dto.FavoriteRegionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * 부동산 관심지역 등록/해제/조회 API.
 * <p>
 * userId는 JWT SecurityContext에서 추출 — @RequestParam 금지 (학습: favorite-indicator-dashboard).
 * favorite 도메인 entity는 변경하지 않으며, realestate 도메인 자체 entity를 사용한다.
 */
@RestController
@RequestMapping("/api/realestate/favorites/regions")
@RequiredArgsConstructor
public class RealEstateFavoriteRegionController {

    private final RealEstateFavoriteRegionService favoriteService;

    @GetMapping
    public ResponseEntity<FavoriteRegionResponse> list() {
        return ResponseEntity.ok(favoriteService.list(currentUserId()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> register(@RequestBody FavoriteRegionRequest request) {
        var saved = favoriteService.register(currentUserId(), request.regionCode(), request.emdCode());
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "regionCode", saved.getRegionCode(),
                "emdCode", saved.getEmdCode()));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> unregister(@RequestBody FavoriteRegionRequest request) {
        boolean removed = favoriteService.unregister(currentUserId(), request.regionCode(), request.emdCode());
        return ResponseEntity.ok(Map.of("removed", removed));
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || auth instanceof AnonymousAuthenticationToken
                || !(auth.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return id;
    }
}