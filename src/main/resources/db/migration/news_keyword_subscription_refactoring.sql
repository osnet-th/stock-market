-- ============================================================
-- 뉴스 키워드 구독 모델 리팩토링 마이그레이션 (PostgreSQL)
-- 실행 전 반드시 DB 백업 필요
-- ============================================================

-- ============================================================
-- 1. user_keyword 테이블 생성
-- ============================================================
CREATE TABLE IF NOT EXISTS user_keyword (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, keyword_id)
);

CREATE INDEX IF NOT EXISTS idx_user_keyword_user_active ON user_keyword (user_id, active);
CREATE INDEX IF NOT EXISTS idx_user_keyword_keyword ON user_keyword (keyword_id);

-- ============================================================
-- 2. 기존 keyword 데이터를 user_keyword로 이관
--    (keyword 테이블에 user_id가 아직 있는 상태에서 실행)
-- ============================================================
INSERT INTO user_keyword (user_id, keyword_id, active, created_at, updated_at)
SELECT user_id, id, active, created_at, created_at
FROM keyword
ON CONFLICT (user_id, keyword_id) DO NOTHING;

-- ============================================================
-- 3. keyword 테이블에서 동일 keyword+region 중복 처리
--    (공유 모델 전환 전 중복 키워드 병합)
-- ============================================================

-- 3-1. 중복 키워드의 user_keyword를 대표 키워드(가장 오래된 id)로 이관
UPDATE user_keyword
SET keyword_id = dup.keep_id
FROM keyword k
JOIN (
    SELECT keyword, region, MIN(id) AS keep_id
    FROM keyword
    GROUP BY keyword, region
) dup ON k.keyword = dup.keyword AND k.region = dup.region AND k.id != dup.keep_id
WHERE user_keyword.keyword_id = k.id
  AND NOT EXISTS (
    SELECT 1 FROM user_keyword uk2
    WHERE uk2.user_id = user_keyword.user_id AND uk2.keyword_id = dup.keep_id
  );

-- 3-2. 이관 후 남은 중복 user_keyword 삭제 (이미 대표 키워드 구독이 존재하는 경우)
DELETE FROM user_keyword
WHERE keyword_id IN (
    SELECT k.id FROM keyword k
    JOIN (
        SELECT keyword, region, MIN(id) AS keep_id
        FROM keyword
        GROUP BY keyword, region
    ) dup ON k.keyword = dup.keyword AND k.region = dup.region AND k.id != dup.keep_id
);

-- 3-3. news 테이블의 source_id도 대표 키워드로 이관 (purpose=KEYWORD인 것만)
UPDATE news
SET source_id = dup.keep_id
FROM keyword k
JOIN (
    SELECT keyword, region, MIN(id) AS keep_id
    FROM keyword
    GROUP BY keyword, region
) dup ON k.keyword = dup.keyword AND k.region = dup.region AND k.id != dup.keep_id
WHERE news.source_id = k.id AND news.purpose = 'KEYWORD';

-- 3-4. 중복 키워드 삭제 (대표 키워드만 남김)
DELETE FROM keyword
WHERE id IN (
    SELECT k.id FROM keyword k
    JOIN (
        SELECT keyword, region, MIN(id) AS keep_id
        FROM keyword
        GROUP BY keyword, region
    ) dup ON k.keyword = dup.keyword AND k.region = dup.region AND k.id != dup.keep_id
);

-- ============================================================
-- 4. news 테이블 변경: keyword_id 컬럼 추가 + 데이터 이관
-- ============================================================

-- 4-1. keyword_id 컬럼 추가
ALTER TABLE news ADD COLUMN IF NOT EXISTS keyword_id BIGINT;

-- 4-2. purpose=KEYWORD인 뉴스의 source_id를 keyword_id로 복사
UPDATE news SET keyword_id = source_id WHERE purpose = 'KEYWORD';

-- 4-3. purpose=PORTFOLIO인 뉴스 삭제
DELETE FROM news WHERE purpose = 'PORTFOLIO';

-- 4-4. purpose=STOCK인 뉴스 삭제
DELETE FROM news WHERE purpose = 'STOCK';

-- 4-5. keyword_id가 NULL인 뉴스 삭제
DELETE FROM news WHERE keyword_id IS NULL;

-- 4-6. keyword_id를 NOT NULL로 변경
ALTER TABLE news ALTER COLUMN keyword_id SET NOT NULL;

-- 4-7. 기존 컬럼 삭제
ALTER TABLE news DROP COLUMN IF EXISTS user_id;
ALTER TABLE news DROP COLUMN IF EXISTS purpose;
ALTER TABLE news DROP COLUMN IF EXISTS source_id;

-- 4-8. 기존 인덱스 삭제 + 새 인덱스 생성
DROP INDEX IF EXISTS idx_news_purpose_source_published;
CREATE INDEX IF NOT EXISTS idx_news_keyword_published ON news (keyword_id, published_at DESC);

-- ============================================================
-- 5. keyword 테이블에서 user_id, active 컬럼 제거 + UNIQUE 추가
-- ============================================================
ALTER TABLE keyword DROP COLUMN IF EXISTS user_id;
ALTER TABLE keyword DROP COLUMN IF EXISTS active;
ALTER TABLE keyword ADD CONSTRAINT uk_keyword_region UNIQUE (keyword, region);

-- ============================================================
-- 6. news_source 테이블 삭제
-- ============================================================
DROP TABLE IF EXISTS news_source;