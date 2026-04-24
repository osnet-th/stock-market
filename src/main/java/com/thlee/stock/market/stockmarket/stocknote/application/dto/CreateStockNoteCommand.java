package com.thlee.stock.market.stockmarket.stocknote.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.ImpactLevel;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.UserJudgment;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 기록 생성 application command. presentation Request 에서 매핑된 뒤 WriteService 가 소비한다.
 */
public record CreateStockNoteCommand(
        Long userId,
        String stockCode,
        MarketType marketType,
        ExchangeCode exchangeCode,
        NoteDirection direction,
        BigDecimal changePercent,
        LocalDate noteDate,
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