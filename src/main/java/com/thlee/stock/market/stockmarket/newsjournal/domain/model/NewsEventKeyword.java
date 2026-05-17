package com.thlee.stock.market.stockmarket.newsjournal.domain.model;

import lombok.Getter;

/**
 * 뉴스 저널 사건의 키워드(해시태그).
 *
 * <p>{@link NewsEvent}와는 Entity 연관관계 없이 {@code eventId} 값 참조만 보유한다.
 * 갱신은 replace-all 정책이므로 본 모델은 불변에 가깝게 다루며, 별도의 수정 메서드를
 * 두지 않는다 (재생성으로 대체).
 */
@Getter
public class NewsEventKeyword {

    /** 키워드 길이 상한 */
    public static final int KEYWORD_MAX_LENGTH = 50;

    private Long id;
    private final Long eventId;
    private final String keyword;
    private final int displayOrder;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public NewsEventKeyword(Long id, Long eventId, String keyword, int displayOrder) {
        this.id = id;
        this.eventId = eventId;
        this.keyword = keyword;
        this.displayOrder = displayOrder;
    }

    /**
     * 신규 키워드 생성 팩토리.
     */
    public static NewsEventKeyword create(Long eventId, String keyword, int displayOrder) {
        requireNonNull(eventId, "eventId");
        requireNonBlank(keyword, "keyword");
        if (keyword.length() > KEYWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("keyword 길이는 " + KEYWORD_MAX_LENGTH + "자 이하여야 합니다.");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder 는 0 이상이어야 합니다.");
        }
        return new NewsEventKeyword(null, eventId, keyword, displayOrder);
    }

    /** 저장 후 id 주입용 (RepositoryImpl 내부 재구성 전용) */
    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }

    private static void requireNonNull(Object v, String name) {
        if (v == null) {
            throw new IllegalArgumentException(name + " 는 필수입니다.");
        }
    }

    private static void requireNonBlank(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(name + " 는 필수입니다.");
        }
    }
}
