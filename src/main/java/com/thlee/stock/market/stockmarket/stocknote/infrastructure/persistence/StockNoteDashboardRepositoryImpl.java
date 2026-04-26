package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stocknote.application.dto.DashboardResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteDashboardRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 대시보드 KPI 집계 어댑터. JPQL/Native SQL 5건 보유.
 * native 쿼리 1건 (aggregateTopTagCombos) 은 PostgreSQL STRING_AGG 사용.
 */
@Repository
public class StockNoteDashboardRepositoryImpl implements StockNoteDashboardRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public long countByUserAndNoteDateBetween(Long userId, LocalDate from, LocalDate to) {
        Query q = em.createQuery(
                "SELECT COUNT(n) FROM StockNoteEntity n WHERE n.userId = :u AND n.noteDate BETWEEN :f AND :t");
        q.setParameter("u", userId);
        q.setParameter("f", from);
        q.setParameter("t", to);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DashboardResult.HitRate aggregateHitRate(Long userId) {
        Query q = em.createQuery(
                "SELECT v.judgmentResult, COUNT(v) FROM StockNoteVerificationEntity v "
                        + "JOIN StockNoteEntity n ON n.id = v.noteId "
                        + "WHERE n.userId = :u GROUP BY v.judgmentResult");
        q.setParameter("u", userId);
        long correct = 0, wrong = 0, partial = 0;
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            String r = row[0] == null ? null : row[0].toString();
            long count = ((Number) row[1]).longValue();
            switch (r) {
                case "CORRECT" -> correct = count;
                case "WRONG" -> wrong = count;
                case "PARTIAL" -> partial = count;
                default -> { /* ignore */ }
            }
        }
        return new DashboardResult.HitRate(correct, wrong, partial, correct + wrong + partial);
    }

    @Override
    public long countPendingVerification(Long userId) {
        Query q = em.createQuery(
                "SELECT COUNT(n) FROM StockNoteEntity n "
                        + "WHERE n.userId = :u AND NOT EXISTS ("
                        + "  SELECT 1 FROM StockNoteVerificationEntity v WHERE v.noteId = n.id)");
        q.setParameter("u", userId);
        return ((Number) q.getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Long> aggregateCharacterDistribution(Long userId) {
        Query q = em.createQuery(
                "SELECT t.tagValue, COUNT(t) FROM StockNoteTagEntity t "
                        + "WHERE t.userId = :u AND t.tagSource = 'CHARACTER' "
                        + "GROUP BY t.tagValue ORDER BY COUNT(t) DESC");
        q.setParameter("u", userId);
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : (List<Object[]>) q.getResultList()) {
            result.put((String) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DashboardResult.TagComboEntry> aggregateTopTagCombos(Long userId, int limit) {
        Query q = em.createNativeQuery("""
                SELECT combo_key, COUNT(*) AS cnt
                FROM (
                    SELECT t.note_id,
                           STRING_AGG(t.tag_source || '::' || t.tag_value, '|'
                                      ORDER BY t.tag_source, t.tag_value) AS combo_key
                    FROM stock_note_tag t
                    WHERE t.user_id = :userId
                    GROUP BY t.note_id
                ) grouped
                GROUP BY combo_key
                ORDER BY cnt DESC
                LIMIT :limit
                """);
        q.setParameter("userId", userId);
        q.setParameter("limit", limit);
        List<DashboardResult.TagComboEntry> entries = new ArrayList<>();
        for (Object row : q.getResultList()) {
            Object[] r = (Object[]) row;
            String comboKey = (String) r[0];
            long count = ((Number) r[1]).longValue();
            entries.add(new DashboardResult.TagComboEntry(splitCombo(comboKey), count));
        }
        return entries;
    }

    private List<String> splitCombo(String comboKey) {
        if (comboKey == null) {
            return List.of();
        }
        return List.of(comboKey.split("\\|"));
    }
}
