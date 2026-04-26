package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import lombok.Getter;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 기록별 태그. 고정 enum 태그(TRIGGER/CHARACTER/SUPPLY) 와 자유 태그(CUSTOM) 가
 * 같은 테이블에 저장되며 {@code tagSource} 로 구분된다.
 *
 * <p>사용자 스코프 쿼리 성능을 위해 {@code userId} 를 denormalize 로 보유한다.
 */
@Getter
public class StockNoteTag {

    public static final int TAG_VALUE_MAX = 32;
    public static final String SOURCE_TRIGGER = "TRIGGER";
    public static final String SOURCE_CHARACTER = "CHARACTER";
    public static final String SOURCE_SUPPLY = "SUPPLY";
    public static final String SOURCE_CUSTOM = "CUSTOM";

    /** 자유 태그 정규화 패턴: 한글/영문/숫자/._- 공백 허용 */
    private static final Pattern CUSTOM_TAG_PATTERN = Pattern.compile("^[\\p{IsHangul}\\p{Alnum}._\\- ]+$");

    private Long id;
    private final Long noteId;
    private final Long userId;
    private final String tagSource;
    private final String tagValue;

    public StockNoteTag(Long id, Long noteId, Long userId, String tagSource, String tagValue) {
        this.id = id;
        this.noteId = noteId;
        this.userId = userId;
        this.tagSource = tagSource;
        this.tagValue = tagValue;
    }

    public static StockNoteTag createFixed(Long noteId, Long userId, String tagSource, Enum<?> enumValue) {
        Objects.requireNonNull(noteId, "noteId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(enumValue, "enumValue");
        requireFixedSource(tagSource);
        return new StockNoteTag(null, noteId, userId, tagSource, enumValue.name());
    }

    public static StockNoteTag createCustom(Long noteId, Long userId, String rawTagValue) {
        Objects.requireNonNull(noteId, "noteId");
        Objects.requireNonNull(userId, "userId");
        String normalized = normalizeCustom(rawTagValue);
        return new StockNoteTag(null, noteId, userId, SOURCE_CUSTOM, normalized);
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }

    /** 자유 태그 정규화: trim + lowercase + 길이/패턴 검증. */
    public static String normalizeCustom(String rawTagValue) {
        if (rawTagValue == null || rawTagValue.isBlank()) {
            throw new IllegalArgumentException("자유 태그는 비어 있을 수 없습니다.");
        }
        String normalized = rawTagValue.trim().toLowerCase();
        if (normalized.length() > TAG_VALUE_MAX) {
            throw new IllegalArgumentException("자유 태그는 " + TAG_VALUE_MAX + "자 이하여야 합니다.");
        }
        if (!CUSTOM_TAG_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("자유 태그에 허용되지 않는 문자가 포함되었습니다.");
        }
        return normalized;
    }

    private static void requireFixedSource(String tagSource) {
        if (!SOURCE_TRIGGER.equals(tagSource)
                && !SOURCE_CHARACTER.equals(tagSource)
                && !SOURCE_SUPPLY.equals(tagSource)) {
            throw new IllegalArgumentException("고정 태그 source 가 올바르지 않습니다: " + tagSource);
        }
    }
}