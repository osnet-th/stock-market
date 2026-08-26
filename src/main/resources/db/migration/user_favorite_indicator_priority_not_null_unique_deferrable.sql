-- user_favorite_indicator.priority 운영 강화: NOT NULL + UNIQUE DEFERRABLE INITIALLY DEFERRED
--
-- 적용 순서:
--   1) issue #42 Phase 자바 배포 (Entity priority 컬럼 nullable 추가 + FavoritePriorityBackfillRunner 실행)
--   2) Bootstrap이 모든 NULL 행을 (user_id, source_type) 그룹별 dense 0..N-1로 채웠는지 확인
--      예) SELECT COUNT(*) FROM user_favorite_indicator WHERE priority IS NULL;  -- expect 0
--   3) 본 SQL 적용 (운영자 수동 psql 실행)
--   4) (선택) 다음 자바 배포에서 UserFavoriteIndicatorEntity.priority 컬럼을 nullable=false로 격상
--
-- 주의:
-- - DEFERRABLE INITIALLY DEFERRED는 Hibernate @UniqueConstraint로 표현 불가 → SQL로만 관리한다.
-- - Entity에 unique=true / @UniqueConstraint(columnNames={"user_id","source_type","priority"}) 절대 두지 말 것.
--   (ddl-auto=update가 NOT DEFERRABLE 중복 constraint를 silent하게 추가해 본 swap-update가 깨진다.)
-- - Postgres 기본 distinct 동작 가정 — 다중 NULL 허용은 backfill 미완 단계에서만 의미.
--   Postgres 15+ 환경 업그레이드 시 NULLS NOT DISTINCT 키워드를 도입하지 말 것.
-- - 본 plan: docs/plans/2026-05-07-001-feat-watchlist-priority-and-graph-layout-plan.md
-- - 본 SQL은 단일 트랜잭션으로 적용한다 — 어느 ALTER 하나라도 실패하면 전체 롤백되어
--   'NOT NULL은 적용되었지만 UNIQUE는 빠진' 어색한 중간 상태를 회피한다.
--   psql에서 `psql -v ON_ERROR_STOP=1 -f ...`와 함께 적용 권장.

BEGIN;

ALTER TABLE user_favorite_indicator
    ALTER COLUMN priority SET NOT NULL;

ALTER TABLE user_favorite_indicator
    DROP CONSTRAINT IF EXISTS user_favorite_indicator_priority_unique;

ALTER TABLE user_favorite_indicator
    ADD CONSTRAINT user_favorite_indicator_priority_unique
        UNIQUE (user_id, source_type, priority)
        DEFERRABLE INITIALLY DEFERRED;

COMMIT;