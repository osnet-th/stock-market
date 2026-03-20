# Flyway 마이그레이션 예시

```sql
-- 1. user_keyword 테이블 생성
CREATE TABLE user_keyword (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_keyword (user_id, keyword_id),
    KEY idx_user_keyword_user_active (user_id, active),
    KEY idx_user_keyword_keyword (keyword_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 기존 keyword 데이터를 user_keyword로 이관
INSERT INTO user_keyword (user_id, keyword_id, active, created_at, updated_at)
SELECT user_id, id, active, created_at, created_at
FROM keyword;

-- 3. 기존 news의 source_id를 keyword_id로 변환 (purpose=KEYWORD인 것만)
-- news.source_id가 keyword.id를 참조하므로 그대로 사용 가능
ALTER TABLE news ADD COLUMN keyword_id BIGINT NULL;
UPDATE news SET keyword_id = source_id WHERE purpose = 'KEYWORD';

-- 4. purpose=PORTFOLIO인 뉴스 처리 (포트폴리오 뉴스는 keyword로 매핑 불가 → 삭제 또는 별도 처리)
DELETE FROM news_source WHERE purpose = 'PORTFOLIO';
DELETE FROM news WHERE purpose = 'PORTFOLIO';

-- 5. news 테이블 컬럼 정리
ALTER TABLE news MODIFY keyword_id BIGINT NOT NULL;
ALTER TABLE news DROP COLUMN user_id;
ALTER TABLE news DROP COLUMN purpose;
ALTER TABLE news DROP COLUMN source_id;
ALTER TABLE news DROP INDEX idx_news_purpose_source_published;
CREATE INDEX idx_news_keyword_published ON news (keyword_id, published_at DESC);

-- 6. keyword 테이블 변경
ALTER TABLE keyword DROP COLUMN user_id;
ALTER TABLE keyword ADD UNIQUE KEY uk_keyword_region (keyword, region);

-- 7. news_source 테이블 삭제
DROP TABLE IF EXISTS news_source;
```

## 주의사항

- keyword 테이블의 동일 keyword+region 조합이 여러 user_id로 존재할 경우, 하나만 남기고 나머지는 user_keyword로만 매핑해야 함
- 실행 전 반드시 백업 필요
- MariaDB 기준 문법 확인 필요 (ON CONFLICT → ON DUPLICATE KEY UPDATE)