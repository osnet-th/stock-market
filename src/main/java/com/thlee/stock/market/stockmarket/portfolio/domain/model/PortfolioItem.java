package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Getter
public class PortfolioItem {
    private Long id;
    private Long userId;
    private String itemName;
    private AssetType assetType;
    private BigDecimal investedAmount;
    private boolean newsEnabled;
    private Region region;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private StockDetail stockDetail;
    private BondDetail bondDetail;
    private RealEstateDetail realEstateDetail;
    private FundDetail fundDetail;
    private CashDetail cashDetail;

    /**
     * 재구성용 생성자 (Repository에서 조회 시 사용)
     */
    public PortfolioItem(Long id,
                         Long userId,
                         String itemName,
                         AssetType assetType,
                         BigDecimal investedAmount,
                         boolean newsEnabled,
                         Region region,
                         String memo,
                         LocalDateTime createdAt,
                         LocalDateTime updatedAt,
                         StockDetail stockDetail,
                         BondDetail bondDetail,
                         RealEstateDetail realEstateDetail,
                         FundDetail fundDetail,
                         CashDetail cashDetail) {
        this.id = id;
        this.userId = userId;
        this.itemName = itemName;
        this.assetType = assetType;
        this.investedAmount = investedAmount;
        this.newsEnabled = newsEnabled;
        this.region = region;
        this.memo = memo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.stockDetail = stockDetail;
        this.bondDetail = bondDetail;
        this.realEstateDetail = realEstateDetail;
        this.fundDetail = fundDetail;
        this.cashDetail = cashDetail;
    }

    private PortfolioItem(Long userId,
                          String itemName,
                          AssetType assetType,
                          BigDecimal investedAmount,
                          Region region) {
        validateRequired(userId, itemName, assetType, investedAmount, region);
        this.userId = userId;
        this.itemName = itemName;
        this.assetType = assetType;
        this.investedAmount = investedAmount;
        this.newsEnabled = false;
        this.region = region;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Detail 없는 타입용 (CRYPTO, GOLD, COMMODITY, OTHER)
     */
    public static PortfolioItem create(Long userId,
                                       String itemName,
                                       AssetType assetType,
                                       BigDecimal investedAmount,
                                       Region region) {
        return new PortfolioItem(userId, itemName, assetType, investedAmount, region);
    }

    /**
     * 주식 항목 생성 (investedAmount = quantity × avgBuyPrice 자동 계산)
     */
    public static PortfolioItem createWithStock(Long userId,
                                                String itemName,
                                                Region region,
                                                StockDetail stockDetail) {
        validateDetail(stockDetail, "stockDetail");
        BigDecimal investedAmount = calcInvestedAmount(stockDetail.getAvgBuyPrice(), stockDetail.getQuantity());
        PortfolioItem item = new PortfolioItem(userId, itemName, AssetType.STOCK, investedAmount, region);
        item.stockDetail = stockDetail;
        return item;
    }

    /**
     * 채권 항목 생성
     */
    public static PortfolioItem createWithBond(Long userId,
                                               String itemName,
                                               BigDecimal investedAmount,
                                               Region region,
                                               BondDetail bondDetail) {
        validateDetail(bondDetail, "bondDetail");
        PortfolioItem item = new PortfolioItem(userId, itemName, AssetType.BOND, investedAmount, region);
        item.bondDetail = bondDetail;
        return item;
    }

    /**
     * 부동산 항목 생성
     */
    public static PortfolioItem createWithRealEstate(Long userId,
                                                     String itemName,
                                                     BigDecimal investedAmount,
                                                     Region region,
                                                     RealEstateDetail realEstateDetail) {
        validateDetail(realEstateDetail, "realEstateDetail");
        PortfolioItem item = new PortfolioItem(userId, itemName, AssetType.REAL_ESTATE, investedAmount, region);
        item.realEstateDetail = realEstateDetail;
        return item;
    }

    /**
     * 펀드 항목 생성
     */
    public static PortfolioItem createWithFund(Long userId,
                                               String itemName,
                                               BigDecimal investedAmount,
                                               Region region,
                                               FundDetail fundDetail) {
        validateDetail(fundDetail, "fundDetail");
        PortfolioItem item = new PortfolioItem(userId, itemName, AssetType.FUND, investedAmount, region);
        item.fundDetail = fundDetail;
        return item;
    }

    /**
     * 현금성 자산 항목 생성 (예금/적금/CMA)
     */
    public static PortfolioItem createWithCash(Long userId,
                                               String itemName,
                                               BigDecimal investedAmount,
                                               Region region,
                                               CashDetail cashDetail) {
        validateDetail(cashDetail, "cashDetail");
        PortfolioItem item = new PortfolioItem(userId, itemName, AssetType.CASH, investedAmount, region);
        item.cashDetail = cashDetail;
        return item;
    }

    public void updateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("투자 금액은 0보다 커야 합니다.");
        }
        this.investedAmount = amount;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * CASH 잔액 차감 (잔액 >= 차감액 검증, 결과 0원 허용)
     */
    public void deductAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
        if (this.investedAmount.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    String.format("원화 잔액이 부족합니다. (잔액: %s원, 필요: %s원)",
                            this.investedAmount.toPlainString(), amount.toPlainString()));
        }
        this.investedAmount = this.investedAmount.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * CASH 잔액 복원
     */
    public void restoreAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("복원 금액은 0보다 커야 합니다.");
        }
        this.investedAmount = this.investedAmount.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    public void enableNews() {
        this.newsEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disableNews() {
        this.newsEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateItemName(String itemName) {
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("항목명은 필수입니다.");
        }
        this.itemName = itemName;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateMemo(String memo) {
        this.memo = memo;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 주식 추가 매수 (가중평균 계산)
     * newAvg = (기존수량 × 기존평균단가 + 추가수량 × 추가매수가) / (기존수량 + 추가수량)
     */
    public void addStockPurchase(int additionalQuantity, BigDecimal purchasePrice) {
        if (this.assetType != AssetType.STOCK) {
            throw new IllegalArgumentException("주식 항목이 아닙니다.");
        }
        validateDetail(this.stockDetail, "stockDetail");
        if (additionalQuantity <= 0) {
            throw new IllegalArgumentException("추가 매수 수량은 0보다 커야 합니다.");
        }
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("매수가는 0보다 커야 합니다.");
        }

        int existingQuantity = this.stockDetail.getQuantity();
        BigDecimal existingAvg = this.stockDetail.getAvgBuyPrice();

        BigDecimal totalCost = existingAvg.multiply(BigDecimal.valueOf(existingQuantity))
                .add(purchasePrice.multiply(BigDecimal.valueOf(additionalQuantity)));
        int newQuantity = existingQuantity + additionalQuantity;
        BigDecimal newAvgBuyPrice = totalCost.divide(BigDecimal.valueOf(newQuantity), 2, RoundingMode.HALF_UP);

        this.stockDetail = new StockDetail(
                this.stockDetail.getSubType(),
                this.stockDetail.getStockCode(),
                this.stockDetail.getMarket(),
                this.stockDetail.getExchangeCode(),
                this.stockDetail.getCountry(),
                newQuantity,
                newAvgBuyPrice,
                this.stockDetail.getDividendYield(),
                this.stockDetail.getPriceCurrency(),
                this.stockDetail.getInvestedAmountKrw()
        );
        this.investedAmount = calcInvestedAmount(newAvgBuyPrice, newQuantity);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 매수이력 기반 수량/평균단가/투자금 재계산
     * 이력 수정/삭제 후 호출
     */
    public void recalculateFromPurchaseHistories(List<StockPurchaseHistory> histories) {
        if (this.assetType != AssetType.STOCK) {
            throw new IllegalArgumentException("주식 항목이 아닙니다.");
        }
        validateDetail(this.stockDetail, "stockDetail");
        if (histories.isEmpty()) {
            throw new IllegalArgumentException("매수 이력이 최소 1건 이상 있어야 합니다.");
        }

        int totalQuantity = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (StockPurchaseHistory h : histories) {
            totalQuantity += h.getQuantity();
            totalCost = totalCost.add(h.getTotalCost());
        }

        BigDecimal newAvgBuyPrice = totalCost.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);

        this.stockDetail = new StockDetail(
                this.stockDetail.getSubType(),
                this.stockDetail.getStockCode(),
                this.stockDetail.getMarket(),
                this.stockDetail.getExchangeCode(),
                this.stockDetail.getCountry(),
                totalQuantity,
                newAvgBuyPrice,
                this.stockDetail.getDividendYield(),
                this.stockDetail.getPriceCurrency(),
                this.stockDetail.getInvestedAmountKrw()
        );
        this.investedAmount = calcInvestedAmount(newAvgBuyPrice, totalQuantity);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 납입이력 기반 투자금액 재계산
     * 이력 추가/수정/삭제 후 호출
     */
    public void recalculateFromDepositHistories(List<DepositHistory> histories) {
        if (this.assetType != AssetType.CASH && this.assetType != AssetType.FUND) {
            throw new IllegalArgumentException("현금성 자산 또는 펀드 항목이 아닙니다.");
        }
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (DepositHistory h : histories) {
            totalAmount = totalAmount.add(h.getAmount());
        }
        this.investedAmount = totalAmount;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStockDetail(StockDetail stockDetail) {
        validateDetail(stockDetail, "stockDetail");
        if (this.assetType != AssetType.STOCK) {
            throw new IllegalArgumentException("주식 항목이 아닙니다.");
        }
        this.stockDetail = stockDetail;
        this.investedAmount = calcInvestedAmount(stockDetail.getAvgBuyPrice(), stockDetail.getQuantity());
        this.updatedAt = LocalDateTime.now();
    }

    public void updateBondDetail(BondDetail bondDetail) {
        validateDetail(bondDetail, "bondDetail");
        if (this.assetType != AssetType.BOND) {
            throw new IllegalArgumentException("채권 항목이 아닙니다.");
        }
        this.bondDetail = bondDetail;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRealEstateDetail(RealEstateDetail realEstateDetail) {
        validateDetail(realEstateDetail, "realEstateDetail");
        if (this.assetType != AssetType.REAL_ESTATE) {
            throw new IllegalArgumentException("부동산 항목이 아닙니다.");
        }
        this.realEstateDetail = realEstateDetail;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateFundDetail(FundDetail fundDetail) {
        validateDetail(fundDetail, "fundDetail");
        if (this.assetType != AssetType.FUND) {
            throw new IllegalArgumentException("펀드 항목이 아닙니다.");
        }
        this.fundDetail = fundDetail;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateCashDetail(CashDetail cashDetail) {
        validateDetail(cashDetail, "cashDetail");
        if (this.assetType != AssetType.CASH) {
            throw new IllegalArgumentException("현금성 자산 항목이 아닙니다.");
        }
        this.cashDetail = cashDetail;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateRequired(Long userId,
                                         String itemName,
                                         AssetType assetType,
                                         BigDecimal investedAmount,
                                         Region region) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (itemName == null || itemName.isBlank()) {
            throw new IllegalArgumentException("항목명은 필수입니다.");
        }
        if (assetType == null) {
            throw new IllegalArgumentException("자산 유형은 필수입니다.");
        }
        if (investedAmount == null || investedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("투자 금액은 0보다 커야 합니다.");
        }
        if (region == null) {
            throw new IllegalArgumentException("region은 필수입니다.");
        }
    }

    private static void validateDetail(Object detail, String detailName) {
        if (detail == null) {
            throw new IllegalArgumentException(detailName + "은(는) 필수입니다.");
        }
    }

    private static BigDecimal calcInvestedAmount(BigDecimal avgBuyPrice, int quantity) {
        return avgBuyPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}