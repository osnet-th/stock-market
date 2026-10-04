-- 포트폴리오 항목 금융기관 컬럼 — 자산을 보관하는 은행·증권사 등의 이름
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
--
-- 기존 행은 금융기관을 알 수 없으므로 NULL 로 남기고, 화면에서 "미지정"으로 묶는다.

ALTER TABLE portfolio_item
    ADD COLUMN IF NOT EXISTS institution VARCHAR(50);

-- 롤백 (입력한 금융기관 값이 함께 사라진다)
-- ALTER TABLE portfolio_item DROP COLUMN IF EXISTS institution;
