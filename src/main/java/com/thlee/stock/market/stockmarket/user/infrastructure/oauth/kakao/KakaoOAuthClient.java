package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao;

import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoTokenResponse;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoUserResponse;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception.KakaoTokenIssueFailed;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception.KakaoUserInfoFetchFailed;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthClient {
    private final RestClient restClient;
    private final KakaoOAuthProperties properties;

    public KakaoOAuthClient(RestClient.Builder builder, KakaoOAuthProperties properties) {
        this.restClient = builder.build();
        this.properties = properties;
    }

    public KakaoTokenResponse issueToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("redirect_uri", properties.redirectUri());
        body.add("code", code);

        try {
            return restClient.post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new KakaoTokenIssueFailed(
                        "카카오 토큰 발급 실패: " + response.getStatusCode()
                    );
                })
                .body(KakaoTokenResponse.class);
        } catch (Exception e) {
            throw new KakaoTokenIssueFailed("카카오 토큰 발급 중 오류 발생", e);
        }
    }

    public KakaoUserResponse getUserInfo(String accessToken) {
        try {
            return restClient.post()
                .uri(properties.userInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new KakaoUserInfoFetchFailed(
                        "카카오 사용자 정보 조회 실패: " + response.getStatusCode()
                    );
                })
                .body(KakaoUserResponse.class);
        } catch (Exception e) {
            throw new KakaoUserInfoFetchFailed("카카오 사용자 정보 조회 중 오류 발생", e);
        }
    }
}