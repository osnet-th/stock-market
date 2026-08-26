-- #116 월급 사용 비율 — 카테고리 enum → 사용자 정의 code 전환
--
-- Hibernate가 @Enumerated(EnumType.STRING) 매핑으로 테이블을 생성하면서
-- category 컬럼에 enum 8종 CHECK 제약을 만들어 두었다.
-- 커스텀 카테고리 code('U…')는 이 제약에 걸려 INSERT가 실패하므로,
-- 본 브랜치 배포 시(앱 기동 전후 무관) 반드시 1회 실행해야 한다.
-- ddl-auto=update는 기존 CHECK 제약을 제거하지 않는다.
--
-- 참고: spending_item 테이블은 배포 시점에 처음 생성되는 환경(운영)에서는
-- 앱 첫 기동 전엔 존재하지 않고, 생성될 때도 String 매핑이라 제약이 없다.
-- 따라서 테이블 부재까지 방어하도록 ALTER TABLE IF EXISTS를 사용한다
-- (DROP CONSTRAINT IF EXISTS만으로는 테이블이 없으면 에러).

ALTER TABLE IF EXISTS spending_config DROP CONSTRAINT IF EXISTS spending_config_category_check;
ALTER TABLE IF EXISTS spending_item DROP CONSTRAINT IF EXISTS spending_item_category_check;
