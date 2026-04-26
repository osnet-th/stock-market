package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao;

import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.dto.KakaoTokenResponse;
import com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception.KakaoTokenIssueFailed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Slf4j
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
                    String responseBody = readResponseBody(response);
                    log.error("카카오 토큰 발급 실패: status={}, redirectUri={}, body={}",
                            response.getStatusCode(), properties.getRedirectUri(), responseBody);
                    throw new KakaoTokenIssueFailed(
                        "카카오 토큰 발급 실패: " + response.getStatusCode() + " body=" + responseBody
                    );
                })
                .body(KakaoTokenResponse.class);
        } catch (KakaoTokenIssueFailed e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 토큰 발급 중 예외 발생: redirectUri={}, message={}",
                    properties.getRedirectUri(), e.getMessage(), e);
            throw new KakaoTokenIssueFailed("카카오 토큰 발급 중 오류 발생", e);
        }
    }

    private static String readResponseBody(org.springframework.http.client.ClientHttpResponse response) {
        try {
            return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "<응답 바디 읽기 실패: " + e.getMessage() + ">";
        }
    }
}