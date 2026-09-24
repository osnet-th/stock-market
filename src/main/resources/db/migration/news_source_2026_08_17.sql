-- 뉴스 언론사 컬럼 (#115) — 키워드 뉴스 워크스페이스 기사 카드 언론사 표기용
-- 권위는 Entity 어노테이션 (ddl-auto: update 적용). 본 파일은 운영 DBA 수동 적용/롤백 백업용.
--
-- 이 컬럼 도입 전에 수집된 기존 행은 언론사를 알 수 없으므로 NULL 로 남긴다.
-- 백필하지 않는다 — 프론트가 source 가 비어 있으면 original_url 도메인으로 폴백해 표시한다.
-- 수집 어댑터는 GNews/NewsAPI 는 응답의 source.name 을, 네이버는 응답에 언론사가 없어
-- original_url 도메인 파생값을 채운다.

ALTER TABLE news
    ADD COLUMN IF NOT EXISTS source VARCHAR(100);

-- 롤백
-- ALTER TABLE news DROP COLUMN IF EXISTS source;
