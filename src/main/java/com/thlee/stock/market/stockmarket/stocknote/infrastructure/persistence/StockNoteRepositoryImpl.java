package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteListFilter;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence.mapper.StockNoteMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * StockNote 포트 어댑터.
 *
 * <p>{@link #findList} 는 기본 필터(stockCode/fromDate/toDate/direction) 만 JPQL 로 동적 조립한다.
 * character / judgmentResult 필터는 별 테이블 JOIN 이 필요해 Phase 5 에서 확장 예정.
 */
@Repository
@RequiredArgsConstructor
public class StockNoteRepositoryImpl implements StockNoteRepository {

    private final StockNoteJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager em;

    @Override
    public StockNote save(StockNote note) {
        StockNoteEntity entity = StockNoteMapper.toEntity(note);
        StockNoteEntity saved = jpaRepository.save(entity);
        if (note.getId() == null) {
            note.assignId(saved.getId());
        }
        return StockNoteMapper.toDomain(saved);
    }

    @Override
    public Optional<StockNote> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(StockNoteMapper::toDomain);
    }

    @Override
    public Optional<StockNote> findById(Long id) {
        return jpaRepository.findById(id).map(StockNoteMapper::toDomain);
    }

    @Override
    public List<StockNote> findList(Long userId, StockNoteListFilter filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        String where = buildWhereClause(userId, filter, params);
        String jpql = "SELECT n FROM StockNoteEntity n" + where
                + " ORDER BY n.noteDate DESC, n.id DESC";
        TypedQuery<StockNoteEntity> query = em.createQuery(jpql, StockNoteEntity.class);
        params.forEach(query::setParameter);
        query.setFirstResult(filter.offset());
        query.setMaxResults(filter.limit());
        List<StockNote> result = new ArrayList<>(query.getResultList().size());
        for (StockNoteEntity e : query.getResultList()) {
            result.add(StockNoteMapper.toDomain(e));
        }
        return result;
    }

    @Override
    public long countList(Long userId, StockNoteListFilter filter) {
        Map<String, Object> params = new LinkedHashMap<>();
        String where = buildWhereClause(userId, filter, params);
        TypedQuery<Long> query = em.createQuery("SELECT COUNT(n) FROM StockNoteEntity n" + where, Long.class);
        params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    @Override
    public List<StockNote> findByUserAndStock(Long userId, String stockCode, LocalDate fromDate, LocalDate toDate) {
        return jpaRepository
                .findByUserIdAndStockCodeAndNoteDateBetweenOrderByNoteDateAsc(userId, stockCode, fromDate, toDate)
                .stream()
                .map(StockNoteMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByIdAndUserId(Long id, Long userId) {
        jpaRepository.deleteByIdAndUserId(id, userId);
    }

    private String buildWhereClause(Long userId, StockNoteListFilter filter, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(" WHERE n.userId = :userId");
        params.put("userId", userId);
        if (filter.stockCode() != null && !filter.stockCode().isBlank()) {
            sb.append(" AND n.stockCode = :stockCode");
            params.put("stockCode", filter.stockCode());
        }
        if (filter.fromDate() != null) {
            sb.append(" AND n.noteDate >= :fromDate");
            params.put("fromDate", filter.fromDate());
        }
        if (filter.toDate() != null) {
            sb.append(" AND n.noteDate <= :toDate");
            params.put("toDate", filter.toDate());
        }
        if (filter.direction() != null) {
            sb.append(" AND n.direction = :direction");
            params.put("direction", filter.direction());
        }
        // character / judgmentResult 필터는 Phase 5 에서 JOIN 쿼리로 확장.
        return sb.toString();
    }
}