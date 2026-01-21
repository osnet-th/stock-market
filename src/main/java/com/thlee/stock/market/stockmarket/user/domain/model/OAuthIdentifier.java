package com.thlee.stock.market.stockmarket.user.domain.model;

import java.util.Objects;

/**
 * OAuth 식별자 Value Object
 * provider + issuer + subject 조합으로 OAuth 계정을 고유하게 식별
 */
public class OAuthIdentifier {
    private final OAuthProvider provider;
    private final String issuer;
    private final String subject;

    public OAuthIdentifier(OAuthProvider provider, String issuer, String subject) {
        validate(provider, issuer, subject);
        this.provider = provider;
        this.issuer = issuer;
        this.subject = subject;
    }

    private void validate(OAuthProvider provider, String issuer, String subject) {
        if (provider == null) {
            throw new IllegalArgumentException("provider는 필수입니다.");
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer는 필수입니다.");
        }

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject는 필수입니다.");
        }
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getSubject() {
        return subject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OAuthIdentifier that = (OAuthIdentifier) o;
        return provider == that.provider &&
                Objects.equals(issuer, that.issuer) &&
                Objects.equals(subject, that.subject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, issuer, subject);
    }

    @Override
    public String toString() {
        return String.format("OAuthIdentifier{provider=%s, issuer=%s, subject=%s}", provider, issuer, subject);
    }
}