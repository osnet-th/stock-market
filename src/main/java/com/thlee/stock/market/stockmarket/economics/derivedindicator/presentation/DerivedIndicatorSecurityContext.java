package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 파생지표 컨트롤러 공용 인증 컨텍스트 헬퍼(가드형).
 *
 * <p>{@code JwtAuthenticationFilter}가 principal을 {@link Long}(userId)로 세팅한다는 가정 하에 안전 추출.
 * Authentication==null 또는 principal이 Long이 아닌 경우(dev permitAll + JWT 미발급, anonymous principal)
 * {@link InsufficientAuthenticationException}을 던져 401로 매핑한다.
 * (Favorite의 unguarded cast 대신 Glossary/NewsJournal 가드형 패턴을 따른다.)
 *
 * <p>known-debt: presentation/에 두어 기존 도메인과 일관성 유지. 공통 CurrentUserContext 통합은 후속 과제.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DerivedIndicatorSecurityContext {

    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new InsufficientAuthenticationException("로그인이 필요합니다.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        throw new InsufficientAuthenticationException("로그인이 필요합니다.");
    }
}
