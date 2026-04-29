-- news_event 테이블의 시장영향 enum 컬럼/인덱스 명칭 정리.
-- 자바 코드(@Column(name = "impact"))와 운영 스키마를 동기화한다.
--
-- 적용 순서: 본 SQL 적용 → 자바 배포 (역순일 경우 ddl-auto 가 impact 컬럼을 NULL 추가하여 데이터 분리 사고 발생).
--
-- 관련 설계: .claude/designs/newsjournal/news-event-category/news-event-category.md (Phase A)

ALTER TABLE news_event RENAME COLUMN category TO impact;
ALTER INDEX idx_news_event_user_category RENAME TO idx_news_event_user_impact;