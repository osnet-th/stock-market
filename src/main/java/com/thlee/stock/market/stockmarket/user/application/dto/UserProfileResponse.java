package com.thlee.stock.market.stockmarket.user.application.dto;

import com.thlee.stock.market.stockmarket.user.domain.model.User;

/**
 * 사용자 프로필 응답 DTO
 */
public record UserProfileResponse(
        Long userId,
        String name,
        String nickname,
        String role,
        String displayName
) {

    /**
     * User 엔티티로부터 응답 생성
     * 표시 우선순위: nickname > name > "사용자 {id}"
     */
    public static UserProfileResponse from(User user) {
        String nicknameValue = user.getNickname() != null ? user.getNickname().getValue() : null;
        String displayName = resolveDisplayName(user, nicknameValue);

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                nicknameValue,
                user.getRole().name(),
                displayName
        );
    }

    private static String resolveDisplayName(User user, String nicknameValue) {
        if (nicknameValue != null && !nicknameValue.isBlank()) {
            return nicknameValue;
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return "사용자 " + user.getId();
    }
}