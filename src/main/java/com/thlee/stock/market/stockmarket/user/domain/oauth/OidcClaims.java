package com.thlee.stock.market.stockmarket.user.domain.oauth;

public record OidcClaims(
    String issuer,
    String subject,
    String email
) {
}