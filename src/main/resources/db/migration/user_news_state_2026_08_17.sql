-- 사용자별 뉴스 읽음/저장 상태 (#115) — 통합 화면의 안 읽음 표시 · ☆저장 · `안 읽은 것만` 필터
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
--
-- 뉴스는 키워드에 딸린 공유 데이터라 읽음 여부를 news 행에 둘 수 없다(같은 기사를 여러 사용자가 본다).
-- 행은 상호작용이 있을 때만 생성한다 — 행이 없으면 "안 읽음 · 미저장"으로 해석한다.
-- 컬럼명이 is_read / is_saved 인 이유: read 는 SQL 키워드와 겹칠 소지가 있다.

CREATE TABLE IF NOT EXISTS user_news_state (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    news_id    BIGINT    NOT NULL,
    is_read    BOOLEAN   NOT NULL DEFAULT FALSE,
    is_saved   BOOLEAN   NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE user_news_state
    ADD CONSTRAINT uk_user_news_state UNIQUE (user_id, news_id);

CREATE INDEX IF NOT EXISTS idx_uns_user_read  ON user_news_state (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_uns_user_saved ON user_news_state (user_id, is_saved);

-- 롤백
-- DROP TABLE IF EXISTS user_news_state;
