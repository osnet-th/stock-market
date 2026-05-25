package com.thlee.stock.market.stockmarket.economics.derivedindicator.application;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorMetadataService;
import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.dto.AvailableIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.dto.EvaluatedDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.exception.*;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.UserDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.repository.UserDerivedIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service.DerivedFormulaEvaluator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service.DerivedFormulaValidator;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorValueParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 커스텀 파생지표 application 서비스.
 * <p>
 * 검증 화이트리스트는 EcosIndicatorMetadataService.getMetadataMap()(캐시 독립 고정 메타) 기반.
 * 평가용 최신값은 EcosIndicatorService.findAllLatest()를 compareKey→Double 맵으로 변환해 주입.
 * 순수 도메인 서비스(Validator/Evaluator)는 무상태이므로 new로 보유한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserDerivedIndicatorService {

    static final int MAX_PER_USER = 50;

    private final UserDerivedIndicatorRepository repository;
    private final EcosIndicatorMetadataService metadataService;
    private final EcosIndicatorService ecosIndicatorService;
    private final DerivedIndicatorPresetProvider presetProvider;

    private final DerivedFormulaValidator validator = new DerivedFormulaValidator();
    private final DerivedFormulaEvaluator evaluator = new DerivedFormulaEvaluator();

    public UserDerivedIndicator create(Long userId, String name, String unit, DerivedFormula formula) {
        checkCreatable(userId, name);
        EcosIndicatorCategory category = validateFormula(formula, false);
        return repository.save(UserDerivedIndicator.create(userId, name, unit, formula, category));
    }

    @Transactional(readOnly = true)
    public List<EvaluatedDerivedIndicator> listWithValues(Long userId) {
        Map<String, Double> latestValues = buildLatestValueMap();
        return repository.findByUserId(userId).stream()
                .map(indicator -> new EvaluatedDerivedIndicator(
                        indicator, evaluator.evaluate(indicator.getFormula(), latestValues)))
                .toList();
    }

    public UserDerivedIndicator update(Long userId, Long id, String name, String unit, DerivedFormula formula) {
        UserDerivedIndicator existing = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new DerivedIndicatorNotFoundException(id));
        if (!existing.getName().equals(name) && repository.existsByUserIdAndName(userId, name)) {
            throw new DuplicateDerivedIndicatorNameException(name);
        }
        EcosIndicatorCategory category = validateFormula(formula, false);
        return repository.save(existing.update(name, unit, formula, category));
    }

    public void delete(Long userId, Long id) {
        if (repository.deleteByIdAndUserId(id, userId) == 0) {
            throw new DerivedIndicatorNotFoundException(id);
        }
    }

    public UserDerivedIndicator copyPreset(Long userId, String presetKey) {
        DerivedIndicatorPreset preset = presetProvider.findByKey(presetKey)
                .orElseThrow(() -> new UnknownPresetException(presetKey));
        checkLimit(userId);
        EcosIndicatorCategory category = validateFormula(preset.formula(), true); // 프리셋 R3 예외
        String name = resolveUniqueName(userId, preset.name());
        return repository.save(UserDerivedIndicator.create(userId, name, preset.unit(), preset.formula(), category));
    }

    @Transactional(readOnly = true)
    public List<DerivedIndicatorPreset> presets() {
        return presetProvider.all();
    }

    @Transactional(readOnly = true)
    public List<AvailableIndicator> availableIndicators(EcosIndicatorCategory categoryFilter) {
        return metadataService.getMetadataMap().values().stream()
                .map(this::toAvailableIndicator)
                .filter(indicator -> indicator.category() != null)
                .filter(indicator -> categoryFilter == null || indicator.category() == categoryFilter)
                .toList();
    }

    private AvailableIndicator toAvailableIndicator(EcosIndicatorMetadata metadata) {
        EcosIndicatorCategory category = EcosIndicatorCategory.fromClassName(metadata.getClassName());
        return new AvailableIndicator(metadata.getClassName(), metadata.getKeystatName(), category);
    }

    private void checkCreatable(Long userId, String name) {
        checkLimit(userId);
        if (repository.existsByUserIdAndName(userId, name)) {
            throw new DuplicateDerivedIndicatorNameException(name);
        }
    }

    private void checkLimit(Long userId) {
        if (repository.countByUserId(userId) >= MAX_PER_USER) {
            throw new DerivedIndicatorLimitExceededException(MAX_PER_USER);
        }
    }

    private EcosIndicatorCategory validateFormula(DerivedFormula formula, boolean allowCrossCategory) {
        DerivedFormulaValidator.ValidationResult result =
                validator.validate(formula, buildAvailableMeta(), allowCrossCategory);
        if (!result.valid()) {
            throw new InvalidDerivedFormulaException(result.reason());
        }
        return result.resolvedCategory();
    }

    private Map<String, EcosIndicatorCategory> buildAvailableMeta() {
        Map<String, EcosIndicatorCategory> result = new HashMap<>();
        for (Map.Entry<String, EcosIndicatorMetadata> entry : metadataService.getMetadataMap().entrySet()) {
            EcosIndicatorCategory category = EcosIndicatorCategory.fromClassName(entry.getValue().getClassName());
            if (category != null) {
                result.put(entry.getKey(), category);
            }
        }
        return result;
    }

    private Map<String, Double> buildLatestValueMap() {
        Map<String, Double> result = new HashMap<>();
        for (EcosIndicatorLatest latest : ecosIndicatorService.findAllLatest()) {
            Double value = IndicatorValueParser.parse(latest.getDataValue());
            if (value != null) {
                result.put(latest.toCompareKey(), value);
            }
        }
        return result;
    }

    private String resolveUniqueName(Long userId, String base) {
        String candidate = base;
        int suffix = 2;
        while (repository.existsByUserIdAndName(userId, candidate)) {
            candidate = base + " (" + suffix++ + ")";
        }
        return candidate;
    }
}
