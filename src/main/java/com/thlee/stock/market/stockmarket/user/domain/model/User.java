package com.thlee.stock.market.stockmarket.user.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 사용자 Aggregate Root
 */
public class User {
    private Long id;
    private String name;
    private Nickname nickname;
    private PhoneNumber phoneNumber;
    private List<Long> oauthAccountIds;
    private UserStatus status;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    private User(UserStatus status, UserRole role, LocalDateTime createdAt) {
        this.status = status;
        this.role = role;
        this.oauthAccountIds = new ArrayList<>();
        this.createdAt = createdAt;
    }

    /**
     * 가입중 상태의 사용자 생성
     */
    public static User createSigning() {
        return new User(UserStatus.ACTIVE, UserRole.SIGNING_USER, LocalDateTime.now());
    }

    /**
     * 가입 완료 처리
     */
    public void completeSignup(String name, Nickname nickname, PhoneNumber phoneNumber) {
        if (this.role == UserRole.USER) {
            throw new IllegalStateException("이미 가입 완료된 사용자입니다.");
        }

        validateSignupParameters(name, nickname, phoneNumber);

        this.name = name;
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.role = UserRole.USER;
    }

    private void validateSignupParameters(String name, Nickname nickname, PhoneNumber phoneNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (nickname == null) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        if (phoneNumber == null) {
            throw new IllegalArgumentException("전화번호는 필수입니다.");
        }
    }

    /**
     * 활성화 상태 확인
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * 리소스 접근 가능 여부
     */
    public boolean canAccessResource() {
        return status == UserStatus.ACTIVE && role == UserRole.USER;
    }

    /**
     * 계정 연결 매칭 확인
     */
    public boolean matchesForConnection(Nickname nickname, PhoneNumber phoneNumber) {
        return Objects.equals(this.nickname, nickname) &&
                Objects.equals(this.phoneNumber, phoneNumber);
    }

    /**
     * OAuth 계정 ID 추가
     */
    public void addOAuthAccount(Long oauthAccountId) {
        if (oauthAccountId == null) {
            throw new IllegalArgumentException("oauthAccountId는 필수입니다.");
        }
        if (!this.oauthAccountIds.contains(oauthAccountId)) {
            this.oauthAccountIds.add(oauthAccountId);
        }
    }

    /**
     * OAuth 계정 ID 제거
     */
    public void removeOAuthAccount(Long oauthAccountId) {
        this.oauthAccountIds.remove(oauthAccountId);
    }

    /**
     * 사용자 삭제
     */
    public void delete() {
        if (this.status == UserStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 사용자입니다.");
        }
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Nickname getNickname() {
        return nickname;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public List<Long> getOauthAccountIds() {
        return Collections.unmodifiableList(oauthAccountIds);
    }

    public UserStatus getStatus() {
        return status;
    }

    public UserRole getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}