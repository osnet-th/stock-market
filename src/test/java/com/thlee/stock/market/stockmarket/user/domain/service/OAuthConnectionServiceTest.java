package com.thlee.stock.market.stockmarket.user.domain.service;

import com.thlee.stock.market.stockmarket.user.domain.model.*;
import com.thlee.stock.market.stockmarket.user.domain.repository.OAuthAccountRepository;
import com.thlee.stock.market.stockmarket.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthConnectionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuthAccountRepository oauthAccountRepository;

    private OAuthConnectionService service;

    @BeforeEach
    void setUp() {
        service = new OAuthConnectionService(userRepository, oauthAccountRepository);
    }

    @Test
    void 연결된_OAuth계정이_없으면_provider_연결_가능() {
        // given
        User user = User.createSigning();
        given(oauthAccountRepository.findByIdIn(List.of())).willReturn(List.of());

        // when
        boolean canConnect = service.canConnectProvider(user, OAuthProvider.GOOGLE);

        // then
        assertThat(canConnect).isTrue();
    }

    @Test
    void 동일_provider가_이미_있으면_연결_불가() {
        // given
        User user = User.createSigning();
        user.addOAuthAccount(1L);

        OAuthAccount existingAccount = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );

        given(oauthAccountRepository.findByIdIn(List.of(1L))).willReturn(List.of(existingAccount));

        // when
        boolean canConnect = service.canConnectProvider(user, OAuthProvider.GOOGLE);

        // then
        assertThat(canConnect).isFalse();
    }

    @Test
    void 다른_provider는_연결_가능() {
        // given
        User user = User.createSigning();
        user.addOAuthAccount(1L);

        OAuthAccount googleAccount = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );

        given(oauthAccountRepository.findByIdIn(List.of(1L))).willReturn(List.of(googleAccount));

        // when
        boolean canConnect = service.canConnectProvider(user, OAuthProvider.KAKAO);

        // then
        assertThat(canConnect).isTrue();
    }

    @Test
    void OAuth계정_연결_성공() throws Exception {
        // given
        User user = User.createSigning();
        OAuthAccount oauthAccount = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );

        // Mock save 메서드가 ID를 설정한 객체를 반환하도록 설정
        given(userRepository.save(user)).willAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            setPrivateField(savedUser, "id", 1L);
            return savedUser;
        });

        given(oauthAccountRepository.save(oauthAccount)).willAnswer(invocation -> {
            OAuthAccount savedAccount = invocation.getArgument(0);
            setPrivateField(savedAccount, "id", 1L);
            return savedAccount;
        });

        given(oauthAccountRepository.findByIdIn(List.of())).willReturn(List.of());

        // when
        service.connectOAuthAccount(user, oauthAccount);

        // then
        verify(userRepository, org.mockito.Mockito.times(2)).save(user);
        verify(oauthAccountRepository, org.mockito.Mockito.times(2)).save(oauthAccount);
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void 동일_provider가_이미_있으면_연결_실패() {
        // given
        User user = User.createSigning();
        user.addOAuthAccount(1L);

        OAuthAccount existingAccount = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer1",
                "subject1",
                "test1@example.com"
        );

        OAuthAccount newAccount = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer2",
                "subject2",
                "test2@example.com"
        );

        given(oauthAccountRepository.findByIdIn(List.of(1L))).willReturn(List.of(existingAccount));

        // when & then
        assertThatThrownBy(() -> service.connectOAuthAccount(user, newAccount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 연결");
    }

    @Test
    void provider를_보유하고_있으면_true() {
        // given
        User user = User.createSigning();
        user.addOAuthAccount(1L);

        OAuthAccount googleAccount = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );

        given(oauthAccountRepository.findByIdIn(List.of(1L))).willReturn(List.of(googleAccount));

        // when
        boolean hasProvider = service.hasProvider(user, OAuthProvider.GOOGLE);

        // then
        assertThat(hasProvider).isTrue();
    }

    @Test
    void provider를_보유하지_않으면_false() {
        // given
        User user = User.createSigning();
        given(oauthAccountRepository.findByIdIn(List.of())).willReturn(List.of());

        // when
        boolean hasProvider = service.hasProvider(user, OAuthProvider.GOOGLE);

        // then
        assertThat(hasProvider).isFalse();
    }

    @Test
    void OAuth계정_연결_해제() {
        // given
        User user = User.createSigning();
        Long oauthAccountId = 1L;
        user.addOAuthAccount(oauthAccountId);

        // when
        service.disconnectOAuthAccount(user, oauthAccountId);

        // then
        assertThat(user.getOauthAccountIds()).doesNotContain(oauthAccountId);
        verify(userRepository).save(user);
    }
}