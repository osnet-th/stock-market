package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.UserDerivedIndicatorService;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto.*;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 커스텀 파생지표 REST API.
 * <p>
 * userId는 가드형 SecurityContext(JWT principal)에서만 획득 — 요청 파라미터/바디에 userId 없음(IDOR 방지).
 * 전 엔드포인트 인증 필수(미인증 401).
 */
@RestController
@RequestMapping("/api/economics/derived-indicators")
@RequiredArgsConstructor
public class UserDerivedIndicatorController {

    private final UserDerivedIndicatorService service;

    @GetMapping
    public List<DerivedIndicatorResponse> list() {
        Long userId = DerivedIndicatorSecurityContext.currentUserId();
        return service.listWithValues(userId).stream()
                .map(DerivedIndicatorResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DerivedIndicatorResponse create(@Valid @RequestBody DerivedIndicatorCreateRequest request) {
        Long userId = DerivedIndicatorSecurityContext.currentUserId();
        return DerivedIndicatorResponse.of(
                service.create(userId, request.name(), request.unit(), request.description(),
                        request.formula().toDomain()));
    }

    @PutMapping("/{id}")
    public DerivedIndicatorResponse update(@PathVariable Long id,
                                           @Valid @RequestBody DerivedIndicatorUpdateRequest request) {
        Long userId = DerivedIndicatorSecurityContext.currentUserId();
        return DerivedIndicatorResponse.of(
                service.update(userId, id, request.name(), request.unit(), request.description(),
                        request.formula().toDomain()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Long userId = DerivedIndicatorSecurityContext.currentUserId();
        service.delete(userId, id);
    }

    @GetMapping("/available-indicators")
    public List<AvailableIndicatorResponse> availableIndicators(
            @RequestParam(required = false) EcosIndicatorCategory category) {
        DerivedIndicatorSecurityContext.currentUserId();
        return service.availableIndicators(category).stream()
                .map(AvailableIndicatorResponse::from)
                .toList();
    }

    @GetMapping("/presets")
    public List<DerivedIndicatorPresetResponse> presets() {
        DerivedIndicatorSecurityContext.currentUserId();
        return service.presets().stream()
                .map(DerivedIndicatorPresetResponse::from)
                .toList();
    }

    @PostMapping("/presets/{key}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public DerivedIndicatorResponse copyPreset(@PathVariable String key) {
        Long userId = DerivedIndicatorSecurityContext.currentUserId();
        return DerivedIndicatorResponse.of(service.copyPreset(userId, key));
    }
}
