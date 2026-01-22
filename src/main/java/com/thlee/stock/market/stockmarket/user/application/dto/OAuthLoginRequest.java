package com.thlee.stock.market.stockmarket.user.application.dto;

import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;

public record OAuthLoginRequest(
        OAuthProvider provider,
        String issuer,
        String subject,
        String email
) {
}