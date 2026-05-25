package com.thlee.stock.market.stockmarket.economics.derivedindicator.application;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 시스템 제공 파생지표 프리셋 카탈로그(코드 정의). 별도 테이블 없음.
 * <p>
 * 카탈로그 상수는 Unit 5에서 기존 EcosDerivedIndicatorService 계산식을 실제 메타데이터 기준으로
 * 수동 전사해 채운다(className/keystatName 정확성 확보, R3 예외, 최소 N개 보장, 4항 이상 제외).
 */
@Component
public class DerivedIndicatorPresetProvider {

    private final List<DerivedIndicatorPreset> presets = buildPresets();

    public List<DerivedIndicatorPreset> all() {
        return presets;
    }

    public Optional<DerivedIndicatorPreset> findByKey(String key) {
        return presets.stream().filter(p -> p.key().equals(key)).findFirst();
    }

    private static List<DerivedIndicatorPreset> buildPresets() {
        // Unit 5에서 실제 메타데이터 기준 전사로 채운다.
        return List.of();
    }
}
