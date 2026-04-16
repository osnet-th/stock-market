package com.thlee.stock.market.stockmarket.favorite.presentation.dto;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FavoriteToggleRequest(
    @NotNull FavoriteIndicatorSourceType sourceType,
    @NotBlank String indicatorCode
) {}