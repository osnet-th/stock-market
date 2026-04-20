package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.logging.infrastructure.config.AdminProperties;
import com.thlee.stock.market.stockmarket.user.application.dto.UserProfileResponse;
import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import com.thlee.stock.market.stockmarket.user.domain.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 프로필 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AdminProperties adminProperties;

    /**
     * JWT 토큰으로 사용자 프로필 조회
     */
    public UserProfileResponse getMyProfile(String token) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        boolean admin = adminProperties.userIds().contains(userId);
        return UserProfileResponse.from(user, admin);
    }

    /**
     * 알림 설정 토글
     */
    @Transactional
    public void toggleNotification(String token, boolean enabled) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (enabled) {
            user.enableNotification();
        } else {
            user.disableNotification();
        }

        userRepository.save(user);
    }
}