package com.thlee.stock.market.stockmarket.user.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * OAuthAccount JPA Entity
 */
@Entity
@Table(name = "oauth_accounts")
public class OAuthAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    @Column(name = "issuer", nullable = false, length = 255)
    private String issuer;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "connected_at", nullable = false, updatable = false)
    private LocalDateTime connectedAt;

    protected OAuthAccountEntity() {
    }

    public OAuthAccountEntity(Long id, Long userId, String provider, String issuer,
                              String subject, String email, LocalDateTime connectedAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.issuer = issuer;
        this.subject = subject;
        this.email = email;
        this.connectedAt = connectedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (connectedAt == null) {
            connectedAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProvider() {
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

    // Setters (for JPA)
    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
}