-- 현금성 자산 자동납입 처리 방식 컬럼 — NOTIFY(알림 받고 직접 납입) / AUTO(납입일에 자동 납입)
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
--
-- 기존 행은 NULL 로 남기고 알림 확인(NOTIFY)으로 읽는다.

ALTER TABLE cash_detail
    ADD COLUMN IF NOT EXISTS deposit_mode VARCHAR(20);

-- 롤백 (처리 방식 설정이 사라진다. 이미 생긴 자동 납입 이력은 일반 납입 이력으로 남는다)
-- ALTER TABLE cash_detail DROP COLUMN IF EXISTS deposit_mode;
