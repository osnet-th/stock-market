package com.thlee.stock.market.stockmarket.newsjournal.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 뉴스 저널 사건의 주제 분류.
 *
 * <p>사용자별 1:N 분류이며, {@code (user_id, name)} unique 로 중복 등록을 방지한다.
 * 사건 입력 시 사용자가 *이름* 만 입력하면 application 계층의 resolve 로직이
 * find-or-create 후 categoryId 를 사건 본체에 주입한다.
 *
 * <p>Entity 연관관계는 두지 않고, 사건은 {@code categoryId} 값 참조만 보유한다 (CLAUDE.md 규칙).
 */
@Getter
public class NewsEventCategory {

    /** 분류명 길이 상한 */
    public static final int NAME_MAX_LENGTH = 50;

    private Long id;
    private final Long userId;
    private final String name;
    private final LocalDateTime createdAt;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public NewsEventCategory(Long id, Long userId, String name, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
    }

    /**
     * 신규 분류 생성 팩토리. {@code name} 은 trim 되어 저장된다.
     */
    public static NewsEventCategory create(Long userId, String name) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 은 필수입니다.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("name 길이는 " + NAME_MAX_LENGTH + "자 이하여야 합니다.");
        }
        return new NewsEventCategory(null, userId, trimmed, LocalDateTime.now());
    }

    /** 저장 후 id 주입용 (RepositoryImpl 내부 재구성 전용) */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }
}