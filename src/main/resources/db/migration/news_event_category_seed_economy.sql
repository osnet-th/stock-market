-- 사용자별 '경제' 카테고리 추가 + 현재 등록된 모든 사건을 '경제' 로 매핑.
--
-- 적용 시점: Phase B 자바 배포 후 (ddl-auto 가 news_event_category 테이블/unique 제약을 생성한 뒤).
-- 1인 환경이지만 distinct user_id 로 안전하게 다중 사용자 대응.
--
-- 멱등(idempotent): 재실행해도 카테고리는 ON CONFLICT 로 무시되고, UPDATE 는 같은 결과를 만든다.
--
-- 관련 설계: .claude/designs/newsjournal/news-event-category/news-event-category.md (Phase B-6)

-- 1) 사건이 1건이라도 있는 사용자별로 '경제' 카테고리 보장
INSERT INTO news_event_category (user_id, name, created_at)
SELECT DISTINCT e.user_id, '경제', NOW()
  FROM news_event e
ON CONFLICT (user_id, name) DO NOTHING;

-- 2) 모든 사건을 사용자별 '경제' 카테고리 id 로 매핑
UPDATE news_event AS e
   SET category_id = c.id
  FROM news_event_category AS c
 WHERE c.user_id = e.user_id
   AND c.name = '경제';

-- 검증 쿼리 (수동):
--   SELECT COUNT(*) FROM news_event WHERE category_id IS NULL;  -- expect 0
--   SELECT c.name, COUNT(e.id)
--     FROM news_event_category c
--     LEFT JOIN news_event e ON e.category_id = c.id
--    GROUP BY c.name;