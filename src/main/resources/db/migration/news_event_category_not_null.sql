-- news_event.category_id 운영 강화 + 인덱스 정리.
-- 적용 순서:
--   1) Phase B 자바 배포 (news_event_category 테이블 / category_id 컬럼 / 신규 인덱스 / Bootstrap 실행)
--   2) Bootstrap 이 모든 NULL 행을 사용자별 "기타" 카테고리로 backfill 했는지 확인
--      ex) SELECT COUNT(*) FROM news_event WHERE category_id IS NULL;  -- expect 0
--   3) 본 SQL 적용
--   4) 다음 자바 배포에서 NewsEventEntity.category_id 의 nullable=false 강화
--
-- 관련 설계: .claude/designs/newsjournal/news-event-category/news-event-category.md (Phase B-6)

ALTER TABLE news_event ALTER COLUMN category_id SET NOT NULL;