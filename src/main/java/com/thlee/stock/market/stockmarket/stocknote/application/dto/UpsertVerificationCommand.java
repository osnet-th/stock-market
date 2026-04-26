package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;

/**
 * 사후 검증 upsert command.
 */
public record UpsertVerificationCommand(
        Long noteId,
        Long userId,
        JudgmentResult judgmentResult,
        String verificationNote
) { }