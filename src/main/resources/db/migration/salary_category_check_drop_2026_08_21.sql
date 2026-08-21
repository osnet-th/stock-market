-- #116 월급 사용 비율 — 카테고리 enum → 사용자 정의 code 전환
--
-- Hibernate가 @Enumerated(EnumType.STRING) 매핑으로 테이블을 생성하면서
-- category 컬럼에 enum 8종 CHECK 제약을 만들어 두었다.
-- 커스텀 카테고리 code('U…')는 이 제약에 걸려 INSERT가 실패하므로,
-- 본 브랜치 배포 시(앱 기동 전후 무관) 반드시 1회 실행해야 한다.
-- ddl-auto=update는 기존 CHECK 제약을 제거하지 않는다.
--
-- 참고: spending_item 테이블은 배포 시점에 처음 생성되는 환경(운영)에서는
-- String 매핑으로 생성되어 제약이 없을 수 있다 — IF EXISTS로 안전하게 처리.

ALTER TABLE spending_config DROP CONSTRAINT IF EXISTS spending_config_category_check;
ALTER TABLE spending_item DROP CONSTRAINT IF EXISTS spending_item_category_check;
