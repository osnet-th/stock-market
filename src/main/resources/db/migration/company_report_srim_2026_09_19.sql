-- Entity가 권위이며 ddl-auto:update 또는 아래 수동 적용으로 nullable 컬럼 추가.
ALTER TABLE company_analysis_report ADD COLUMN IF NOT EXISTS srim jsonb;
-- 롤백: 이전 앱은 추가 컬럼을 무시한다. 데이터를 보존하려면 컬럼을 유지한다.
-- DROP COLUMN srim은 저장된 S-RIM 근거와 결과를 삭제하므로 백업 및 별도 승인 없이 실행하지 않는다.
