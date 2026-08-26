-- 목표 자산 배분 설정 테이블 (#102)
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.

CREATE TABLE IF NOT EXISTS allocation_target (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    safe_ratio      NUMERIC(5,2)  NOT NULL,
    invest_ratio    NUMERIC(5,2)  NOT NULL,
    band_pct_point  NUMERIC(4,2)  NOT NULL,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP     NOT NULL,
    CONSTRAINT uk_allocation_target_user_id UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS allocation_target_asset (
    id                    BIGSERIAL PRIMARY KEY,
    allocation_target_id  BIGINT        NOT NULL,
    asset_type            VARCHAR(20)   NOT NULL,
    target_ratio          NUMERIC(5,2)  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_allocation_target_asset_target_id
    ON allocation_target_asset (allocation_target_id);

-- 롤백:
-- DROP TABLE IF EXISTS allocation_target_asset;
-- DROP TABLE IF EXISTS allocation_target;
