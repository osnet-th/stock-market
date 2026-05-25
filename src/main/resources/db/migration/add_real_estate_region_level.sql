-- real_estate_region에 level 컬럼 추가 (광역시도/시군구 discriminator)
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
-- 참고: docs/plans/2026-05-24-001-feat-reb-adapter-redesign-plan.md
-- 이슈: #58
--
-- 적용 순서 (반드시 코드 머지 전 선행):
--   1) 본 SQL 적용
--   2) SELECT level, COUNT(*) FROM real_estate_region GROUP BY level; → SIGUNGU=56 검증
--   3) Entity/JPA hook 코드 머지·배포
-- 순서가 어긋나면 @PostLoad가 level=null mismatch로 부팅 즉시 fail.

-- 1) 컬럼 추가 (nullable로 시작 → backfill 후 NOT NULL)
ALTER TABLE real_estate_region
    ADD COLUMN IF NOT EXISTS level VARCHAR(16);

-- 2) 기존 row backfill (region_code 길이 기반 derive)
-- 비정상 길이(2/5 외)는 NULL로 남겨 다음 NOT NULL 적용에서 fail-fast → 데이터 audit 필요
UPDATE real_estate_region
SET level = CASE
    WHEN LENGTH(region_code) = 2 THEN 'SIDO'
    WHEN LENGTH(region_code) = 5 THEN 'SIGUNGU'
    ELSE NULL
END
WHERE level IS NULL;

-- 2-1) 비정상 길이 row audit (NOT NULL 적용 전 확인)
-- 결과 0건이어야 함. 비정상 row 발견 시 별도 데이터 수정 후 재실행.
SELECT region_code, LENGTH(region_code) AS code_len, COUNT(*) AS count
FROM real_estate_region
WHERE level IS NULL
GROUP BY region_code, LENGTH(region_code);

-- 3) NOT NULL 적용 (위 audit 결과 0건 확인 후 실행)
ALTER TABLE real_estate_region
    ALTER COLUMN level SET NOT NULL;