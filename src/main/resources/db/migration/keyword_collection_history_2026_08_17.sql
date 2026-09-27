-- 키워드 수집 시도 이력 (#115) — 마지막 성공 · 연속 실패 · 배치 마지막 실행 표시용
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
--
-- 도입 배경: 기존에는 MAX(news.created_at) 으로 "마지막 수집"을 근사했는데,
-- 그건 마지막으로 기사가 저장된 시각이라 "수집은 성공했지만 새 기사가 없던" 경우를 놓친다.
-- 또한 수집 실패 자체를 남기는 곳이 없어 연속 실패 판정이 불가능했다.
--
-- 보존: KeywordCollectionHistoryCleanupScheduler 가 기본 90일 경과분을 매일 04:30 삭제한다.
-- 키워드 14개 × 매시 수집이면 하루 약 336행이 쌓인다.

CREATE TABLE IF NOT EXISTS keyword_collection_history (
    id            BIGSERIAL    PRIMARY KEY,
    keyword_id    BIGINT       NOT NULL,
    attempted_at  TIMESTAMP    NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    saved_count   INTEGER      NOT NULL DEFAULT 0,
    ignored_count INTEGER      NOT NULL DEFAULT 0,
    error_message VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_kch_keyword_attempted
    ON keyword_collection_history (keyword_id, attempted_at DESC);
CREATE INDEX IF NOT EXISTS idx_kch_attempted
    ON keyword_collection_history (attempted_at DESC);

-- 롤백
-- DROP TABLE IF EXISTS keyword_collection_history;
