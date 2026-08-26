package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import java.util.List;

public record FavoriteRegionResponse(List<Item> items) {

    public record Item(Long id,
                       String regionCode,
                       String sidoName,
                       String sigunguName,
                       String emdCode,
                       String emdName,
                       int displayOrder,
                       String createdAt) {
    }
}