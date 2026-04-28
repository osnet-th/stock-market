package com.thlee.stock.market.stockmarket.newsjournal.domain.model;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 뉴스 저널 사건(Event) 본체.
 *
 * <p>사용자가 직접 정리한 사건을 일자 단위로 보관한다. 본문은 WHAT / WHY / HOW 세 텍스트
 * 필드와 카테고리({@link EventCategory})로 구성되며, 관련 기사 URL은 별도 자식 엔티티
 * {@link NewsEventLink}로 분리해 관리한다.
 *
 * <p>Entity 연관관계는 두지 않고 자식은 {@code eventId} 값 참조만 보유한다.
 */
@Getter
public class NewsEvent {

    /** 본문(WHAT/WHY/HOW) 길이 상한 (보안/저장 보호) */
    public static final int TEXT_MAX_LENGTH = 4000;

    /** 제목 길이 상한 */
    public static final int TITLE_MAX_LENGTH = 200;

    private Long id;
    private final Long userId;

    private String title;
    private LocalDate occurredDate;
    private EventCategory category;
    private String what;
    private String why;
    private String how;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public NewsEvent(Long id, Long userId, String title, LocalDate occurredDate, EventCategory category,
                     String what, String why, String how,
                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.occurredDate = occurredDate;
        this.category = category;
        this.what = what;
        this.why = why;
        this.how = how;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 사건 생성 팩토리.
     *
     * @param today occurredDate 미래 검증 기준 (테스트 주입 목적)
     */
    public static NewsEvent create(Long userId, String title, LocalDate occurredDate, LocalDate today,
                                   EventCategory category, String what, String why, String how) {
        requireNonNull(userId, "userId");
        requireNonBlank(title, "title");
        requireTitleWithin(title);
        requireNonNull(occurredDate, "occurredDate");
        requireNonNull(today, "today");
        if (occurredDate.isAfter(today)) {
            throw new IllegalArgumentException("미래 날짜로 기록할 수 없습니다.");
        }
        requireNonNull(category, "category");
        requireTextWithin(what, "what");
        requireTextWithin(why, "why");
        requireTextWithin(how, "how");
        LocalDateTime now = LocalDateTime.now();
        return new NewsEvent(null, userId, title, occurredDate, category, what, why, how, now, now);
    }

    /**
     * 사건 본문 수정.
     */
    public void updateBody(String title, LocalDate occurredDate, LocalDate today,
                           EventCategory category, String what, String why, String how) {
        requireNonBlank(title, "title");
        requireTitleWithin(title);
        requireNonNull(occurredDate, "occurredDate");
        requireNonNull(today, "today");
        if (occurredDate.isAfter(today)) {
            throw new IllegalArgumentException("미래 날짜로 기록할 수 없습니다.");
        }
        requireNonNull(category, "category");
        requireTextWithin(what, "what");
        requireTextWithin(why, "why");
        requireTextWithin(how, "how");
        this.title = title;
        this.occurredDate = occurredDate;
        this.category = category;
        this.what = what;
        this.why = why;
        this.how = how;
        this.updatedAt = LocalDateTime.now();
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

    private static void requireTitleWithin(String v) {
        if (v.length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("title 길이는 " + TITLE_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    private static void requireTextWithin(String v, String name) {
        if (v != null && v.length() > TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException(name + " 길이는 " + TEXT_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }
}