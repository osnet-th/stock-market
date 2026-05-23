package com.thlee.stock.market.stockmarket.glossary.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 개인 용어 사전의 용어(Term) 본체.
 *
 * <p>사용자가 직접 정리한 용어를 보관한다. 본문은 용어명({@code name}) + 설명({@code definition}) +
 * 카테고리({@code categoryId} → {@link GlossaryCategory}) 로 구성된다.
 *
 * <p>{@code categoryId == null} 인 경우 "미분류" 가상 카테고리로 노출된다 (R5).
 *
 * <p>Entity 연관관계는 두지 않고, 카테고리는 ID 값 참조만 보유한다 (CLAUDE.md 규칙).
 */
@Getter
public class GlossaryTerm {

    /** 용어명 길이 상한 */
    public static final int NAME_MAX_LENGTH = 200;

    /** 설명 길이 상한 (TEXT 컬럼이지만 보안/저장 보호 위해 application 검증) */
    public static final int DEFINITION_MAX_LENGTH = 4000;

    private Long id;
    private final Long userId;

    private String name;
    private String definition;
    private Long categoryId;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public GlossaryTerm(Long id, Long userId, String name, String definition, Long categoryId,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.definition = definition;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 용어 생성 팩토리.
     *
     * @param categoryId  null 이면 "미분류"
     * @param definition  null 또는 빈 문자열 허용 (선택 필드)
     */
    public static GlossaryTerm create(Long userId, String name, String definition, Long categoryId) {
        requireNonNull(userId, "userId");
        requireNonBlank(name, "name");
        String trimmedName = name.trim();
        requireWithinNameLength(trimmedName);
        requireWithinDefinitionLength(definition);
        LocalDateTime now = LocalDateTime.now();
        return new GlossaryTerm(null, userId, trimmedName, definition, categoryId, now, now);
    }

    /**
     * 용어 전체 교체 (PUT semantics). name 은 필수, definition/categoryId 는 null 허용.
     * categoryId 가 null 이면 "미분류" 로 이동.
     */
    public void replace(String name, String definition, Long categoryId) {
        requireNonBlank(name, "name");
        String trimmedName = name.trim();
        requireWithinNameLength(trimmedName);
        requireWithinDefinitionLength(definition);
        this.name = trimmedName;
        this.definition = definition;
        this.categoryId = categoryId;
        this.updatedAt = LocalDateTime.now();
    }

    /** 저장 후 id 주입용 (RepositoryImpl 내부 재구성 전용) */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
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

    private static void requireWithinNameLength(String trimmedName) {
        if (trimmedName.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "name 길이는 " + NAME_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    private static void requireWithinDefinitionLength(String definition) {
        if (definition != null && definition.length() > DEFINITION_MAX_LENGTH) {
            throw new IllegalArgumentException(
                "definition 길이는 " + DEFINITION_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }
}