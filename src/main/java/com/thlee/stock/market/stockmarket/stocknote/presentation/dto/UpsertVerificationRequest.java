package com.thlee.stock.market.stockmarket.stocknote.presentation.dto;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.UpsertVerificationCommand;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/stock-notes/{id}/verification Request body.
 */
public record UpsertVerificationRequest(
        @NotNull JudgmentResult judgmentResult,
        @Size(max = 4000) String verificationNote
) {
    public UpsertVerificationCommand toCommand(Long noteId, Long userId) {
        return new UpsertVerificationCommand(noteId, userId, judgmentResult, verificationNote);
    }
}