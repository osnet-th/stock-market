package com.thlee.stock.market.stockmarket.glossary.domain.model;

import com.thlee.stock.market.stockmarket.glossary.domain.exception.ReservedCategoryNameException;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 개인 용어 사전의 카테고리.
 *
 * <p>사용자별 1:N 분류이며, {@code (user_id, name)} unique 로 중복 등록을 방지한다.
 * 용어 등록 시 사용자가 카테고리명을 인라인으로 입력하면 application 계층의 resolve 로직이
 * find-or-create 후 categoryId 를 용어에 주입한다.
 *
 * <p>{@link #RESERVED_NAME "미분류"} 는 예약어로, 일반 카테고리명으로 생성/리네임할 수 없다.
 * "미분류" 는 카테고리 row 가 아니라 용어의 {@code categoryId == null} 로 표현되는 가상 카테고리이다.
 *
 * <p>Entity 연관관계는 두지 않고, 용어는 {@code categoryId} 값 참조만 보유한다 (CLAUDE.md 규칙).
 */
@Getter
public class GlossaryCategory {

    /** 카테고리명 길이 상한 */
    public static final int NAME_MAX_LENGTH = 50;

    /** 가상 "미분류" 카테고리 예약어. 일반 카테고리명으로 사용 금지. */
    public static final String RESERVED_NAME = "미분류";

    private Long id;
    private final Long userId;
    private String name;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public GlossaryCategory(Long id, Long userId, String name,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 카테고리 생성 팩토리. {@code name} 은 trim 되어 저장된다.
     * "미분류" 예약어는 거부된다.
     */
    public static GlossaryCategory create(Long userId, String name) {
        requireNonNull(userId, "userId");
        requireNonBlank(name, "name");
        String trimmed = normalize(name);
        requireNotReserved(trimmed);
        requireWithinNameLength(trimmed);
        LocalDateTime now = LocalDateTime.now();
        return new GlossaryCategory(null, userId, trimmed, now, now);
    }

    /**
     * 카테고리 이름 변경. "미분류" 예약어는 거부된다.
     */
    public void rename(String newName) {
        requireNonBlank(newName, "name");
        String trimmed = normalize(newName);
        requireNotReserved(trimmed);
        requireWithinNameLength(trimmed);
        this.name = trimmed;
        this.updatedAt = LocalDateTime.now();
    }

    /** 저장 후 id 주입용 (RepositoryImpl 내부 재구성 전용) */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }

    /**
     * "미분류" 예약어인지 검사. application 계층에서 사전 검증에 재사용한다.
     * trim 후 정확 일치만 차단 ("미 분류" 같은 공백 변형은 별도 카테고리로 허용).
     */
    public static boolean isReservedName(String name) {
        return name != null && RESERVED_NAME.equals(name.trim());
    }

    private static String normalize(String name) {
        return name.trim();
    }

    private static void requireNonNull(Object v, String fieldName) {
        if (v == null) {
            throw new IllegalArgumentException(fieldName + " 는 필수입니다.");
        }
    }

    private static void requireNonBlank(String v, String fieldName) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 는 필수입니다.");
        }
    }

    private static void requireNotReserved(String trimmedName) {
        if (RESERVED_NAME.equals(trimmedName)) {
            throw new ReservedCategoryNameException(trimmedName);
        }
    }

    private static void requireWithinNameLength(String trimmedName) {
        if (trimmedName.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "name 길이는 " + NAME_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }
}