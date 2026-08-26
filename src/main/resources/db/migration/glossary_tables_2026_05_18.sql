-- glossary 도메인 테이블 (개인 용어 사전)
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
-- 참고: docs/plans/2026-05-17-001-feat-personal-glossary-plan.md
-- 이슈: #43

-- 카테고리 (사용자 정의)
CREATE TABLE IF NOT EXISTS glossary_category (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT uq_glossary_category_user_name UNIQUE (user_id, name)
);
CREATE INDEX IF NOT EXISTS idx_glossary_category_user_id
    ON glossary_category (user_id);

-- 용어 (개인 사전 본체)
-- category_id NULL = "미분류" (가상 표현)
-- FK 미사용 (ID 참조 컨벤션). 카테고리 삭제 시 application 계층이 category_id = NULL 로 일괄 update.
CREATE TABLE IF NOT EXISTS glossary_term (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL,
    name         VARCHAR(200)  NOT NULL,
    definition   TEXT,
    category_id  BIGINT        NULL,
    created_at   TIMESTAMP     NOT NULL,
    updated_at   TIMESTAMP     NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_glossary_term_user_id
    ON glossary_term (user_id);
CREATE INDEX IF NOT EXISTS idx_glossary_term_user_category
    ON glossary_term (user_id, category_id);