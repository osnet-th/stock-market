-- glossary_term 구조화 필드 + 함께 볼 용어 (마스터-디테일 리디자인)
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
-- 참고: docs/plans/2026-08-21-001-feat-glossary-redesign-plan.md
-- 선행: glossary_tables_2026_05_18.sql (#43)

-- 구조화 콘텐츠 컬럼 (전부 선택 — 기존 row 는 NULL 유지)
-- definition 컬럼은 '풀이' 로 재해석해 그대로 사용 (rename 없음 — 기존 데이터 보존)
ALTER TABLE glossary_term ADD COLUMN IF NOT EXISTS abbreviation VARCHAR(200);
ALTER TABLE glossary_term ADD COLUMN IF NOT EXISTS one_line     VARCHAR(300);
ALTER TABLE glossary_term ADD COLUMN IF NOT EXISTS scale_note   TEXT;
ALTER TABLE glossary_term ADD COLUMN IF NOT EXISTS example      TEXT;
ALTER TABLE glossary_term ADD COLUMN IF NOT EXISTS takeaway     TEXT;

-- 함께 볼 용어 (@ElementCollection + @OrderColumn(position) — Hibernate 생성 스키마와 동일 형태)
-- FK 미사용 (ID 참조 컨벤션). 소유권 검증은 application 계층.
-- 용어 삭제 시 application 계층이 term_id/related_term_id 양방향 정리.
CREATE TABLE IF NOT EXISTS glossary_term_related (
    term_id          BIGINT  NOT NULL,
    related_term_id  BIGINT  NOT NULL,
    position         INTEGER NOT NULL,
    PRIMARY KEY (term_id, position)
);
CREATE INDEX IF NOT EXISTS idx_glossary_term_related_target
    ON glossary_term_related (related_term_id);

-- 롤백 (참고용)
-- DROP TABLE IF EXISTS glossary_term_related;
-- ALTER TABLE glossary_term DROP COLUMN IF EXISTS takeaway;
-- ALTER TABLE glossary_term DROP COLUMN IF EXISTS example;
-- ALTER TABLE glossary_term DROP COLUMN IF EXISTS scale_note;
-- ALTER TABLE glossary_term DROP COLUMN IF EXISTS one_line;
-- ALTER TABLE glossary_term DROP COLUMN IF EXISTS abbreviation;
