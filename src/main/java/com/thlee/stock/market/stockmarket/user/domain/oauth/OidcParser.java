package com.thlee.stock.market.stockmarket.user.domain.oauth;

public interface OidcParser {
    OidcClaims parseIdToken(String idToken);
}