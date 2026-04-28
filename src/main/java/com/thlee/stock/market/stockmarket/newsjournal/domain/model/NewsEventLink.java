package com.thlee.stock.market.stockmarket.newsjournal.domain.model;

import lombok.Getter;

/**
 * 뉴스 저널 사건의 관련 기사 링크.
 *
 * <p>{@link NewsEvent}와는 Entity 연관관계 없이 {@code eventId} 값 참조만 보유한다.
 * 갱신은 replace-all 정책이므로 본 모델은 불변에 가깝게 다루며, 별도의 본문 수정 메서드를
 * 두지 않는다 (재생성으로 대체).
 */
@Getter
public class NewsEventLink {

    /** 제목 길이 상한 */
    public static final int TITLE_MAX_LENGTH = 200;

    /** URL 길이 상한 */
    public static final int URL_MAX_LENGTH = 2000;

    private Long id;
    private final Long eventId;
    private final String title;
    private final String url;
    private final int displayOrder;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public NewsEventLink(Long id, Long eventId, String title, String url, int displayOrder) {
        this.id = id;
        this.eventId = eventId;
        this.title = title;
        this.url = url;
        this.displayOrder = displayOrder;
    }

    /**
     * 신규 링크 생성 팩토리.
     */
    public static NewsEventLink create(Long eventId, String title, String url, int displayOrder) {
        requireNonNull(eventId, "eventId");
        requireNonBlank(title, "title");
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("title 길이는 " + TITLE_MAX_LENGTH + "자 이하여야 합니다.");
        }
        requireNonBlank(url, "url");
        if (url.length() > URL_MAX_LENGTH) {
            throw new IllegalArgumentException("url 길이는 " + URL_MAX_LENGTH + "자 이하여야 합니다.");
        }
        if (displayOrder < 0) {
            throw new IllegalArgumentException("displayOrder 는 0 이상이어야 합니다.");
        }
        return new NewsEventLink(null, eventId, title, url, displayOrder);
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