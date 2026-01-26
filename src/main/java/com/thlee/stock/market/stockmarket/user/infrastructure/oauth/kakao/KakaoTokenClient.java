package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao;

import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoTokenResponse;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception.KakaoTokenIssueFailed;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KakaoTokenClient {
    private final KakaoClient kakaoClient;
    private final KakaoOAuthProperties properties;

    public KakaoTokenResponse issueToken(String code) {
        RestClient restClient = kakaoClient.restClient();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());
        body.add("redirect_uri", properties.getRedirectUri());
        body.add("code", code);

        try {
            return restClient.post()
                .uri(properties.getTokenUri())
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
}