package com.thlee.stock.market.stockmarket.favorite.infrastructure.config;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence.UserFavoriteIndicatorEntity;
import com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence.UserFavoriteIndicatorJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 앱 시작 시 priority가 NULL인 user_favorite_indicator 행에 대해
 * (user_id, source_type) 그룹별로 created_at ASC, id ASC 기준 dense 0..N-1을 부여.
 * 모든 행이 NOT NULL이면 즉시 종료(idempotent).
 *
 * <p>Multi-replica 안전성: Postgres advisory lock(`pg_try_advisory_xact_lock`)으로 backfill을 단일
 * replica에서만 수행하도록 직렬화한다. lock 획득 실패 시 다른 replica가 backfill 중이라 가정하고
 * 즉시 skip — 부팅을 차단하지 않는다.
 *
 * <p>오류 격리: backfill 도중 예외가 발생해도 ApplicationRunner가 부팅을 차단하지 않도록
 * try/catch로 격리하고 ERROR 로그만 남긴다. 다음 부팅에서 idempotent하게 재시도된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FavoritePriorityBackfillRunner implements ApplicationRunner {

    /** Postgres advisory lock key — 본 backfill 작업 전용 상수. 다른 advisory lock과 충돌 회피. */
    private static final long ADVISORY_LOCK_KEY = 4242042001L;

    private final UserFavoriteIndicatorJpaRepository repository;
    private final EntityManager entityManager;

    @Override
    public void run(ApplicationArguments args) {
        try {
            backfillWithLock();
        } catch (Exception e) {
            log.error("FAVORITE_PRIORITY_BACKFILL_FAILED next-boot-will-retry", e);
        }
    }

    @Transactional
    protected void backfillWithLock() {
        if (!tryAcquireAdvisoryLock()) {
            log.info("FAVORITE_PRIORITY_BACKFILL_SKIPPED reason=advisory-lock-held-by-another-replica");
            return;
        }
        List<UserFavoriteIndicatorEntity> nullRows = repository.findAllWithNullPriority();
        if (nullRows.isEmpty()) {
            return;
        }
        Map<GroupKey, Integer> nextSlot = collectNextSlotPerGroup(nullRows);
        Map<Long, Integer> assignments = assignDenseSequence(nullRows, nextSlot);
        applyAssignments(assignments);
        log.info("FAVORITE_PRIORITY_BACKFILL_APPLIED rows={}", assignments.size());
    }

    /**
     * Postgres `pg_try_advisory_xact_lock`으로 트랜잭션 단위 advisory lock 획득 시도.
     * 트랜잭션 종료 시 자동 해제. 다른 replica가 같은 키로 잠그고 있으면 false 반환.
     */
    private boolean tryAcquireAdvisoryLock() {
        Object result = entityManager
            .createNativeQuery("SELECT pg_try_advisory_xact_lock(:key)")
            .setParameter("key", ADVISORY_LOCK_KEY)
            .getSingleResult();
        return Boolean.TRUE.equals(result);
    }

    private Map<GroupKey, Integer> collectNextSlotPerGroup(List<UserFavoriteIndicatorEntity> nullRows) {
        Map<GroupKey, Integer> nextSlot = new HashMap<>();
        for (UserFavoriteIndicatorEntity row : nullRows) {
            GroupKey key = new GroupKey(row.getUserId(), row.getSourceType());
            nextSlot.computeIfAbsent(key, this::resolveStartingPriority);
        }
        return nextSlot;
    }

    private Integer resolveStartingPriority(GroupKey key) {
        Query q = entityManager.createQuery(
            "SELECT COALESCE(MAX(e.priority), -1) + 1 FROM UserFavoriteIndicatorEntity e " +
            "WHERE e.userId = :userId AND e.sourceType = :sourceType");
        q.setParameter("userId", key.userId());
        q.setParameter("sourceType", key.sourceType());
        Object result = q.getSingleResult();
        return result == null ? 0 : ((Number) result).intValue();
    }

    private Map<Long, Integer> assignDenseSequence(List<UserFavoriteIndicatorEntity> nullRows,
                                                   Map<GroupKey, Integer> nextSlot) {
        Map<Long, Integer> assignments = new LinkedHashMap<>();
        for (UserFavoriteIndicatorEntity row : nullRows) {
            GroupKey key = new GroupKey(row.getUserId(), row.getSourceType());
            int slot = nextSlot.get(key);
            assignments.put(row.getId(), slot);
            nextSlot.put(key, slot + 1);
        }
        return assignments;
    }

    /**
     * Repository의 chunk CASE WHEN bulk update를 재사용해 N개 개별 UPDATE 라운드트립을
     * chunk(50개) 단위로 묶어 부팅 지연을 줄인다.
     */
    private void applyAssignments(Map<Long, Integer> assignments) {
        repository.bulkUpdatePriority(assignments);
    }

    private record GroupKey(Long userId, FavoriteIndicatorSourceType sourceType) {
    }
}