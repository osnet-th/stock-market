# 도메인 모델 예시 코드

## PortfolioItem (컴포지션 방식)

```java
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

    // nullable Detail 필드 (해당 AssetType일 때만 값 존재)
    private StockDetail stockDetail;
    private BondDetail bondDetail;
    private RealEstateDetail realEstateDetail;
    private FundDetail fundDetail;

    // 재구성용 생성자 (Repository 조회 시)
    public PortfolioItem(Long id, Long userId, ..., StockDetail stockDetail, ...) { ... }

    // 팩토리 메서드 - Detail 없는 타입용
    public static PortfolioItem create(Long userId, String itemName, AssetType assetType,
                                       BigDecimal investedAmount, Region region) {
        return new PortfolioItem(userId, itemName, assetType, investedAmount, region);
    }

    // 팩토리 메서드 - 주식
    public static PortfolioItem createWithStock(Long userId, String itemName,
                                                BigDecimal investedAmount, Region region,
                                                StockDetail stockDetail) {
        validateDetail(stockDetail, "stockDetail");
        PortfolioItem item = new PortfolioItem(userId, itemName, AssetType.STOCK, investedAmount, region);
        item.stockDetail = stockDetail;
        return item;
    }

    // 행위 메서드
    public void updateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("투자 금액은 0보다 커야 합니다.");
        }
        this.investedAmount = amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void enableNews() { this.newsEnabled = true; this.updatedAt = LocalDateTime.now(); }
    public void disableNews() { this.newsEnabled = false; this.updatedAt = LocalDateTime.now(); }
    public void updateMemo(String memo) { this.memo = memo; this.updatedAt = LocalDateTime.now(); }
}
```

## Detail 값 객체

```java
@Getter
public class StockDetail {
    private final StockSubType subType;
    private final String ticker;
    private final String exchange;
    private final Integer quantity;
    private final BigDecimal avgBuyPrice;
    private final BigDecimal dividendYield;

    public StockDetail(StockSubType subType, String ticker, String exchange,
                       Integer quantity, BigDecimal avgBuyPrice, BigDecimal dividendYield) {
        this.subType = subType;
        // ...
    }
}
```