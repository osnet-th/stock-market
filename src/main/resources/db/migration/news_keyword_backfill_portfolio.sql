-- ============================================================
-- 기존 newsEnabled=true 포트폴리오 항목에 대한 keyword + user_keyword 생성
-- 뉴스 키워드 구독 모델 리팩토링 이후 보정 스크립트 (PostgreSQL)
-- ============================================================

-- 1. newsEnabled=true인 포트폴리오 항목의 종목명으로 keyword 생성 (중복 무시)
INSERT INTO keyword (keyword, region, created_at)
SELECT DISTINCT p.item_name, p.region, NOW()
FROM portfolio_item p
WHERE p.news_enabled = true
ON CONFLICT (keyword, region) DO NOTHING;

-- 2. user_keyword 구독 관계 생성
INSERT INTO user_keyword (user_id, keyword_id, active, created_at, updated_at)
SELECT p.user_id, k.id, true, NOW(), NOW()
FROM portfolio_item p
JOIN keyword k ON k.keyword = p.item_name AND k.region = p.region
WHERE p.news_enabled = true
ON CONFLICT (user_id, keyword_id) DO NOTHING;