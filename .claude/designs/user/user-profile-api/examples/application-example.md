# Application 계층 구현 예시

## UserProfileResponse

```java
package com.thlee.stock.market.stockmarket.user.application.dto;

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
        String displayName = resolveDisplayName(user);
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getNickname() != null ? user.getNickname().getValue() : null,
                user.getRole().name(),
                displayName
        );
    }

    private static String resolveDisplayName(User user) {
        if (user.getNickname() != null && user.getNickname().getValue() != null) {
            return user.getNickname().getValue();
        }
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }
        return "사용자 " + user.getId();
    }
}
```

## UserProfileService

```java
package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.user.application.dto.UserProfileResponse;
import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import com.thlee.stock.market.stockmarket.user.domain.service.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 사용자 프로필 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * JWT 토큰으로 사용자 프로필 조회
     */
    public UserProfileResponse getMyProfile(String token) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return UserProfileResponse.from(user);
    }
}
```