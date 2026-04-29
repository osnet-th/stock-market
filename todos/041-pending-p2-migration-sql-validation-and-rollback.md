---
status: pending
priority: p2
issue_id: 041
tags: [code-review, migration, deployment, newsjournal]
dependencies: []
---

# 마이그레이션 SQL 3종 — 사전/사후 검증 SQL + 롤백 절차 주석 보강

## Problem Statement

3개 마이그레이션 SQL (`news_event_impact_rename.sql`, `news_event_category_not_null.sql`, `news_event_category_seed_economy.sql`) 모두 운영 적용 시 휴먼 에러 방지를 위한 사전/사후 검증 SQL 과 롤백 절차 주석이 부족. 1차 사고 (Phase A DDL 미적용 → impact 컬럼 미존재) 가 이미 발생했으므로 강화 필요.

## Findings

- 위치 1: `src/main/resources/db/migration/news_event_impact_rename.sql:8-9` (RENAME 검증 SQL 없음)
- 위치 2: `src/main/resources/db/migration/news_event_category_not_null.sql:5` (NOT NULL 강화 사전 검증 부족, orphan 검증 없음)
- 위치 3: `src/main/resources/db/migration/news_event_category_seed_economy.sql:14` (적용 순서 / Bootstrap 충돌 / "기타" 잔존 미명시)

확인 보고:
- data-migration-expert P1 (#1, #2, #3) + P2 (#1)
- deployment-verification-agent (Go/No-Go 체크리스트 산출물 별도)

## Proposed Solutions

### Option A — SQL 파일 주석 보강만 (권장)
각 SQL 에 다음 추가:

**1) news_event_impact_rename.sql**:
```sql
-- 적용 후 검증:
--   SELECT COUNT(*) FROM news_event WHERE impact IS NULL;          -- expect 0
--   SELECT impact, COUNT(*) FROM news_event GROUP BY impact;       -- 분포 보존 확인
-- 롤백:
--   ALTER INDEX idx_news_event_user_impact RENAME TO idx_news_event_user_category;
--   ALTER TABLE news_event RENAME COLUMN impact TO category;
```

**2) news_event_category_not_null.sql**:
```sql
-- 적용 전 사전 점검 (필수):
--   SELECT COUNT(*) FROM news_event WHERE category_id IS NULL;     -- expect 0
--   SELECT COUNT(*) FROM news_event e
--     LEFT JOIN news_event_category c ON c.id = e.category_id
--    WHERE c.id IS NULL;                                            -- expect 0 (orphan FK 검증)
-- 롤백:
--   ALTER TABLE news_event ALTER COLUMN category_id DROP NOT NULL;
```

**3) news_event_category_seed_economy.sql**:
```sql
-- 적용 순서 (필수):
--   1) Phase B 자바 배포 → 부팅 완료 (ddl-auto 가 news_event_category 테이블 + uq 제약 생성)
--   2) Bootstrap 자동으로 "기타" 카테고리 생성 + NULL 사건 backfill (모든 사건이 "기타" 매핑)
--   3) 본 Seed SQL 적용 → 사건 전체가 "경제" 로 재매핑, "기타" 카테고리는 빈 채 잔존
--      ⚠ Bootstrap 비활성화 또는 삭제 결정 시 (todo 032/037 참조) 본 절차 변경
-- 적용 후 검증:
--   SELECT c.name, COUNT(e.id) FROM news_event_category c
--     LEFT JOIN news_event e ON e.category_id = c.id GROUP BY c.name;
-- (선택) 빈 "기타" 정리:
--   DELETE FROM news_event_category
--    WHERE name = '기타'
--      AND NOT EXISTS (SELECT 1 FROM news_event WHERE category_id = news_event_category.id);
```

- 장점: 코드 변경 없음, 운영자 휴먼 에러 방지.
- 단점: 주석 신뢰 의존.

### Option B — 별도 운영 가이드 문서 생성
- `docs/operations/newsjournal-deploy-guide.md` 신규.
- 장점: 한 곳에 정리.
- 단점: SQL 파일과 문서 동기화 부담.

## Recommended Action

(Option A 권장 — 즉시 적용 가능, 변경 비용 최소)

## Technical Details

- 영향 파일: 마이그레이션 SQL 3종
- 운영 적용 순서는 `.claude/designs/newsjournal/news-event-category/news-event-category.md` 의 작업 순서와 일치 유지

## Acceptance Criteria

- [ ] 3개 SQL 파일에 사전/사후 검증 SQL 주석
- [ ] 3개 SQL 파일에 롤백 SQL 주석
- [ ] Seed SQL 에 Bootstrap 충돌 명시

## Work Log

- 2026-04-29: ce-review 발견 (data-migration P1×3, P2×1)

## Resources

- `src/main/resources/db/migration/news_event_impact_rename.sql`
- `src/main/resources/db/migration/news_event_category_not_null.sql`
- `src/main/resources/db/migration/news_event_category_seed_economy.sql`
- `.claude/designs/newsjournal/news-event-category/news-event-category.md`