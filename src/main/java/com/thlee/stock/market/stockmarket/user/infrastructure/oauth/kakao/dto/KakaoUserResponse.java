package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserResponse(
    @JsonProperty("id")
    Long id,

    @JsonProperty("kakao_account")
    KakaoAccount kakaoAccount
) {
    public String getEmail() {
        return kakaoAccount != null ? kakaoAccount.email() : null;
    }

    public String getNickname() {
        return kakaoAccount != null && kakaoAccount.profile() != null
            ? kakaoAccount.profile().nickname()
            : null;
    }

    public record KakaoAccount(
        @JsonProperty("email")
        String email,

        @JsonProperty("profile")
        Profile profile
    ) {
    }

    public record Profile(
        @JsonProperty("nickname")
        String nickname
    ) {
    }
}