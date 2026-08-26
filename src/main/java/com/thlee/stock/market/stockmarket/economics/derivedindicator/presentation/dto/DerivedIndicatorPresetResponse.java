package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.DerivedIndicatorPreset;

/**
 * 프리셋 목록 응답. 사용자가 복제(copy)할 후보.
 */
public record DerivedIndicatorPresetResponse(
        String key,
        String name,
        String unit,
        String description,
        DerivedFormulaResponse formula
) {
    public static DerivedIndicatorPresetResponse from(DerivedIndicatorPreset preset) {
        return new DerivedIndicatorPresetResponse(
                preset.key(),
                preset.name(),
                preset.unit(),
                preset.description(),
                DerivedFormulaResponse.from(preset.formula())
        );
    }
}
