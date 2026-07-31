-- 금 항목 보유 중량 컬럼 추가 (#102, 시세 평가용)
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.

ALTER TABLE gold_detail
    ADD COLUMN IF NOT EXISTS quantity_grams NUMERIC(12,3);

-- 롤백:
-- ALTER TABLE gold_detail DROP COLUMN IF EXISTS quantity_grams;
