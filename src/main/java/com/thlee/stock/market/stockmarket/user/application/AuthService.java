package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.TokenRefreshRequest;
import com.thlee.stock.market.stockmarket.user.application.dto.TokenRefreshResponse;
import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import com.thlee.stock.market.stockmarket.user.domain.service.JwtTokenProvider;

/**
 * 인증 관련 유스케이스 처리
 */
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public AuthService(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    /**
     * Refresh Token으로 새로운 Access Token과 Refresh Token 발급
     */
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.refreshToken();

        // 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }

        // 토큰 만료 확인
        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new RuntimeException("만료된 토큰입니다.");
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 새로운 토큰 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }
}