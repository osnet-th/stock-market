package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import lombok.Getter;

import java.util.Objects;

/**
 * 사용자별 자유 태그 마스터. 자동완성과 사용 빈도 추적용.
 *
 * <p>사용자당 최대 {@link #MAX_PER_USER} 개까지 보유. 업데이트(사용 횟수 증가) 는
 * RepositoryImpl 에서 원자적 UPDATE 로 처리한다.
 */
@Getter
public class StockNoteCustomTag {

    public static final int MAX_PER_USER = 500;

    private Long id;
    private final Long userId;
    private final String tagValue;
    private long usageCount;

    public StockNoteCustomTag(Long id, Long userId, String tagValue, long usageCount) {
        this.id = id;
        this.userId = userId;
        this.tagValue = tagValue;
        this.usageCount = usageCount;
    }

    /**
     * 신규 태그 (usageCount = 1). tagValue 는 이미 {@link StockNoteTag#normalizeCustom} 로 정규화되어 있어야 한다.
     */
    public static StockNoteCustomTag createNew(Long userId, String normalizedValue) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(normalizedValue, "normalizedValue");
        return new StockNoteCustomTag(null, userId, normalizedValue, 1L);
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }
}