package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * 기록별 태그 Entity. 고정 enum 태그와 자유 태그가 {@code tag_source} 로 구분되어 한 테이블에 저장.
 *
 * <p>패턴 매칭 쿼리 성능 확보를 위해 {@code user_id} denormalize + covering index 구성.
 */
@Entity
@Table(
        name = "stock_note_tag",
        indexes = {
                // 패턴 매칭 covering index (심화 8): user_id → tag_source/value → note_id
                @Index(name = "idx_stock_note_tag_user_src_val_note",
                        columnList = "user_id, tag_source, tag_value, note_id"),
                // 노트별 태그 조회
                @Index(name = "idx_stock_note_tag_note", columnList = "note_id")
        }
)
@Getter
public class StockNoteTagEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tag_source", nullable = false, length = 20)
    private String tagSource;

    @Column(name = "tag_value", nullable = false, length = 32)
    private String tagValue;

    protected StockNoteTagEntity() {
    }

    public StockNoteTagEntity(Long id, Long noteId, Long userId, String tagSource, String tagValue) {
        this.id = id;
        this.noteId = noteId;
        this.userId = userId;
        this.tagSource = tagSource;
        this.tagValue = tagValue;
    }
}