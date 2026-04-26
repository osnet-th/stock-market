package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNotePriceSnapshotRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.mapper.StockNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockNotePriceSnapshotRepositoryImpl implements StockNotePriceSnapshotRepository {

    private final StockNotePriceSnapshotJpaRepository jpaRepository;

    @Override
    public StockNotePriceSnapshot save(StockNotePriceSnapshot snapshot) {
        StockNotePriceSnapshotEntity saved = jpaRepository.save(StockNoteMapper.toEntity(snapshot));
        if (snapshot.getId() == null) {
            snapshot.assignId(saved.getId());
        }
        return StockNoteMapper.toDomain(saved);
    }

    @Override
    public List<StockNotePriceSnapshot> saveAll(List<StockNotePriceSnapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return List.of();
        }
        List<StockNotePriceSnapshotEntity> entities = new ArrayList<>(snapshots.size());
        for (StockNotePriceSnapshot s : snapshots) {
            entities.add(StockNoteMapper.toEntity(s));
        }
        List<StockNotePriceSnapshotEntity> saved = jpaRepository.saveAll(entities);
        List<StockNotePriceSnapshot> result = new ArrayList<>(saved.size());
        for (StockNotePriceSnapshotEntity e : saved) {
            result.add(StockNoteMapper.toDomain(e));
        }
        return result;
    }

    @Override
    public Optional<StockNotePriceSnapshot> findByNoteIdAndType(Long noteId, SnapshotType type) {
        return jpaRepository.findByNoteIdAndSnapshotType(noteId, type).map(StockNoteMapper::toDomain);
    }

    @Override
    public Map<Long, List<StockNotePriceSnapshot>> findAllByNoteIds(Collection<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<StockNotePriceSnapshot>> grouped = new LinkedHashMap<>();
        for (StockNotePriceSnapshotEntity e : jpaRepository.findByNoteIdIn(noteIds)) {
            grouped.computeIfAbsent(e.getNoteId(), k -> new ArrayList<>())
                    .add(StockNoteMapper.toDomain(e));
        }
        return grouped;
    }

    @Override
    public List<StockNotePriceSnapshot> findRetryable(SnapshotStatus status, int maxRetryCount, int limit) {
        return jpaRepository.findRetryable(status, maxRetryCount, PageRequest.of(0, limit)).stream()
                .map(StockNoteMapper::toDomain)
                .toList();
    }

    @Override
    public List<PendingCaptureTarget> findDueForCapture(SnapshotType type, MarketType marketType, LocalDate asOfDate) {
        // asOfDate 는 현재 예약 파라미터 (추후 영업일 이월 필터링에 사용).
        // 현 구현은 MarketType + PENDING 만 필터링하고, 영업일 판정은 application 계층이 수행.
        List<StockNotePriceSnapshotJpaRepository.PendingCaptureRow> rows =
                jpaRepository.findDueForCapture(type.name(), marketType.name());
        List<PendingCaptureTarget> result = new ArrayList<>(rows.size());
        for (StockNotePriceSnapshotJpaRepository.PendingCaptureRow r : rows) {
            result.add(new PendingCaptureTarget(
                    r.getNoteId(),
                    r.getStockCode(),
                    MarketType.valueOf(r.getMarketType()),
                    com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode.valueOf(r.getExchangeCode()),
                    r.getNoteDate(),
                    r.getAtNoteClosePrice()
            ));
        }
        return result;
    }

    @Override
    public int markSuccessIfPending(Long snapshotId, LocalDate priceDate,
                                    BigDecimal closePrice, BigDecimal changePercent) {
        return jpaRepository.markSuccessIfPending(snapshotId, priceDate, closePrice, changePercent,
                LocalDateTime.now());
    }

    @Override
    public void deleteByNoteId(Long noteId) {
        jpaRepository.deleteByNoteId(noteId);
    }
}