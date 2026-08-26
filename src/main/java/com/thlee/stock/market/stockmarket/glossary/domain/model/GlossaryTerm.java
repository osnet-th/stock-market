package com.thlee.stock.market.stockmarket.glossary.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 개인 용어 사전의 용어(Term) 본체.
 *
 * <p>사용자가 직접 정리한 용어를 보관한다. 본문은 용어명({@code name}) +
 * 구조화 콘텐츠({@code content} → {@link GlossaryTermContent}) +
 * 카테고리({@code categoryId} → {@link GlossaryCategory}) +
 * 함께 볼 용어({@code relatedTermIds} — 같은 사용자 용어 ID 목록) 로 구성된다.
 *
 * <p>{@code categoryId == null} 인 경우 "미분류" 가상 카테고리로 노출된다 (R5).
 *
 * <p>Entity 연관관계는 두지 않고, 카테고리/함께 볼 용어는 ID 값 참조만 보유한다 (CLAUDE.md 규칙).
 * relatedTermIds 의 소유권/실존 검증은 application 계층 책임이며,
 * 도메인은 null/자기 참조/중복 제거와 상한만 강제한다.
 */
@Getter
public class GlossaryTerm {

    /** 용어명 길이 상한 */
    public static final int NAME_MAX_LENGTH = 200;

    /** 함께 볼 용어 개수 상한 */
    public static final int RELATED_TERMS_MAX = 20;

    private Long id;
    private final Long userId;

    private String name;
    private GlossaryTermContent content;
    private Long categoryId;
    private List<Long> relatedTermIds;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public GlossaryTerm(Long id, Long userId, String name, GlossaryTermContent content, Long categoryId,
                        List<Long> relatedTermIds, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.content = requireContent(content);
        this.categoryId = categoryId;
        this.relatedTermIds = normalizeRelated(id, relatedTermIds);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 용어 생성 팩토리.
     *
     * @param content        구조화 콘텐츠 (필드 전부 선택 — null 대신 {@link GlossaryTermContent#empty()} 허용)
     * @param categoryId     null 이면 "미분류"
     * @param relatedTermIds null 이면 빈 목록
     */
    public static GlossaryTerm create(Long userId, String name, GlossaryTermContent content, Long categoryId,
                                      List<Long> relatedTermIds) {
        requireNonNull(userId, "userId");
        LocalDateTime now = LocalDateTime.now();
        return new GlossaryTerm(null, userId, validName(name), content, categoryId, relatedTermIds, now, now);
    }

    /**
     * 용어 전체 교체 (PUT semantics). name 은 필수, 나머지는 null 허용.
     * categoryId 가 null 이면 "미분류" 로 이동.
     */
    public void replace(String name, GlossaryTermContent content, Long categoryId, List<Long> relatedTermIds) {
        this.name = validName(name);
        this.content = requireContent(content);
        this.categoryId = categoryId;
        this.relatedTermIds = normalizeRelated(this.id, relatedTermIds);
        this.updatedAt = LocalDateTime.now();
    }

    /** 저장 후 id 주입용 (RepositoryImpl 내부 재구성 전용) */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }

    private static String validName(String name) {
        requireNonBlank(name, "name");
        String trimmedName = name.trim();
        requireWithinNameLength(trimmedName);
        return trimmedName;
    }

    private static GlossaryTermContent requireContent(GlossaryTermContent content) {
        return content != null ? content : GlossaryTermContent.empty();
    }

    /** null 요소/자기 참조/중복 제거 후 불변 목록으로 보관. 상한 초과는 즉시 거부. */
    private static List<Long> normalizeRelated(Long selfId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> normalized = ids.stream()
                .filter(Objects::nonNull)
                .filter(relatedId -> !relatedId.equals(selfId))
                .distinct()
                .toList();
        requireRelatedWithinMax(normalized);
        return normalized;
    }

    private static void requireRelatedWithinMax(List<Long> relatedTermIds) {
        if (relatedTermIds.size() > RELATED_TERMS_MAX) {
            throw new IllegalArgumentException(
                "relatedTermIds 는 " + RELATED_TERMS_MAX + "개 이하여야 합니다.");
        }
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
}
