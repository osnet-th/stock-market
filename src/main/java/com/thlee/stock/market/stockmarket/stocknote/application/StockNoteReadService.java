package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.StockNoteDetailResult;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.StockNoteListItemResult;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.StockNoteListResult;
import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteNotFoundException;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteListFilter;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNotePriceSnapshotRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteTagRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 기록 조회 유스케이스. 상세는 aggregate 조립, 리스트는 IN 배치 fetch 로 N+1 회피.
 */
@Service
@RequiredArgsConstructor
public class StockNoteReadService {

    private final StockNoteRepository noteRepository;
    private final StockNoteTagRepository tagRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockNoteVerificationRepository verificationRepository;

    @Transactional(readOnly = true)
    public StockNoteDetailResult findById(Long noteId, Long userId) {
        StockNote note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new StockNoteNotFoundException(noteId));
        List<StockNoteTag> tags = tagRepository.findByNoteId(noteId);
        List<StockNotePriceSnapshot> snapshots = snapshotRepository
                .findAllByNoteIds(List.of(noteId))
                .getOrDefault(noteId, List.of());
        StockNoteVerification verification = verificationRepository.findByNoteId(noteId).orElse(null);
        return new StockNoteDetailResult(note, tags, snapshots, verification);
    }

    @Transactional(readOnly = true)
    public StockNoteListResult findList(Long userId, StockNoteListFilter filter) {
        List<StockNote> notes = noteRepository.findList(userId, filter);
        long total = noteRepository.countList(userId, filter);
        if (notes.isEmpty()) {
            return new StockNoteListResult(List.of(), total, filter.page(), filter.size());
        }
        List<Long> noteIds = notes.stream().map(StockNote::getId).toList();
        Map<Long, List<StockNoteTag>> tagMap = tagRepository.findAllByNoteIds(noteIds);
        Map<Long, List<StockNotePriceSnapshot>> snapshotMap = snapshotRepository.findAllByNoteIds(noteIds);
        Map<Long, StockNoteVerification> verificationMap = verificationRepository.findAllByNoteIds(noteIds);

        List<StockNoteListItemResult> items = new ArrayList<>(notes.size());
        for (StockNote note : notes) {
            Long id = note.getId();
            items.add(new StockNoteListItemResult(
                    note,
                    tagMap.getOrDefault(id, Collections.emptyList()),
                    snapshotMap.getOrDefault(id, Collections.emptyList()),
                    verificationMap.get(id)
            ));
        }
        return new StockNoteListResult(items, total, filter.page(), filter.size());
    }
}