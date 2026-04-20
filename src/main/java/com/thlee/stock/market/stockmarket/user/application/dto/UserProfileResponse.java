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
        String displayName,
        boolean notificationEnabled,
        boolean admin
) {

    /**
     * User 엔티티로부터 응답 생성
     * 표시 우선순위: nickname > name > "사용자 {id}"
     *
     * @param admin 운영자 화이트리스트({@code app.logging.admin.user-ids}) 포함 여부.
     *              프론트 메뉴 조건부 렌더링 용도.
     */
    public static UserProfileResponse from(User user, boolean admin) {
        String nicknameValue = user.getNickname() != null ? user.getNickname().getValue() : null;
        String displayName = resolveDisplayName(user, nicknameValue);

        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                nicknameValue,
                user.getRole().name(),
                displayName,
                user.isNotificationEnabled(),
                admin
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