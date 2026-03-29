package com.thlee.stock.market.stockmarket.user.presentation;

import com.thlee.stock.market.stockmarket.user.application.UserProfileService;
import com.thlee.stock.market.stockmarket.user.application.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 사용자 프로필 관련 HTTP 엔드포인트
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 현재 로그인한 사용자의 프로필 조회
     * Authorization 헤더의 Bearer 토큰에서 사용자 식별
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @RequestHeader("Authorization") String authorization
    ) {
        String token = authorization.replace("Bearer ", "");
        UserProfileResponse response = userProfileService.getMyProfile(token);
        return ResponseEntity.ok(response);
    }

    /**
     * 알림 설정 토글
     */
    @PatchMapping("/me/notification")
    public ResponseEntity<Void> toggleNotification(
            @RequestHeader("Authorization") String authorization,
            @RequestBody Map<String, Boolean> body
    ) {
        String token = authorization.replace("Bearer ", "");
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        userProfileService.toggleNotification(token, enabled);
        return ResponseEntity.noContent().build();
    }
}