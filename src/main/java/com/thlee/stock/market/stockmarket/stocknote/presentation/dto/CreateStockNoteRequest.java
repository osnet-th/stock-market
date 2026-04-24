package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.CreateStockNoteCommand;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.TagInput;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 기록 생성 Request.
 */
public record CreateStockNoteRequest(
        @NotBlank @Size(max = 20) String stockCode,
        @NotNull MarketType marketType,
        @NotNull ExchangeCode exchangeCode,
        @NotNull NoteDirection direction,
        BigDecimal changePercent,
        @NotNull LocalDate noteDate,
        @Size(max = 4000) String triggerText,
        @Size(max = 4000) String interpretationText,
        @Size(max = 4000) String riskText,
        boolean preReflected,
        @NotNull UserJudgment initialJudgment,
        @Valid List<TagPayload> tags,
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

    public CreateStockNoteCommand toCommand(Long userId) {
        List<TagInput> tagInputs = tags == null ? List.of() : tags.stream()
                .map(t -> new TagInput(t.source(), t.value()))
                .toList();
        return new CreateStockNoteCommand(
                userId, stockCode, marketType, exchangeCode, direction, changePercent, noteDate,
                triggerText, interpretationText, riskText,
                preReflected, initialJudgment,
                tagInputs,
                per, pbr, evEbitda, vsAverage,
                revenueImpact, profitImpact, cashflowImpact,
                oneTime, structural
        );
    }
}