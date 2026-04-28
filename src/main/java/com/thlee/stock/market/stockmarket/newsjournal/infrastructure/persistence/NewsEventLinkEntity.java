package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 뉴스 저널 사건의 관련 기사 링크 Entity.
 *
 * <p>{@link NewsEventEntity}와 연관관계 없이 {@code event_id} 컬럼 값 참조만 사용한다.
 * 정합성은 application 트랜잭션에서 보장 (CLAUDE.md Entity 연관관계 금지 규칙).
 */
@Entity
@Table(
        name = "news_event_link",
        indexes = {
                @Index(name = "idx_news_event_link_event",
                        columnList = "event_id, display_order")
        }
)
@Getter
public class NewsEventLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "url", nullable = false, length = 2000)
    private String url;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected NewsEventLinkEntity() {
    }

    public NewsEventLinkEntity(Long id, Long eventId, String title, String url, int displayOrder) {
        this.id = id;
        this.eventId = eventId;
        this.title = title;
        this.url = url;
        this.displayOrder = displayOrder;
    }
}