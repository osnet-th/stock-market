package com.thlee.stock.market.stockmarket.user.application.dto;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
}