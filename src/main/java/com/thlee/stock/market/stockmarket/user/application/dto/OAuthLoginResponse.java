package com.thlee.stock.market.stockmarket.user.application.dto;

import com.thlee.stock.market.stockmarket.user.domain.model.UserRole;

public record OAuthLoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        UserRole role
) {
}