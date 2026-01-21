package com.thlee.stock.market.stockmarket.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthIdentifierTest {

    @Test
    void 정상적인_OAuthIdentifier_생성() {
        // given
        OAuthProvider provider = OAuthProvider.GOOGLE;
        String issuer = "https://accounts.google.com";
        String subject = "1234567890";

        // when
        OAuthIdentifier identifier = new OAuthIdentifier(provider, issuer, subject);

        // then
        assertThat(identifier.getProvider()).isEqualTo(provider);
        assertThat(identifier.getIssuer()).isEqualTo(issuer);
        assertThat(identifier.getSubject()).isEqualTo(subject);
    }

    @Test
    void null_provider_거부() {
        // when & then
        assertThatThrownBy(() -> new OAuthIdentifier(null, "issuer", "subject"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("provider");
    }

    @Test
    void null_issuer_거부() {
        // when & then
        assertThatThrownBy(() -> new OAuthIdentifier(OAuthProvider.GOOGLE, null, "subject"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void null_subject_거부() {
        // when & then
        assertThatThrownBy(() -> new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void 빈_문자열_issuer_거부() {
        // when & then
        assertThatThrownBy(() -> new OAuthIdentifier(OAuthProvider.GOOGLE, "", "subject"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void 빈_문자열_subject_거부() {
        // when & then
        assertThatThrownBy(() -> new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void 같은_값의_OAuthIdentifier는_동등하다() {
        // given
        OAuthIdentifier identifier1 = new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer", "subject");
        OAuthIdentifier identifier2 = new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer", "subject");

        // when & then
        assertThat(identifier1).isEqualTo(identifier2);
        assertThat(identifier1.hashCode()).isEqualTo(identifier2.hashCode());
    }

    @Test
    void 다른_provider의_OAuthIdentifier는_동등하지_않다() {
        // given
        OAuthIdentifier identifier1 = new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer", "subject");
        OAuthIdentifier identifier2 = new OAuthIdentifier(OAuthProvider.KAKAO, "issuer", "subject");

        // when & then
        assertThat(identifier1).isNotEqualTo(identifier2);
    }

    @Test
    void 다른_issuer의_OAuthIdentifier는_동등하지_않다() {
        // given
        OAuthIdentifier identifier1 = new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer1", "subject");
        OAuthIdentifier identifier2 = new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer2", "subject");

        // when & then
        assertThat(identifier1).isNotEqualTo(identifier2);
    }

    @Test
    void 다른_subject의_OAuthIdentifier는_동등하지_않다() {
        // given
        OAuthIdentifier identifier1 = new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer", "subject1");
        OAuthIdentifier identifier2 = new OAuthIdentifier(OAuthProvider.GOOGLE, "issuer", "subject2");

        // when & then
        assertThat(identifier1).isNotEqualTo(identifier2);
    }
}