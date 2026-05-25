package com.thlee.stock.market.stockmarket.economics.derivedindicator.application;

import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorMetadataService;
import com.thlee.stock.market.stockmarket.economics.application.EcosIndicatorService;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.exception.*;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.UserDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.repository.UserDerivedIndicatorRepository;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDerivedIndicatorServiceTest {

    @Mock UserDerivedIndicatorRepository repository;
    @Mock EcosIndicatorMetadataService metadataService;
    @Mock EcosIndicatorService ecosIndicatorService;
    @Mock DerivedIndicatorPresetProvider presetProvider;

    @InjectMocks UserDerivedIndicatorService service;

    private static final Long USER = 1L;

    @BeforeEach
    void stubMeta() {
        lenient().when(metadataService.getMetadataMap()).thenReturn(Map.of(
                "시장금리::A", new EcosIndicatorMetadata("시장금리", "A", "d"),
                "시장금리::B", new EcosIndicatorMetadata("시장금리", "B", "d")
        ));
    }

    private DerivedFormula validFormula() {
        return new DerivedFormula(
                List.of(FormulaOperand.indicator("시장금리", "A"), FormulaOperand.indicator("시장금리", "B")),
                List.of(FormulaOperator.SUB));
    }

    @Test
    void create_검증통과시_저장() {
        when(repository.countByUserId(USER)).thenReturn(0L);
        when(repository.existsByUserIdAndName(USER, "내지표")).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DerivedFormula formula = validFormula();
        UserDerivedIndicator saved = service.create(USER, "내지표", "%p", "설명", formula);

        assertThat(saved.getName()).isEqualTo("내지표");
        assertThat(saved.getUserId()).isEqualTo(USER);
        assertThat(saved.getFormula()).isSameAs(formula);
        assertThat(saved.getCategory()).isEqualTo(EcosIndicatorCategory.INTEREST_RATE);
        verify(repository).save(any());
    }

    @Test
    void update_잘못된수식_거부() {
        UserDerivedIndicator existing =
                UserDerivedIndicator.create(USER, "기존", "%p", null, validFormula(), EcosIndicatorCategory.INTEREST_RATE);
        when(repository.findByIdAndUserId(7L, USER)).thenReturn(Optional.of(existing));
        DerivedFormula crossOrUnknown = new DerivedFormula(
                List.of(FormulaOperand.indicator("시장금리", "A"), FormulaOperand.indicator("없음", "Z")),
                List.of(FormulaOperator.SUB));

        assertThatThrownBy(() -> service.update(USER, 7L, "기존", "%p", null, crossOrUnknown))
                .isInstanceOf(InvalidDerivedFormulaException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_잘못된수식_저장안됨() {
        when(repository.countByUserId(USER)).thenReturn(0L);
        DerivedFormula crossOrUnknown = new DerivedFormula(
                List.of(FormulaOperand.indicator("시장금리", "A"), FormulaOperand.indicator("없음", "Z")),
                List.of(FormulaOperator.SUB));

        assertThatThrownBy(() -> service.create(USER, "x", "%", null, crossOrUnknown))
                .isInstanceOf(InvalidDerivedFormulaException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_상한초과_거부() {
        when(repository.countByUserId(USER)).thenReturn(50L);
        assertThatThrownBy(() -> service.create(USER, "x", "%", null, validFormula()))
                .isInstanceOf(DerivedIndicatorLimitExceededException.class);
    }

    @Test
    void update_타사용자_리소스_404() {
        when(repository.findByIdAndUserId(99L, USER)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(USER, 99L, "x", "%", null, validFormula()))
                .isInstanceOf(DerivedIndicatorNotFoundException.class);
    }

    @Test
    void delete_미소유_404() {
        when(repository.deleteByIdAndUserId(99L, USER)).thenReturn(0);
        assertThatThrownBy(() -> service.delete(USER, 99L))
                .isInstanceOf(DerivedIndicatorNotFoundException.class);
    }

    @Test
    void copyPreset_알수없는키_404() {
        when(presetProvider.findByKey("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.copyPreset(USER, "nope"))
                .isInstanceOf(UnknownPresetException.class);
    }
}
