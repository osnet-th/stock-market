package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.CreateStockNoteCommand;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.TagInput;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.UpdateStockNoteCommand;
import com.thlee.stock.market.stockmarket.stocknote.application.event.StockNoteCreatedEvent;
import com.thlee.stock.market.stockmarket.stocknote.domain.exception.StockNoteLockedException;
import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteNotFoundException;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.FundamentalImpact;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteCustomTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.Valuation;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.RiseCharacter;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SupplyActor;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.TriggerType;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteCustomTagRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNotePriceSnapshotRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteTagRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteVerificationRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.cache.StocknoteCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 기록 생성/수정/삭제 유스케이스.
 *
 * <p>트랜잭션 경계는 본 서비스가 소유한다 (ARCHITECTURE.md 규칙). {@code create} 는 본체 + 태그 +
 * AT_NOTE PENDING 스냅샷을 원자적으로 insert 후 이벤트 발행. AT_NOTE 를 실제 현재가로 갱신하는
 * 비동기 리스너는 Phase 3 에서 추가된다.
 */
@Service
@RequiredArgsConstructor
public class StockNoteWriteService {

    private final StockNoteRepository noteRepository;
    private final StockNoteTagRepository tagRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockNoteVerificationRepository verificationRepository;
    private final StockNoteCustomTagRepository customTagRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @CacheEvict(cacheNames = StocknoteCacheConfig.DASHBOARD_CACHE_NAME,
            cacheManager = "stocknoteCacheManager", key = "#cmd.userId")
    public Long create(CreateStockNoteCommand cmd) {
        LocalDate today = LocalDate.now();
        Valuation valuation = new Valuation(cmd.per(), cmd.pbr(), cmd.evEbitda(), cmd.vsAverage());
        FundamentalImpact fundamentalImpact = new FundamentalImpact(
                cmd.revenueImpact(), cmd.profitImpact(), cmd.cashflowImpact(),
                cmd.oneTime(), cmd.structural());
        StockNote note = StockNote.create(
                cmd.userId(), cmd.stockCode(), cmd.marketType(), cmd.exchangeCode(),
                cmd.direction(), cmd.changePercent(), cmd.noteDate(), today,
                cmd.triggerText(), cmd.interpretationText(), cmd.riskText(),
                cmd.preReflected(), cmd.initialJudgment(),
                valuation, fundamentalImpact
        );
        StockNote saved = noteRepository.save(note);
        Long noteId = saved.getId();

        replaceTags(noteId, cmd.userId(), cmd.tags());
        // AT_NOTE + D+7 + D+30 PENDING 동시 생성. 도달일에 스케줄러가 markSuccess 로 전이.
        snapshotRepository.saveAll(List.of(
                StockNotePriceSnapshot.createPending(noteId, SnapshotType.AT_NOTE),
                StockNotePriceSnapshot.createPending(noteId, SnapshotType.D_PLUS_7),
                StockNotePriceSnapshot.createPending(noteId, SnapshotType.D_PLUS_30)
        ));

        eventPublisher.publishEvent(new StockNoteCreatedEvent(noteId));
        return noteId;
    }

    @Transactional
    @CacheEvict(cacheNames = StocknoteCacheConfig.DASHBOARD_CACHE_NAME,
            cacheManager = "stocknoteCacheManager", key = "#cmd.userId")
    public void update(UpdateStockNoteCommand cmd) {
        StockNote note = noteRepository.findByIdAndUserId(cmd.noteId(), cmd.userId())
                .orElseThrow(() -> new StockNoteNotFoundException(cmd.noteId()));
        if (verificationRepository.existsByNoteId(cmd.noteId())) {
            throw new StockNoteLockedException(cmd.noteId());
        }
        Valuation valuation = new Valuation(cmd.per(), cmd.pbr(), cmd.evEbitda(), cmd.vsAverage());
        FundamentalImpact fundamentalImpact = new FundamentalImpact(
                cmd.revenueImpact(), cmd.profitImpact(), cmd.cashflowImpact(),
                cmd.oneTime(), cmd.structural());
        note.updateBody(
                cmd.triggerText(), cmd.interpretationText(), cmd.riskText(),
                cmd.preReflected(), cmd.initialJudgment(),
                valuation, fundamentalImpact
        );
        noteRepository.save(note);

        replaceTags(cmd.noteId(), cmd.userId(), cmd.tags());
    }

    @Transactional
    @CacheEvict(cacheNames = StocknoteCacheConfig.DASHBOARD_CACHE_NAME,
            cacheManager = "stocknoteCacheManager", key = "#userId")
    public void delete(Long noteId, Long userId) {
        noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new StockNoteNotFoundException(noteId));
        // cascade 순서: verification → snapshots → tags → note
        verificationRepository.deleteByNoteId(noteId);
        snapshotRepository.deleteByNoteId(noteId);
        tagRepository.deleteByNoteId(noteId);
        noteRepository.deleteByIdAndUserId(noteId, userId);
    }

    /**
     * 태그 전체 교체 (기존 태그 삭제 후 신규 insert) + custom 태그 마스터 사용횟수 upsert.
     * 동일 (source, value) 중복 입력은 제거 (ce-review #25).
     */
    private void replaceTags(Long noteId, Long userId, List<TagInput> inputs) {
        tagRepository.deleteByNoteId(noteId);
        if (inputs == null || inputs.isEmpty()) {
            return;
        }
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>(inputs.size());
        List<TagInput> deduped = new ArrayList<>(inputs.size());
        for (TagInput input : inputs) {
            String key = input.source() + "::" + (input.value() == null ? "" : input.value().trim().toLowerCase());
            if (seen.add(key)) {
                deduped.add(input);
            }
        }
        List<StockNoteTag> tags = new ArrayList<>(deduped.size());
        for (TagInput input : deduped) {
            tags.add(buildTag(noteId, userId, input));
        }
        tagRepository.saveAll(tags);
        updateCustomTagUsage(userId, deduped);
    }

    private StockNoteTag buildTag(Long noteId, Long userId, TagInput input) {
        return switch (input.source()) {
            case StockNoteTag.SOURCE_TRIGGER -> StockNoteTag.createFixed(noteId, userId,
                    StockNoteTag.SOURCE_TRIGGER, TriggerType.valueOf(input.value()));
            case StockNoteTag.SOURCE_CHARACTER -> StockNoteTag.createFixed(noteId, userId,
                    StockNoteTag.SOURCE_CHARACTER, RiseCharacter.valueOf(input.value()));
            case StockNoteTag.SOURCE_SUPPLY -> StockNoteTag.createFixed(noteId, userId,
                    StockNoteTag.SOURCE_SUPPLY, SupplyActor.valueOf(input.value()));
            case StockNoteTag.SOURCE_CUSTOM -> StockNoteTag.createCustom(noteId, userId, input.value());
            default -> throw new IllegalArgumentException("알 수 없는 tag source: " + input.source());
        };
    }

    private void updateCustomTagUsage(Long userId, List<TagInput> inputs) {
        long currentCount = -1L; // lazy 초기화 (custom 태그 없으면 count 쿼리 생략)
        for (TagInput input : inputs) {
            if (!StockNoteTag.SOURCE_CUSTOM.equals(input.source())) {
                continue;
            }
            String normalized = StockNoteTag.normalizeCustom(input.value());
            int updated = customTagRepository.incrementUsage(userId, normalized);
            if (updated == 0) {
                if (currentCount < 0L) {
                    currentCount = customTagRepository.countByUserId(userId);
                }
                if (currentCount >= StockNoteCustomTag.MAX_PER_USER) {
                    throw new IllegalStateException("자유 태그 상한 초과: " + StockNoteCustomTag.MAX_PER_USER);
                }
                try {
                    customTagRepository.save(StockNoteCustomTag.createNew(userId, normalized));
                    currentCount++;
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // TOCTOU 동시 요청에서 다른 트랜잭션이 같은 (user_id, tag_value) 를 먼저 insert.
                    // unique 제약(uk_stock_note_custom_tag_user_value) 이 최종 가드. 본 트랜잭션은
                    // incrementUsage 재시도로 복구 (다른 트랜잭션 commit 후라야 보임 — read-after-commit).
                    int retried = customTagRepository.incrementUsage(userId, normalized);
                    if (retried == 0) {
                        throw e; // 동시 insert 도 아닌 다른 violation
                    }
                }
            }
        }
    }
}