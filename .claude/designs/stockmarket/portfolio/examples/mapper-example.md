# PortfolioItemMapper 예시 코드

## Entity(상속) → Domain(컴포지션) 변환

```java
public class PortfolioItemMapper {

    public static PortfolioItem toDomain(PortfolioItemEntity entity) {
        StockDetail stockDetail = null;
        BondDetail bondDetail = null;
        // ...

        // instanceof 패턴 매칭으로 타입별 Detail 생성
        if (entity instanceof StockItemEntity stock) {
            stockDetail = new StockDetail(
                    stock.getSubType() != null ? StockSubType.valueOf(stock.getSubType()) : null,
                    stock.getTicker(),
                    stock.getExchange(),
                    stock.getQuantity(),
                    stock.getAvgBuyPrice(),
                    stock.getDividendYield()
            );
        } else if (entity instanceof BondItemEntity bond) {
            bondDetail = new BondDetail(
                    bond.getSubType() != null ? BondSubType.valueOf(bond.getSubType()) : null,
                    bond.getMaturityDate(),
                    bond.getCouponRate(),
                    bond.getCreditRating()
            );
        }
        // ... RealEstate, Fund도 동일 패턴

        return new PortfolioItem(
                entity.getId(), entity.getUserId(), entity.getItemName(),
                AssetType.valueOf(entity.getAssetType()),
                entity.getInvestedAmount(), entity.isNewsEnabled(),
                Region.valueOf(entity.getRegion()),
                entity.getMemo(), entity.getCreatedAt(), entity.getUpdatedAt(),
                stockDetail, bondDetail, realEstateDetail, fundDetail
        );
    }
```

## Domain(컴포지션) → Entity(상속) 변환

```java
    public static PortfolioItemEntity toEntity(PortfolioItem item) {
        String region = item.getRegion().name();

        // switch 표현식으로 AssetType별 자식 엔티티 생성
        return switch (item.getAssetType()) {
            case STOCK -> {
                StockDetail detail = item.getStockDetail();
                yield new StockItemEntity(
                        item.getId(), item.getUserId(), item.getItemName(),
                        item.getInvestedAmount(), item.isNewsEnabled(), region,
                        item.getMemo(), item.getCreatedAt(), item.getUpdatedAt(),
                        detail.getSubType() != null ? detail.getSubType().name() : null,
                        detail.getTicker(), detail.getExchange(),
                        detail.getQuantity(), detail.getAvgBuyPrice(), detail.getDividendYield()
                );
            }
            // ... BOND, REAL_ESTATE, FUND도 동일 패턴
            case CRYPTO -> new CryptoItemEntity(
                    item.getId(), item.getUserId(), item.getItemName(),
                    item.getInvestedAmount(), item.isNewsEnabled(), region,
                    item.getMemo(), item.getCreatedAt(), item.getUpdatedAt()
            );
            // ... GOLD, COMMODITY, CASH, OTHER도 동일 패턴
        };
    }
}
```