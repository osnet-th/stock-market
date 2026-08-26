package com.thlee.stock.market.stockmarket.glossary.presentation;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * glossary 컨트롤러 공용 인증 컨텍스트 헬퍼.
 *
 * <p>{@code JwtAuthenticationFilter} 가 principal 을 {@link Long} (userId) 으로 세팅한다는 가정 하에
 * 안전하게 추출한다. 인증되지 않은 호출(Authentication==null 또는 principal 이 Long 이 아님 —
 * dev 프로파일 permitAll + JWT 미발급, anonymous principal "anonymousUser") 에서는
 * {@link InsufficientAuthenticationException} 을 던져 ExceptionHandler 가 401 로 매핑한다.
 *
 * <p>known-debt: 본 헬퍼는 newsjournal 등 다른 도메인과 동일 위치(presentation/)에 두어 일관성을 유지한다.
 * 본래 {@code infrastructure/security/} 가 적합하므로 공통 CurrentUserContext 로 통합하는 리팩토링이 후속 과제.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GlossarySecurityContext {

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
