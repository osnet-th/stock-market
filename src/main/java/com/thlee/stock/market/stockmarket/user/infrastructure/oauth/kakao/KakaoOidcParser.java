package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thlee.stock.market.stockmarket.user.domain.oauth.OidcClaims;
import com.thlee.stock.market.stockmarket.user.domain.oauth.OidcParser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOidcParser implements OidcParser {
    private final KakaoJwksProvider jwksProvider;
    private final KakaoOAuthProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public OidcClaims parseIdToken(String idToken) {
        validateOidcConfig();
        String kid = extractKid(idToken);
        RSAPublicKey publicKey = jwksProvider.getPublicKey(kid);
        JwtParser parser = Jwts.parser()
            .verifyWith(publicKey)
            .requireIssuer(properties.getIssuer())
            .requireAudience(properties.getAudience())
            .build();

        Jws<Claims> jws = parser.parseSignedClaims(idToken);
        Claims claims = jws.getPayload();
        return new OidcClaims(
            claims.getIssuer(),
            claims.getSubject(),
            claims.get("email", String.class)
        );
    }

    private String extractKid(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("유효하지 않은 id_token 형식입니다.");
            }
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            Map<String, Object> header = objectMapper.readValue(headerJson, new TypeReference<>() {});
            Object kid = header.get("kid");
            if (kid == null) {
                throw new IllegalArgumentException("id_token 헤더에 kid가 없습니다.");
            }
            return kid.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("id_token 헤더 파싱 실패", e);
        }
    }

    private void validateOidcConfig() {
        if (properties.getIssuer() == null || properties.getIssuer().isBlank()) {
            throw new IllegalStateException("kakao.oauth.issuer 설정이 필요합니다.");
        }
        if (properties.getAudience() == null || properties.getAudience().isBlank()) {
            throw new IllegalStateException("kakao.oauth.audience 설정이 필요합니다.");
        }
    }
}