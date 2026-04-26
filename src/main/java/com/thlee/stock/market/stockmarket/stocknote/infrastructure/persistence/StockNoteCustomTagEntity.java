package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

/**
 * 사용자별 자유 태그 마스터. 자동완성 + 사용 빈도 집계.
 */
@Entity
@Table(
        name = "stock_note_custom_tag",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_note_custom_tag_user_value",
                        columnNames = {"user_id", "tag_value"})
        },
        indexes = {
                // 자동완성 접두어 스캔 (user_id 스코프 내 tag_value 시작 LIKE)
                @Index(name = "idx_stock_note_custom_tag_user_value",
                        columnList = "user_id, tag_value")
        }
)
@Getter
public class StockNoteCustomTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tag_value", nullable = false, length = 32)
    private String tagValue;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    protected StockNoteCustomTagEntity() {
    }

    public StockNoteCustomTagEntity(Long id, Long userId, String tagValue, long usageCount) {
        this.id = id;
        this.userId = userId;
        this.tagValue = tagValue;
        this.usageCount = usageCount;
    }
}