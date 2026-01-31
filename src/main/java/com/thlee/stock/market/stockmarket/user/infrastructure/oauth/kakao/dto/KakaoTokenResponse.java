package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponse(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("refresh_token")
    String refreshToken,
    @JsonProperty("id_token")
    String openId,

    @JsonProperty("expires_in")
    Long expiresIn,

    @JsonProperty("refresh_token_expires_in")
    Long refreshTokenExpiresIn
) {
}