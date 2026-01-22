package com.thlee.stock.market.stockmarket.user.domain.service;

import com.thlee.stock.market.stockmarket.user.domain.model.UserRole;

/**
 * JWT 토큰 생성 및 검증을 위한 포트 인터페이스
 * 실제 구현은 infrastructure 계층에 위치
 */
public interface JwtTokenProvider {

    /**
     * Access Token 생성
     * @param userId 사용자 ID
     * @param role 사용자 권한
     * @return 생성된 Access Token
     */
    String generateAccessToken(Long userId, UserRole role);

    /**
     * Refresh Token 생성
     * @param userId 사용자 ID
     * @return 생성된 Refresh Token
     */
    String generateRefreshToken(Long userId);

    /**
     * 토큰 유효성 검증
     * @param token 검증할 토큰
     * @return 유효하면 true, 아니면 false
     */
    boolean validateToken(String token);

    /**
     * 토큰에서 사용자 ID 추출
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    Long getUserIdFromToken(String token);

    /**
     * 토큰 만료 여부 확인
     * @param token 확인할 토큰
     * @return 만료되었으면 true, 아니면 false
     */
    boolean isTokenExpired(String token);
}