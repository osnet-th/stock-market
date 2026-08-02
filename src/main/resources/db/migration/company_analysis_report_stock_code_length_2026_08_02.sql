-- #105 기업 분석 리포트 미국 주식 확장: stock_code 길이 6 → 20 (미국 영문 티커 허용)
-- ddl-auto: update가 길이 확대를 자동 반영하지만, 운영 환경 수동 적용 대비 백업 스크립트로 기록한다.
ALTER TABLE company_analysis_report
    ALTER COLUMN stock_code TYPE VARCHAR(20);
