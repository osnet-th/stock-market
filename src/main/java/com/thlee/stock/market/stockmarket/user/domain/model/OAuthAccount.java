package com.thlee.stock.market.stockmarket.user.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * OAuth 계정 Entity
 */
public class OAuthAccount {
    private Long id;
    private Long userId;
    private final OAuthProvider provider;
    private final String issuer;
    private final String subject;
    private final String email;
    private final LocalDateTime connectedAt;

    private OAuthAccount(OAuthProvider provider, String issuer, String subject, String email, LocalDateTime connectedAt) {
        this.provider = provider;
        this.issuer = issuer;
        this.subject = subject;
        this.email = email;
        this.connectedAt = connectedAt;
    }

    public static OAuthAccount create(OAuthProvider provider, String issuer, String subject, String email) {
        validateParameters(provider, issuer, subject, email);
        return new OAuthAccount(provider, issuer, subject, email, LocalDateTime.now());
    }

    private static void validateParameters(OAuthProvider provider, String issuer, String subject, String email) {
        if (provider == null) {
            throw new IllegalArgumentException("provider는 필수입니다.");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("issuer는 필수입니다.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject는 필수입니다.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email은 필수입니다.");
        }
    }

    /**
     * provider + issuer + subject 일치 여부 확인
     */
    public boolean matches(OAuthProvider provider, String issuer, String subject) {
        return this.provider == provider &&
                Objects.equals(this.issuer, issuer) &&
                Objects.equals(this.subject, subject);
    }

    /**
     * 동일 provider 확인
     */
    public boolean isSameProvider(OAuthProvider provider) {
        return this.provider == provider;
    }

    /**
     * 사용자 연결
     */
    public void connectToUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (this.userId != null) {
            throw new IllegalStateException("이미 연결된 OAuth 계정입니다.");
        }
        this.userId = userId;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
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

    public String getEmail() {
        return email;
    }

    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
}