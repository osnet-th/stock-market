package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;

import java.math.BigDecimal;
import java.util.List;

/**
 * 기록 수정 application command.
 *
 * <p>검증이 생성된 기록은 잠금 상태로 {@link
 * com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteLockedException}
 * 가 던져진다. stockCode / marketType / noteDate / direction 등 기록의 정체성에 해당하는 필드는
 * 수정 불가 (변경 시 새 기록 작성 원칙).
 */
public record UpdateStockNoteCommand(
        Long noteId,
        Long userId,
        String triggerText,
        String interpretationText,
        String riskText,
        boolean preReflected,
        UserJudgment initialJudgment,
        List<TagInput> tags,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal evEbitda,
        VsAverageLevel vsAverage,
        ImpactLevel revenueImpact,
        ImpactLevel profitImpact,
        ImpactLevel cashflowImpact,
        boolean oneTime,
        boolean structural
) { }