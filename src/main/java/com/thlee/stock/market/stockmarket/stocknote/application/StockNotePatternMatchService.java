package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.SimilarPatternResult;
import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteNotFoundException;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteVerification;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.JudgmentResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNotePriceSnapshotRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteTagRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 태그 조합 기반 유사 패턴 매칭.
 *
 * <p>기준 노트의 전체 태그 셋을 추출 → 동일 셋을 모두 포함한 과거 노트 조회 → 각 매치의
 * D+7/D+30 스냅샷과 검증 결과를 집계.
 */
@Service
@RequiredArgsConstructor
public class StockNotePatternMatchService {

    private static final int MAX_MATCHES = 20;

    private final StockNoteRepository noteRepository;
    private final StockNoteTagRepository tagRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockNoteVerificationRepository verificationRepository;

    @Transactional(readOnly = true)
    public SimilarPatternResult findSimilar(Long noteId, Long userId, NoteDirection directionFilter) {
        StockNote baseNote = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new StockNoteNotFoundException(noteId));
        List<StockNoteTag> baseTags = tagRepository.findByNoteId(noteId);
        if (baseTags.isEmpty()) {
            return new SimilarPatternResult(List.of(), List.of(), zeroAggregate());
        }
        List<StockNoteTagRepository.TagPair> tagPairs = baseTags.stream()
                .map(t -> new StockNoteTagRepository.TagPair(t.getTagSource(), t.getTagValue()))
                .toList();
        List<SimilarPatternResult.TagPair> basisTags = tagPairs.stream()
                .map(p -> new SimilarPatternResult.TagPair(p.source(), p.value()))
                .toList();

        List<Long> matchedIds = tagRepository.findNoteIdsMatchingAllTags(
                userId, tagPairs, noteId, directionFilter);
        if (matchedIds.isEmpty()) {
            return new SimilarPatternResult(basisTags, List.of(), zeroAggregate());
        }
        if (matchedIds.size() > MAX_MATCHES) {
            matchedIds = matchedIds.subList(0, MAX_MATCHES);
        }

        Map<Long, List<StockNotePriceSnapshot>> snapshotMap = snapshotRepository.findAllByNoteIds(matchedIds);
        Map<Long, StockNoteVerification> verificationMap = verificationRepository.findAllByNoteIds(matchedIds);
        List<StockNote> notes = matchedIds.stream()
                .map(id -> noteRepository.findById(id).orElse(null))
                .filter(n -> n != null)
                .toList();

        List<SimilarPatternResult.Match> matches = notes.stream()
                .map(n -> toMatch(n,
                        snapshotMap.getOrDefault(n.getId(), List.of()),
                        verificationMap.get(n.getId())))
                .toList();
        SimilarPatternResult.Aggregate aggregate = aggregate(matches);
        // basisTag 에서 baseNote.direction 을 사용하지 않으므로 baseNote 는 현재는 참조만.
        // 추후 확장: matching 결과에 baseNote 의 초기 판단을 비교 컬럼으로 넣을 수 있음.
        if (baseNote == null) {
            // 이론적으로 unreachable, 컴파일러 경고 회피용
            throw new IllegalStateException("base note disappeared");
        }
        return new SimilarPatternResult(basisTags, matches, aggregate);
    }

    private SimilarPatternResult.Match toMatch(StockNote n, List<StockNotePriceSnapshot> snapshots,
                                               StockNoteVerification verification) {
        BigDecimal d7 = successChangePercent(snapshots, SnapshotType.D_PLUS_7);
        BigDecimal d30 = successChangePercent(snapshots, SnapshotType.D_PLUS_30);
        JudgmentResult judgment = verification == null ? null : verification.getJudgmentResult();
        return new SimilarPatternResult.Match(
                n.getId(), n.getStockCode(), n.getNoteDate(), n.getDirection(),
                d7, d30, judgment);
    }

    private BigDecimal successChangePercent(List<StockNotePriceSnapshot> snapshots, SnapshotType type) {
        for (StockNotePriceSnapshot s : snapshots) {
            if (s.getSnapshotType() == type && s.getStatus() == SnapshotStatus.SUCCESS) {
                return s.getChangePercent();
            }
        }
        return null;
    }

    private SimilarPatternResult.Aggregate aggregate(List<SimilarPatternResult.Match> matches) {
        if (matches.isEmpty()) {
            return zeroAggregate();
        }
        long correct = 0, wrong = 0, partial = 0;
        BigDecimal d7Sum = BigDecimal.ZERO;
        int d7Count = 0;
        BigDecimal d30Sum = BigDecimal.ZERO;
        int d30Count = 0;
        for (SimilarPatternResult.Match m : matches) {
            if (m.judgmentResult() == JudgmentResult.CORRECT) correct++;
            else if (m.judgmentResult() == JudgmentResult.WRONG) wrong++;
            else if (m.judgmentResult() == JudgmentResult.PARTIAL) partial++;
            if (m.d7ChangePercent() != null) { d7Sum = d7Sum.add(m.d7ChangePercent()); d7Count++; }
            if (m.d30ChangePercent() != null) { d30Sum = d30Sum.add(m.d30ChangePercent()); d30Count++; }
        }
        BigDecimal avgD7 = d7Count == 0 ? null
                : d7Sum.divide(BigDecimal.valueOf(d7Count), 2, RoundingMode.HALF_UP);
        BigDecimal avgD30 = d30Count == 0 ? null
                : d30Sum.divide(BigDecimal.valueOf(d30Count), 2, RoundingMode.HALF_UP);
        return new SimilarPatternResult.Aggregate(matches.size(), correct, wrong, partial, avgD7, avgD30);
    }

    private SimilarPatternResult.Aggregate zeroAggregate() {
        return new SimilarPatternResult.Aggregate(0, 0, 0, 0, null, null);
    }
}