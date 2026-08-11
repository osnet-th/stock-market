-- 매수 시점 적용 환율 컬럼 (#110) — 환차손익 산출용
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
--
-- 기존 행은 매수 시점 환율을 알 수 없으므로 NULL 로 남긴다.
-- 환차손익 집계는 fx_rate IS NULL 인 이력을 제외하고, 제외 건수를 응답의 fxUnknownCount 로 노출한다.

ALTER TABLE stock_purchase_history
    ADD COLUMN IF NOT EXISTS fx_rate NUMERIC(12,4);

-- 롤백
-- ALTER TABLE stock_purchase_history DROP COLUMN IF EXISTS fx_rate;
