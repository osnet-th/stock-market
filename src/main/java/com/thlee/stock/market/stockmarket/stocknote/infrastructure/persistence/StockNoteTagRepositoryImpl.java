package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNoteTag;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteTagRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.mapper.StockNoteMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class StockNoteTagRepositoryImpl implements StockNoteTagRepository {

    private final StockNoteTagJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<StockNoteTag> saveAll(List<StockNoteTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        List<StockNoteTagEntity> entities = new ArrayList<>(tags.size());
        for (StockNoteTag t : tags) {
            entities.add(StockNoteMapper.toEntity(t));
        }
        List<StockNoteTagEntity> saved = jpaRepository.saveAll(entities);
        List<StockNoteTag> result = new ArrayList<>(saved.size());
        for (StockNoteTagEntity e : saved) {
            result.add(StockNoteMapper.toDomain(e));
        }
        return result;
    }

    @Override
    public List<StockNoteTag> findByNoteId(Long noteId) {
        return jpaRepository.findByNoteId(noteId).stream()
                .map(StockNoteMapper::toDomain)
                .toList();
    }

    @Override
    public Map<Long, List<StockNoteTag>> findAllByNoteIds(Collection<Long> noteIds) {
        if (noteIds == null || noteIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<StockNoteTag>> grouped = new LinkedHashMap<>();
        for (StockNoteTagEntity e : jpaRepository.findByNoteIdIn(noteIds)) {
            grouped.computeIfAbsent(e.getNoteId(), k -> new ArrayList<>())
                    .add(StockNoteMapper.toDomain(e));
        }
        return grouped;
    }

    @Override
    public void deleteByNoteId(Long noteId) {
        jpaRepository.deleteByNoteId(noteId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Long> findNoteIdsMatchingAllTags(Long userId, List<TagPair> tags,
                                                 Long excludeNoteId, NoteDirection directionFilter) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.note_id ");
        sql.append("FROM stock_note_tag t ");
        sql.append("JOIN stock_note n ON n.id = t.note_id ");
        sql.append("WHERE t.user_id = :userId ");
        if (excludeNoteId != null) {
            sql.append("AND n.id <> :excludeId ");
        }
        List<String> orClauses = new ArrayList<>(tags.size());
        for (int i = 0; i < tags.size(); i++) {
            orClauses.add("(t.tag_source = :src" + i + " AND t.tag_value = :val" + i + ")");
        }
        sql.append("AND (").append(String.join(" OR ", orClauses)).append(") ");
        if (directionFilter != null) {
            sql.append("AND n.direction = :direction ");
        }
        sql.append("GROUP BY t.note_id ");
        sql.append("HAVING COUNT(DISTINCT t.tag_source || '::' || t.tag_value) = :tagCount");

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("userId", userId);
        if (excludeNoteId != null) {
            query.setParameter("excludeId", excludeNoteId);
        }
        for (int i = 0; i < tags.size(); i++) {
            query.setParameter("src" + i, tags.get(i).source());
            query.setParameter("val" + i, tags.get(i).value());
        }
        if (directionFilter != null) {
            query.setParameter("direction", directionFilter.name());
        }
        query.setParameter("tagCount", tags.size());

        List<Object> rawResults = query.getResultList();
        List<Long> noteIds = new ArrayList<>(rawResults.size());
        for (Object o : rawResults) {
            noteIds.add(((Number) o).longValue());
        }
        return noteIds;
    }
}