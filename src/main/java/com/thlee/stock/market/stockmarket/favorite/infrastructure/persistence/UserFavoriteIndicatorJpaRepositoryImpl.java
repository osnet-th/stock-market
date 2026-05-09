package com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring Data JPA가 *Impl 접미사로 자동 인식해 main repository에 합치는 custom fragment.
 * 가변 길이의 (id → priority) 매핑을 동적 native SQL로 발행해 일괄 갱신한다.
 *
 * 50개 이상이면 chunk로 분할 — 단일 statement의 파라미터/식 길이 비대화를 피한다.
 * 5개 이하는 개별 update로 단순화.
 */
public class UserFavoriteIndicatorJpaRepositoryImpl implements UserFavoriteIndicatorJpaRepositoryCustom {

    private static final int SMALL_BATCH_THRESHOLD = 5;
    private static final int CHUNK_SIZE = 50;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public int bulkUpdatePriority(Map<Long, Integer> idToPriority) {
        if (idToPriority == null || idToPriority.isEmpty()) {
            return 0;
        }
        int affected;
        if (idToPriority.size() <= SMALL_BATCH_THRESHOLD) {
            affected = updateOneByOne(idToPriority);
        } else {
            affected = updateInChunks(idToPriority);
        }
        entityManager.flush();
        entityManager.clear();
        return affected;
    }

    private int updateOneByOne(Map<Long, Integer> idToPriority) {
        int total = 0;
        for (Map.Entry<Long, Integer> entry : idToPriority.entrySet()) {
            Query q = entityManager.createNativeQuery(
                "UPDATE user_favorite_indicator SET priority = ? WHERE id = ?");
            q.setParameter(1, entry.getValue());
            q.setParameter(2, entry.getKey());
            total += q.executeUpdate();
        }
        return total;
    }

    private int updateInChunks(Map<Long, Integer> idToPriority) {
        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(idToPriority.entrySet());
        int total = 0;
        for (int from = 0; from < entries.size(); from += CHUNK_SIZE) {
            int to = Math.min(from + CHUNK_SIZE, entries.size());
            total += executeChunk(entries.subList(from, to));
        }
        return total;
    }

    private int executeChunk(List<Map.Entry<Long, Integer>> chunk) {
        StringBuilder sql = new StringBuilder("UPDATE user_favorite_indicator SET priority = CASE id");
        for (int i = 0; i < chunk.size(); i++) {
            sql.append(" WHEN ?").append(i * 2 + 1).append(" THEN ?").append(i * 2 + 2);
        }
        sql.append(" END WHERE id IN (");
        for (int i = 0; i < chunk.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?").append(chunk.size() * 2 + i + 1);
        }
        sql.append(")");

        Query q = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < chunk.size(); i++) {
            q.setParameter(i * 2 + 1, chunk.get(i).getKey());
            q.setParameter(i * 2 + 2, chunk.get(i).getValue());
        }
        for (int i = 0; i < chunk.size(); i++) {
            q.setParameter(chunk.size() * 2 + i + 1, chunk.get(i).getKey());
        }
        return q.executeUpdate();
    }
}
