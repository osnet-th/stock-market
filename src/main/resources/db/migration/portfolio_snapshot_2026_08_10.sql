-- 일자별 포트폴리오 스냅샷 테이블 (#110) — 자산 추이 차트 데이터 소스
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
--
-- (user_id, snapshot_date) 유일. 같은 날 재저장은 UPDATE 로 덮어쓴다.
-- 자동 적재 스케줄러는 이번 범위 밖 — 사용자가 [스냅샷 저장] 을 눌렀을 때만 기록된다.

CREATE TABLE IF NOT EXISTS portfolio_snapshot (
    id               BIGSERIAL      PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    snapshot_date    DATE           NOT NULL,
    total_evaluated  NUMERIC(18,2)  NOT NULL,
    total_invested   NUMERIC(18,2)  NOT NULL,
    created_at       TIMESTAMP      NOT NULL,
    CONSTRAINT uk_portfolio_snapshot_user_date UNIQUE (user_id, snapshot_date)
);

CREATE INDEX IF NOT EXISTS idx_portfolio_snapshot_user_id ON portfolio_snapshot (user_id);

-- 롤백
-- DROP TABLE IF EXISTS portfolio_snapshot;
