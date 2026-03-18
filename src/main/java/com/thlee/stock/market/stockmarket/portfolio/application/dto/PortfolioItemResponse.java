package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PortfolioItemResponse {
    private final Long id;
    private final String assetType;
    private final String itemName;
    private final BigDecimal investedAmount;
    private final boolean newsEnabled;
    private final String region;
    private final String memo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final StockDetailResponse stockDetail;
    private final BondDetailResponse bondDetail;
    private final RealEstateDetailResponse realEstateDetail;
    private final FundDetailResponse fundDetail;
    private final Long linkedCashItemId;

    private PortfolioItemResponse(Long id, String assetType, String itemName,
                                  BigDecimal investedAmount, boolean newsEnabled,
                                  String region, String memo,
                                  LocalDateTime createdAt, LocalDateTime updatedAt,
                                  StockDetailResponse stockDetail,
                                  BondDetailResponse bondDetail,
                                  RealEstateDetailResponse realEstateDetail,
                                  FundDetailResponse fundDetail,
                                  Long linkedCashItemId) {
        this.id = id;
        this.assetType = assetType;
        this.itemName = itemName;
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
        this.linkedCashItemId = linkedCashItemId;
    }

    public static PortfolioItemResponse from(PortfolioItem item) {
        return from(item, null);
    }

    public static PortfolioItemResponse from(PortfolioItem item, Long linkedCashItemId) {
        return new PortfolioItemResponse(
                item.getId(),
                item.getAssetType().name(),
                item.getItemName(),
                item.getInvestedAmount(),
                item.isNewsEnabled(),
                item.getRegion().name(),
                item.getMemo(),
                item.getCreatedAt(),
                item.getUpdatedAt(),
                item.getStockDetail() != null ? StockDetailResponse.from(item.getStockDetail()) : null,
                item.getBondDetail() != null ? BondDetailResponse.from(item.getBondDetail()) : null,
                item.getRealEstateDetail() != null ? RealEstateDetailResponse.from(item.getRealEstateDetail()) : null,
                item.getFundDetail() != null ? FundDetailResponse.from(item.getFundDetail()) : null,
                linkedCashItemId
        );
    }
}