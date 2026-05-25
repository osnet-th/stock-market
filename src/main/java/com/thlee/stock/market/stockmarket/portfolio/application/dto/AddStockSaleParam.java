package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 주식 매도 등록 파라미터.
 * presentation 계층의 Bean Validation 통과 후 application 계층에 전달된다.
 *
 * @param fxRate 사용자 입력 환율 (외화 종목, null이면 자동 조회)
 * @param deductionAmountKrw 제세금·수수료·기타 비용 합계(KRW)
 * @param netProceedsKrw 최종 실입금액(KRW), 입력 시 정산 SoT
 * @param depositCashItemId 입금할 CASH 항목 (CashStockLink 미연결 시 사용자 선택)
 */
public record AddStockSaleParam(
        int quantity,
        BigDecimal salePrice,
        LocalDate soldAt,
        SaleReason reason,
        String memo,
        BigDecimal fxRate,
        BigDecimal deductionAmountKrw,
        BigDecimal netProceedsKrw,
        Long depositCashItemId
) {
}
