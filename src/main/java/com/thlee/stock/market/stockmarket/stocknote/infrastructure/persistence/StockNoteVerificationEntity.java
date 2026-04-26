package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사후 검증 Entity. {@code note_id} 에 대해 1:1 (unique).
 * 이 엔티티가 존재하면 본문 잠금 상태로 판정한다.
 */
@Entity
@Table(
        name = "stock_note_verification",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_note_verification_note",
                        columnNames = {"note_id"})
        }
)
@Getter
public class StockNoteVerificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "judgment_result", nullable = false, length = 15)
    private JudgmentResult judgmentResult;

    @Column(name = "verification_note", columnDefinition = "TEXT")
    private String verificationNote;

    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    protected StockNoteVerificationEntity() {
    }

    public StockNoteVerificationEntity(Long id, Long noteId, JudgmentResult judgmentResult,
                                       String verificationNote, LocalDateTime verifiedAt) {
        this.id = id;
        this.noteId = noteId;
        this.judgmentResult = judgmentResult;
        this.verificationNote = verificationNote;
        this.verifiedAt = verifiedAt;
    }
}