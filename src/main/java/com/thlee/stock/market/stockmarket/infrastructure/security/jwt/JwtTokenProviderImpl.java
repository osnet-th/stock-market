package com.thlee.stock.market.stockmarket.infrastructure.security.jwt;

import com.thlee.stock.market.stockmarket.infrastructure.security.jwt.exception.ExpiredTokenException;
import com.thlee.stock.market.stockmarket.infrastructure.security.jwt.exception.InvalidTokenException;
import com.thlee.stock.market.stockmarket.user.domain.model.UserRole;
import com.thlee.stock.market.stockmarket.user.domain.service.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT 토큰 제공자 구현체
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProviderImpl implements JwtTokenProvider {

    private final JwtProperties jwtProperties;

    @Override
    public String generateAccessToken(Long userId, UserRole role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        claims.put("type", "ACCESS");

        return generateToken(claims, userId, jwtProperties.getAccessTokenExpiration());
    }

    @Override
    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "REFRESH");

        return generateToken(claims, userId, jwtProperties.getRefreshTokenExpiration());
    }

    @Override
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (ExpiredTokenException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Long getUserIdFromToken(String token) {
        return extractClaim(token, claims -> Long.parseLong(claims.getSubject()));
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            return expiration.before(new Date());
        } catch (ExpiredTokenException e) {
            return true;
        }
    }

    /**
     * JWT 토큰 생성
     */
    private String generateToken(Map<String, Object> claims, Long userId, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰에서 모든 Claims 추출
     */
    private Claims extractAllClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("토큰이 null이거나 빈 문자열입니다");
        }

        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("토큰이 만료되었습니다", e);
        } catch (Exception e) {
            throw new InvalidTokenException("유효하지 않은 토큰입니다", e);
        }
    }

    /**
     * 토큰에서 특정 Claim 추출
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 서명 키 생성
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}