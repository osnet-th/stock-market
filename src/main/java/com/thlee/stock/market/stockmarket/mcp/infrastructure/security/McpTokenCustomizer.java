package com.thlee.stock.market.stockmarket.mcp.infrastructure.security;

import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;

/**
 * MCP 토큰 발급 정책. 토큰을 만들 때마다(코드 교환·refresh) 실행된다.
 * <ul>
 *   <li>sub(userId)가 가입을 마친 활성 사용자가 아니면 발급하지 않는다(탈퇴·정지 후 refresh로 계속 쓰지 못하게)</li>
 *   <li>access token의 aud를 MCP 리소스로 고정한다. 클라이언트가 resource 파라미터를 보내지 않아도
 *       리소스 서버의 aud 검증을 통과하게 하기 위해서다</li>
 * </ul>
 */
@RequiredArgsConstructor
public class McpTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final UserRepository userRepository;
    private final String resourceUri;

    @Override
    public void customize(JwtEncodingContext context) {
        if (!isActiveUser(context.getPrincipal().getName())) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT,
                    "The user is not active. Check the account in the app and connect again.", null));
        }
        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
            context.getClaims().audience(List.of(resourceUri));
        }
    }

    private boolean isActiveUser(String principalName) {
        long userId;
        try {
            userId = Long.parseLong(principalName);
        } catch (NumberFormatException e) {
            return false;
        }
        return userRepository.findById(userId).map(User::canAccessResource).orElse(false);
    }
}
