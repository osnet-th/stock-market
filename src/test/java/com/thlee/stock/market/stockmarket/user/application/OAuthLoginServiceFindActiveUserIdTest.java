package com.thlee.stock.market.stockmarket.user.application;

import com.thlee.stock.market.stockmarket.logging.application.DomainEventLogger;
import com.thlee.stock.market.stockmarket.user.application.dto.OAuthLoginRequest;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthAccount;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.model.UserRole;
import com.thlee.stock.market.stockmarket.user.domain.model.UserStatus;
import com.thlee.stock.market.stockmarket.user.domain.repository.OAuthAccountRepository;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import com.thlee.stock.market.stockmarket.user.domain.service.JwtTokenProvider;
import com.thlee.stock.market.stockmarket.user.domain.service.OAuthConnectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthLoginServiceFindActiveUserIdTest {

    private static final String ISSUER = "https://kauth.kakao.com";
    private static final String SUBJECT = "kakao-subject";

    private UserRepository userRepository;
    private OAuthAccountRepository oauthAccountRepository;
    private OAuthLoginService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        oauthAccountRepository = mock(OAuthAccountRepository.class);
        service = new OAuthLoginService(userRepository, oauthAccountRepository,
                mock(OAuthConnectionService.class), mock(JwtTokenProvider.class),
                mock(KakaoOAuthService.class), mock(DomainEventLogger.class));
    }

    @Test
    @DisplayName("L1: 가입을 마친 활성 사용자면 userId를 돌려준다")
    void activeUser() {
        // given
        givenAccountOf(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, UserRole.USER, UserStatus.ACTIVE)));

        // when
        Optional<Long> userId = service.findActiveUserId(request());

        // then
        assertThat(userId).contains(7L);
    }

    @Test
    @DisplayName("L2: 연결된 계정이 없으면 비어 있다")
    void noAccount() {
        // given
        when(oauthAccountRepository.findByProviderAndIssuerAndSubject(OAuthProvider.KAKAO, ISSUER, SUBJECT))
                .thenReturn(Optional.empty());

        // when / then
        assertThat(service.findActiveUserId(request())).isEmpty();
    }

    @Test
    @DisplayName("L2: 가입 미완료(SIGNING_USER)면 비어 있다")
    void signingUser() {
        // given
        givenAccountOf(8L);
        when(userRepository.findById(8L)).thenReturn(Optional.of(user(8L, UserRole.SIGNING_USER, UserStatus.ACTIVE)));

        // when / then
        assertThat(service.findActiveUserId(request())).isEmpty();
    }

    @Test
    @DisplayName("L2: 탈퇴한 사용자면 비어 있다")
    void deletedUser() {
        // given
        givenAccountOf(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user(9L, UserRole.USER, UserStatus.DELETED)));

        // when / then
        assertThat(service.findActiveUserId(request())).isEmpty();
    }

    private void givenAccountOf(Long userId) {
        OAuthAccount account = new OAuthAccount(1L, userId, OAuthProvider.KAKAO, ISSUER, SUBJECT,
                "user@example.com", LocalDateTime.now());
        when(oauthAccountRepository.findByProviderAndIssuerAndSubject(OAuthProvider.KAKAO, ISSUER, SUBJECT))
                .thenReturn(Optional.of(account));
    }

    private static OAuthLoginRequest request() {
        return new OAuthLoginRequest(OAuthProvider.KAKAO, ISSUER, SUBJECT, "user@example.com");
    }

    private static User user(Long id, UserRole role, UserStatus status) {
        return new User(id, "홍길동", null, null, List.of(1L), status, role, false, LocalDateTime.now(), null);
    }
}
