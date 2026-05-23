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
 * 뉴스 저널 사건의 키워드 Entity.
 *
 * <p>{@link NewsEventEntity}와 연관관계 없이 {@code event_id} 컬럼 값 참조만 사용한다.
 * 정합성은 application 트랜잭션에서 보장 (CLAUDE.md Entity 연관관계 금지 규칙).
 */
@Entity
@Table(
        name = "news_event_keyword",
        indexes = {
                @Index(name = "idx_news_event_keyword_event",
                        columnList = "event_id, display_order")
        }
)
@Getter
public class NewsEventKeywordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "keyword", nullable = false, length = 50)
    private String keyword;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected NewsEventKeywordEntity() {
    }

    public NewsEventKeywordEntity(Long id, Long eventId, String keyword, int displayOrder) {
        this.id = id;
        this.eventId = eventId;
        this.keyword = keyword;
        this.displayOrder = displayOrder;
    }
}
