package com.thlee.stock.market.stockmarket.economics.derivedindicator.application.dto;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.UserDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service.DerivedFormulaEvaluator;

/**
 * 파생지표 + 평가 결과(또는 "계산 불가" 사유) 묶음. 목록 조회 응답의 application 단 표현.
 */
public record EvaluatedDerivedIndicator(UserDerivedIndicator indicator,
                                        DerivedFormulaEvaluator.EvaluationResult result) {
}
