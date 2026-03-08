package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.*;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.BondSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.FundSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.RealEstateSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence.*;

/**
 * PortfolioItem Entity(상속) ↔ Domain Model(컴포지션) 변환 Mapper
 */
public class PortfolioItemMapper {

    /**
     * Entity → Domain 변환
     */
    public static PortfolioItem toDomain(PortfolioItemEntity entity) {
        StockDetail stockDetail = null;
        BondDetail bondDetail = null;
        RealEstateDetail realEstateDetail = null;
        FundDetail fundDetail = null;

        if (entity instanceof StockItemEntity stock) {
            stockDetail = new StockDetail(
                    stock.getSubType() != null ? StockSubType.valueOf(stock.getSubType()) : null,
                    stock.getStockCode(),
                    stock.getMarket(),
                    stock.getCountry(),
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
        } else if (entity instanceof RealEstateItemEntity realEstate) {
            realEstateDetail = new RealEstateDetail(
                    realEstate.getSubType() != null ? RealEstateSubType.valueOf(realEstate.getSubType()) : null,
                    realEstate.getAddress(),
                    realEstate.getArea()
            );
        } else if (entity instanceof FundItemEntity fund) {
            fundDetail = new FundDetail(
                    fund.getSubType() != null ? FundSubType.valueOf(fund.getSubType()) : null,
                    fund.getManagementFee()
            );
        }

        return new PortfolioItem(
                entity.getId(),
                entity.getUserId(),
                entity.getItemName(),
                resolveAssetType(entity),
                entity.getInvestedAmount(),
                entity.isNewsEnabled(),
                Region.valueOf(entity.getRegion()),
                entity.getMemo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                stockDetail,
                bondDetail,
                realEstateDetail,
                fundDetail
        );
    }

    /**
     * Domain → Entity 변환
     */
    public static PortfolioItemEntity toEntity(PortfolioItem item) {
        String region = item.getRegion().name();

        return switch (item.getAssetType()) {
            case STOCK -> {
                StockDetail detail = item.getStockDetail();
                yield new StockItemEntity(
                        item.getId(), item.getUserId(), item.getItemName(),
                        item.getInvestedAmount(), item.isNewsEnabled(), region,
                        item.getMemo(), item.getCreatedAt(), item.getUpdatedAt(),
                        detail.getSubType() != null ? detail.getSubType().name() : null,
                        detail.getStockCode(), detail.getMarket(), detail.getCountry(),
                        detail.getQuantity(), detail.getAvgBuyPrice(), detail.getDividendYield()
                );
            }
            case BOND -> {
                BondDetail detail = item.getBondDetail();
                yield new BondItemEntity(
                        item.getId(), item.getUserId(), item.getItemName(),
                        item.getInvestedAmount(), item.isNewsEnabled(), region,
                        item.getMemo(), item.getCreatedAt(), item.getUpdatedAt(),
                        detail.getSubType() != null ? detail.getSubType().name() : null,
                        detail.getMaturityDate(), detail.getCouponRate(), detail.getCreditRating()
                );
            }
            case REAL_ESTATE -> {
                RealEstateDetail detail = item.getRealEstateDetail();
                yield new RealEstateItemEntity(
                        item.getId(), item.getUserId(), item.getItemName(),
                        item.getInvestedAmount(), item.isNewsEnabled(), region,
                        item.getMemo(), item.getCreatedAt(), item.getUpdatedAt(),
                        detail.getSubType() != null ? detail.getSubType().name() : null,
                        detail.getAddress(), detail.getArea()
                );
            }
            case FUND -> {
                FundDetail detail = item.getFundDetail();
                yield new FundItemEntity(
                        item.getId(), item.getUserId(), item.getItemName(),
                        item.getInvestedAmount(), item.isNewsEnabled(), region,
                        item.getMemo(), item.getCreatedAt(), item.getUpdatedAt(),
                        detail.getSubType() != null ? detail.getSubType().name() : null,
                        detail.getManagementFee()
                );
            }
            case CRYPTO -> new CryptoItemEntity(
                    item.getId(), item.getUserId(), item.getItemName(),
                    item.getInvestedAmount(), item.isNewsEnabled(), region,
                    item.getMemo(), item.getCreatedAt(), item.getUpdatedAt()
            );
            case GOLD -> new GoldItemEntity(
                    item.getId(), item.getUserId(), item.getItemName(),
                    item.getInvestedAmount(), item.isNewsEnabled(), region,
                    item.getMemo(), item.getCreatedAt(), item.getUpdatedAt()
            );
            case COMMODITY -> new CommodityItemEntity(
                    item.getId(), item.getUserId(), item.getItemName(),
                    item.getInvestedAmount(), item.isNewsEnabled(), region,
                    item.getMemo(), item.getCreatedAt(), item.getUpdatedAt()
            );
            case CASH -> new CashItemEntity(
                    item.getId(), item.getUserId(), item.getItemName(),
                    item.getInvestedAmount(), item.isNewsEnabled(), region,
                    item.getMemo(), item.getCreatedAt(), item.getUpdatedAt()
            );
            case OTHER -> new OtherItemEntity(
                    item.getId(), item.getUserId(), item.getItemName(),
                    item.getInvestedAmount(), item.isNewsEnabled(), region,
                    item.getMemo(), item.getCreatedAt(), item.getUpdatedAt()
            );
        };
    }

    private static AssetType resolveAssetType(PortfolioItemEntity entity) {
        if (entity instanceof StockItemEntity) return AssetType.STOCK;
        if (entity instanceof BondItemEntity) return AssetType.BOND;
        if (entity instanceof RealEstateItemEntity) return AssetType.REAL_ESTATE;
        if (entity instanceof FundItemEntity) return AssetType.FUND;
        if (entity instanceof CryptoItemEntity) return AssetType.CRYPTO;
        if (entity instanceof GoldItemEntity) return AssetType.GOLD;
        if (entity instanceof CommodityItemEntity) return AssetType.COMMODITY;
        if (entity instanceof CashItemEntity) return AssetType.CASH;
        if (entity instanceof OtherItemEntity) return AssetType.OTHER;
        throw new IllegalArgumentException("Unknown entity type: " + entity.getClass().getSimpleName());
    }
}
