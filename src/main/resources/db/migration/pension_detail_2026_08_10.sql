-- 연금 자산군(PENSION) 상세 테이블 (#110)
--
-- ⚠ 이 파일은 백업용이 아니라 **운영 적용 필수**다.
--    portfolio_item.asset_type 에 CHECK 제약이 걸려 있는데 ddl-auto: update 는
--    기존 CHECK 제약을 갱신하지 않는다. 아래 ALTER 없이 배포하면
--    연금 항목 등록이 23514(check constraint violation) → 409 CONFLICT 로 실패한다.
--    (로컬 실DB 검증에서 확인, #110 validation)
--
-- portfolio_item 은 JOINED 상속 + asset_type 디스크리미네이터 구조라
-- 자식 테이블의 PK 가 곧 portfolio_item.id 다 (fund_detail / cash_detail 과 동일).
-- pension_detail 테이블 자체는 ddl-auto 가 자동 생성하지만, 멱등하게 두어 수동 적용도 가능하게 한다.
-- 평가액은 시세 연동 없이 사용자가 직접 갱신하므로 NULL 허용이며,
-- NULL 이면 평가 시 portfolio_item.invested_amount 로 대체한다.

-- [필수] asset_type CHECK 제약에 PENSION 추가
ALTER TABLE portfolio_item DROP CONSTRAINT IF EXISTS portfolio_item_asset_type_check;
ALTER TABLE portfolio_item ADD CONSTRAINT portfolio_item_asset_type_check
    CHECK (asset_type IN ('BOND', 'CASH', 'COMMODITY', 'CRYPTO', 'FUND',
                          'GOLD', 'OTHER', 'PENSION', 'REAL_ESTATE', 'STOCK'));

CREATE TABLE IF NOT EXISTS pension_detail (
    id                      BIGINT         PRIMARY KEY,
    sub_type                VARCHAR(20),
    provider                VARCHAR(100),
    evaluated_amount        NUMERIC(18,2),
    monthly_deposit_amount  NUMERIC(18,2),
    deposit_day             INTEGER,
    CONSTRAINT fk_pension_detail_item FOREIGN KEY (id) REFERENCES portfolio_item (id)
);

-- 롤백
-- DROP TABLE IF EXISTS pension_detail;
