package com.thlee.stock.market.stockmarket.economics.derivedindicator.application;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service.DerivedFormulaValidator;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DerivedIndicatorPresetProviderTest {

    private final DerivedIndicatorPresetProvider provider = new DerivedIndicatorPresetProvider();
    private final DerivedFormulaValidator validator = new DerivedFormulaValidator();

    @Test
    void 모든_프리셋은_실제_메타_기준_검증을_통과한다() {
        Map<String, EcosIndicatorCategory> meta = buildMetaFromPresets();

        for (DerivedIndicatorPreset preset : provider.all()) {
            DerivedFormulaValidator.ValidationResult result =
                    validator.validate(preset.formula(), meta, true);

            assertThat(result.valid())
                    .as("프리셋 '%s'(%s)가 검증을 통과해야 함, 사유=%s", preset.name(), preset.key(), result.reason())
                    .isTrue();
        }
    }

    @Test
    void 프리셋은_빈화면_방지를_위한_최소_개수를_보장한다() {
        assertThat(provider.all()).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void presetKey_화이트리스트_조회() {
        assertThat(provider.findByKey("interest-term-spread")).isPresent();
        assertThat(provider.findByKey("unknown-key")).isEmpty();
    }

    @Test
    void 모든_프리셋_키는_고유하다() {
        List<String> keys = provider.all().stream().map(DerivedIndicatorPreset::key).toList();
        assertThat(keys).doesNotHaveDuplicates();
    }

    /** 각 프리셋 operand의 className을 실제 카테고리로 매핑해 검증용 메타 맵 구성. */
    private Map<String, EcosIndicatorCategory> buildMetaFromPresets() {
        Map<String, EcosIndicatorCategory> meta = new HashMap<>();
        for (DerivedIndicatorPreset preset : provider.all()) {
            for (FormulaOperand operand : preset.formula().indicatorOperands()) {
                EcosIndicatorCategory category = EcosIndicatorCategory.fromClassName(operand.getClassName());
                assertThat(category)
                        .as("프리셋 operand className '%s'은 실제 카테고리에 매핑돼야 함", operand.getClassName())
                        .isNotNull();
                meta.put(operand.compareKey(), category);
            }
        }
        return meta;
    }
}
