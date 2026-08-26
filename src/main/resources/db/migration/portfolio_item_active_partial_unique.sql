-- portfolio_item: ACTIVE 자산만 유일하도록 partial unique index 전환 (issue #66)
--
-- 배경:
--   기존 full UNIQUE(user_id, item_name, asset_type)는 status를 보지 않아
--   전량 매도로 CLOSED가 된 row가 남으면 동일 자산 재등록(새 ACTIVE row)이
--   23505 -> DataIntegrityViolationException -> 409 CONFLICT로 막힌다.
--   앱 중복검사(PortfolioService.validateDuplicate)는 이미 ACTIVE 한정이므로
--   DB 제약도 ACTIVE만 유일하게 맞춰 의도와 일치시킨다.
--   불변식: (user_id, item_name, asset_type)별 ACTIVE 최대 1개, CLOSED 무제한 누적.
--
-- 적용 순서(중요):
--   1) 자바 배포 — PortfolioItemEntity의 @UniqueConstraint(uk_portfolio_item) 제거본.
--      (ddl-auto=update는 기존 uk_portfolio_item을 DROP하지 않으며,
--       어노테이션이 남은 구버전이 재기동되면 full 제약을 silent 재생성한다.)
--   2) 본 SQL 적용 (운영자 수동 psql). dev도 동일 적용 필요 —
--      어노테이션 제거만으로는 기존 DB의 uk_portfolio_item이 사라지지 않는다.
--
-- 주의(롤백 비대칭):
--   본 index 적용 후 사용자가 CLOSED가 있는 조합에 새 ACTIVE를 등록하면
--   (CLOSED + ACTIVE) 중복 조합이 생긴다. 그 이후 full 제약 복원은 실패한다.
--   롤백은 재등록이 발생하기 전에만 무손실. 롤백 SQL:
--     BEGIN;
--     DROP INDEX IF EXISTS uk_portfolio_item_active;
--     ALTER TABLE portfolio_item
--         ADD CONSTRAINT uk_portfolio_item UNIQUE (user_id, item_name, asset_type);
--     COMMIT;
--
-- plan: docs/plans/2026-05-30-001-fix-portfolio-asset-reregister-409-plan.md
-- 적용 권장: psql -v ON_ERROR_STOP=1 -f portfolio_item_active_partial_unique.sql
-- 본 SQL은 단일 트랜잭션으로 적용한다 — DROP/CREATE 중 하나라도 실패하면 전체 롤백되어
-- 'full 제약은 빠졌지만 partial index는 없는' 어색한 중간 상태를 회피한다.

BEGIN;

ALTER TABLE portfolio_item
    DROP CONSTRAINT IF EXISTS uk_portfolio_item;

CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_item_active
    ON portfolio_item (user_id, item_name, asset_type)
    WHERE status = 'ACTIVE';

COMMIT;
