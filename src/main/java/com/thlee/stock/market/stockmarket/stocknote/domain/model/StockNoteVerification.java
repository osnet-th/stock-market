package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 사후 검증. 기록 본체 1건당 최대 1개 존재.
 *
 * <p>이 엔티티가 존재하면 application 계층에서 본문 수정이 차단된다 (본문 잠금 = verification 존재).
 */
@Getter
public class StockNoteVerification {

    private Long id;
    private final Long noteId;
    private JudgmentResult judgmentResult;
    private String verificationNote;
    private LocalDateTime verifiedAt;

    public StockNoteVerification(Long id, Long noteId, JudgmentResult judgmentResult,
                                 String verificationNote, LocalDateTime verifiedAt) {
        this.id = id;
        this.noteId = noteId;
        this.judgmentResult = judgmentResult;
        this.verificationNote = verificationNote;
        this.verifiedAt = verifiedAt;
    }

    public static StockNoteVerification create(Long noteId, JudgmentResult judgmentResult, String verificationNote) {
        Objects.requireNonNull(noteId, "noteId");
        Objects.requireNonNull(judgmentResult, "judgmentResult");
        requireTextWithin(verificationNote);
        return new StockNoteVerification(null, noteId, judgmentResult, verificationNote, LocalDateTime.now());
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }

    public void update(JudgmentResult judgmentResult, String verificationNote) {
        Objects.requireNonNull(judgmentResult, "judgmentResult");
        requireTextWithin(verificationNote);
        this.judgmentResult = judgmentResult;
        this.verificationNote = verificationNote;
        this.verifiedAt = LocalDateTime.now();
    }

    private static void requireTextWithin(String v) {
        if (v != null && v.length() > StockNote.TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "verificationNote 길이는 " + StockNote.TEXT_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }
}