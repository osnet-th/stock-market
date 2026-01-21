package com.thlee.stock.market.stockmarket.user.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthAccountTest {

    @Test
    void OAuth계정_생성() {
        // given
        OAuthProvider provider = OAuthProvider.GOOGLE;
        String issuer = "https://accounts.google.com";
        String subject = "1234567890";
        String email = "test@example.com";

        // when
        OAuthAccount account = OAuthAccount.create(provider, issuer, subject, email);

        // then
        assertThat(account.getProvider()).isEqualTo(provider);
        assertThat(account.getIssuer()).isEqualTo(issuer);
        assertThat(account.getSubject()).isEqualTo(subject);
        assertThat(account.getEmail()).isEqualTo(email);
        assertThat(account.getUserId()).isNull();
        assertThat(account.getConnectedAt()).isNotNull();
    }

    @Test
    void provider_issuer_subject가_일치하면_true() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "https://accounts.google.com",
                "1234567890",
                "test@example.com"
        );

        // when
        boolean matches = account.matches(
                OAuthProvider.GOOGLE,
                "https://accounts.google.com",
                "1234567890"
        );

        // then
        assertThat(matches).isTrue();
    }

    @Test
    void provider가_다르면_false() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );

        // when
        boolean matches = account.matches(OAuthProvider.KAKAO, "issuer", "subject");

        // then
        assertThat(matches).isFalse();
    }

    @Test
    void issuer가_다르면_false() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer1",
                "subject",
                "test@example.com"
        );

        // when
        boolean matches = account.matches(OAuthProvider.GOOGLE, "issuer2", "subject");

        // then
        assertThat(matches).isFalse();
    }

    @Test
    void subject가_다르면_false() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject1",
                "test@example.com"
        );

        // when
        boolean matches = account.matches(OAuthProvider.GOOGLE, "issuer", "subject2");

        // then
        assertThat(matches).isFalse();
    }

    @Test
    void 같은_provider면_true() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );

        // when
        boolean isSame = account.isSameProvider(OAuthProvider.GOOGLE);

        // then
        assertThat(isSame).isTrue();
    }

    @Test
    void 다른_provider면_false() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );

        // when
        boolean isSame = account.isSameProvider(OAuthProvider.KAKAO);

        // then
        assertThat(isSame).isFalse();
    }

    @Test
    void 사용자_연결() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );
        Long userId = 1L;

        // when
        account.connectToUser(userId);

        // then
        assertThat(account.getUserId()).isEqualTo(userId);
    }

    @Test
    void null_userId로_연결_시도하면_예외() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );

        // when & then
        assertThatThrownBy(() -> account.connectToUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId");
    }

    @Test
    void 이미_연결된_계정에_다시_연결_시도하면_예외() {
        // given
        OAuthAccount account = OAuthAccount.create(
                OAuthProvider.GOOGLE,
                "issuer",
                "subject",
                "test@example.com"
        );
        account.connectToUser(1L);

        // when & then
        assertThatThrownBy(() -> account.connectToUser(2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 연결");
    }
}