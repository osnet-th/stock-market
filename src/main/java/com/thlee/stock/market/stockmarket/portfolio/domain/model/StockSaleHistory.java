package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주식 매도 1건의 이력.
 *
 * <p>입력 SoT(quantity, avgBuyPrice, salePrice, currency, fxRate, totalAssetAtSale, ...)와
 * 매도 시점 파생값(profit, profitRate, contributionRate, salePriceKrw, profitKrw)을 함께 보존한다.
 * 파생값은 {@link #recomputeProfit(BigDecimal, BigDecimal)}으로 재계산한다.</p>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class StockSaleHistory {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int RATE_DIVIDE_SCALE = 6;
    private static final int FINAL_SCALE = 2;

    private Long id;
    private Long portfolioItemId;
    private int quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal salePrice;
    private BigDecimal profit;
    private BigDecimal profitRate;
    private BigDecimal contributionRate;
    private BigDecimal totalAssetAtSale;
    private String currency;
    private BigDecimal fxRate;
    private BigDecimal salePriceKrw;
    private BigDecimal profitKrw;
    private SaleReason reason;
    private String memo;
    private String stockCode;
    private String stockName;
    private boolean unrecordedDeposit;
    private LocalDate soldAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 새 매도 이력 생성. 파생값(profit/rate/krw)은 자동 계산된다.
     *
     * @param fxRate 환율(KRW면 1, 외화 환율 조회 실패 시 null 허용 — KRW 환산값은 비움)
     * @param totalAssetAtSale 매도 시점 사용자 전체 자산 KRW 평가금액(자산 기여율 계산 기준)
     */
    public static StockSaleHistory create(Long portfolioItemId,
                                          int quantity,
                                          BigDecimal avgBuyPrice,
                                          BigDecimal salePrice,
                                          String currency,
                                          BigDecimal fxRate,
                                          BigDecimal totalAssetAtSale,
                                          SaleReason reason,
                                          String memo,
                                          String stockCode,
                                          String stockName,
                                          boolean unrecordedDeposit,
                                          LocalDate soldAt,
                                          LocalDate today) {
        validateInputs(quantity, avgBuyPrice, salePrice, currency, reason, soldAt, today);
        StockSaleHistory history = new StockSaleHistory(
                null, portfolioItemId, quantity, avgBuyPrice, salePrice,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                totalAssetAtSale, currency, fxRate, null, null,
                reason, memo, stockCode, stockName, unrecordedDeposit, soldAt,
                LocalDateTime.now(), LocalDateTime.now()
        );
        history.recomputeProfit(totalAssetAtSale, fxRate);
        return history;
    }

    /**
     * 사후 수정 — quantity, salePrice, reason, memo만 변경 가능.
     * 변경 후 {@link #recomputeProfit(BigDecimal, BigDecimal)}로 파생값 재계산.
     */
    public void update(int quantity, BigDecimal salePrice, SaleReason reason, String memo) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("매도 수량은 0보다 커야 합니다.");
        }
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("판매 단가는 0보다 커야 합니다.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("매도 사유는 필수입니다.");
        }
        this.quantity = quantity;
        this.salePrice = salePrice;
        this.reason = reason;
        this.memo = memo;
    }

    /**
     * 입력 SoT(salePrice/quantity/avgBuyPrice + 외화/총자산)를 기반으로 파생값을 다시 계산한다.
     * 외화 환율이 null이면 KRW 환산값(salePriceKrw/profitKrw)은 null로 남고 contributionRate는 0.
     */
    public void recomputeProfit(BigDecimal totalAssetAtSale, BigDecimal fxRate) {
        if (avgBuyPrice == null || avgBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("평균 매수 단가가 0이라 수익률을 계산할 수 없습니다.");
        }
        this.totalAssetAtSale = totalAssetAtSale;
        this.fxRate = fxRate;

        BigDecimal qty = BigDecimal.valueOf(quantity);
        this.profit = salePrice.subtract(avgBuyPrice).multiply(qty).setScale(FINAL_SCALE, RoundingMode.HALF_UP);

        BigDecimal cost = avgBuyPrice.multiply(qty);
        this.profitRate = profit.divide(cost, RATE_DIVIDE_SCALE, RoundingMode.HALF_UP)
                .multiply(HUNDRED).setScale(FINAL_SCALE, RoundingMode.HALF_UP);

        if (fxRate != null) {
            this.salePriceKrw = salePrice.multiply(fxRate).multiply(qty).setScale(FINAL_SCALE, RoundingMode.HALF_UP);
            this.profitKrw = profit.multiply(fxRate).setScale(FINAL_SCALE, RoundingMode.HALF_UP);
        } else {
            this.salePriceKrw = null;
            this.profitKrw = null;
        }

        if (totalAssetAtSale == null || totalAssetAtSale.compareTo(BigDecimal.ZERO) == 0) {
            this.contributionRate = BigDecimal.ZERO.setScale(FINAL_SCALE, RoundingMode.HALF_UP);
        } else {
            BigDecimal numerator = profitKrw != null ? profitKrw : profit;
            this.contributionRate = numerator.divide(totalAssetAtSale, RATE_DIVIDE_SCALE, RoundingMode.HALF_UP)
                    .multiply(HUNDRED).setScale(FINAL_SCALE, RoundingMode.HALF_UP);
        }

        this.updatedAt = LocalDateTime.now();
    }

    private static void validateInputs(int quantity,
                                       BigDecimal avgBuyPrice,
                                       BigDecimal salePrice,
                                       String currency,
                                       SaleReason reason,
                                       LocalDate soldAt,
                                       LocalDate today) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("매도 수량은 0보다 커야 합니다.");
        }
        if (avgBuyPrice == null || avgBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("평균 매수 단가는 0보다 커야 합니다.");
        }
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("판매 단가는 0보다 커야 합니다.");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("통화는 필수입니다.");
        }
        if (reason == null) {
            throw new IllegalArgumentException("매도 사유는 필수입니다.");
        }
        if (soldAt == null) {
            throw new IllegalArgumentException("매도일은 필수입니다.");
        }
        if (today != null && soldAt.isAfter(today)) {
            throw new IllegalArgumentException("매도일은 미래일 수 없습니다.");
        }
    }
}