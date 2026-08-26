package com.thlee.stock.market.stockmarket.news.presentation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 인증 principal(Long userId) 확인 헬퍼 (#114 review H1).
 *
 * <p>뉴스·키워드 API 는 `userId` 를 파라미터로 받는 기존 관행을 따르는데, 값을 그대로 믿으면
 * 로그인한 사용자가 파라미터만 바꿔 남의 데이터를 읽을 수 있다.
 * 파라미터 형태는 유지하되 인증 주체와 같은지만 확인해 그 경로를 막는다.
 *
 * <p>예외를 던지지 않고 boolean 을 돌려주는 이유: {@code GlobalExceptionHandler} 에
 * {@code AccessDeniedException} / {@code InsufficientAuthenticationException} 핸들러가 없어
 * 던지면 500 으로 떨어진다. 호출부가 403 을 직접 반환하도록 두는 편이 정확하다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NewsSecurityContext {

    /**
     * 인증 principal 이 Long userId 이고 인자와 같으면 true.
     * 미인증이거나 principal 형태가 다르면 false.
     */
    public static boolean matchesCurrentUser(Long userId) {
        if (userId == null) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getPrincipal() instanceof Long currentUserId
                && currentUserId.equals(userId);
    }
}
