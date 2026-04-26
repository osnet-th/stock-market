package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.TagInput;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.UpdateStockNoteCommand;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 기록 수정 Request. 정체성 필드(stockCode/direction/noteDate 등) 는 수정 불가.
 */
public record UpdateStockNoteRequest(
        @Size(max = 4000) String triggerText,
        @Size(max = 4000) String interpretationText,
        @Size(max = 4000) String riskText,
        boolean preReflected,
        @NotNull UserJudgment initialJudgment,
        @Valid @Size(max = 50, message = "tags 는 최대 50개까지 허용됩니다.") List<TagPayload> tags,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal evEbitda,
        VsAverageLevel vsAverage,
        ImpactLevel revenueImpact,
        ImpactLevel profitImpact,
        ImpactLevel cashflowImpact,
        boolean oneTime,
        boolean structural
) {

    public UpdateStockNoteCommand toCommand(Long noteId, Long userId) {
        List<TagInput> tagInputs = tags == null ? List.of() : tags.stream()
                .map(t -> new TagInput(t.source(), t.value()))
                .toList();
        return new UpdateStockNoteCommand(
                noteId, userId,
                triggerText, interpretationText, riskText,
                preReflected, initialJudgment,
                tagInputs,
                per, pbr, evEbitda, vsAverage,
                revenueImpact, profitImpact, cashflowImpact,
                oneTime, structural
        );
    }
}