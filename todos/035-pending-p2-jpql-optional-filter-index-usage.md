---
status: pending
priority: p2
issue_id: 035
tags: [code-review, performance, postgresql, newsjournal]
dependencies: []
---

# JPQL 옵셔널 필터 `:p IS NULL OR col = :p` 가 PostgreSQL 인덱스 사용을 방해할 가능성

## Problem Statement

`NewsEventJpaRepository.findList` 가 `impact` / `categoryId` 둘 다 nullable 옵셔널 필터로 받음. PostgreSQL planner 가 prepared statement 의 generic plan 으로 굳히면, 실제 값이 들어와도 신규 인덱스 (`idx_news_event_user_impact`, `idx_news_event_user_category_date`) 를 못 타고 `idx_news_event_user_date` + 테이블 필터로 폴백할 가능성. 신규 인덱스 INSERT 비용은 100% 부담하지만 SELECT 이득은 0% 가능.

## Findings

- 위치: `src/main/java/.../newsjournal/infrastructure/persistence/NewsEventJpaRepository.java:18-50`

```sql
WHERE e.userId = :userId
  AND (:impact IS NULL OR e.impact = :impact)
  AND (:categoryId IS NULL OR e.categoryId = :categoryId)
  ...
```

영향 추정:
- 사용자당 사건 ≤ 수천: planner 가 어떤 인덱스를 타든 차이 미미 (수 ms).
- 사용자당 사건 10만+: 인덱스 미사용 시 user_id+date 인덱스로 좁힌 뒤 추가 필터 적용 → 페이지 응답 50–200ms 가능.

확인 보고:
- performance-oracle P2 (#2)

## Proposed Solutions

### Option A — EXPLAIN 측정 후 결정 (권장)
- `EXPLAIN (ANALYZE, BUFFERS)` 로 4가지 조합 (impact, categoryId 각각 NULL/value) 인덱스 사용 확인.
- 인덱스 미사용 확인 시 Option B 또는 C 적용.
- 장점: 데이터 기반 결정.
- 단점: 측정 필요.

### Option B — `categoryId` 필터만 별도 메서드로 분리
- `findListByCategory(userId, categoryId, ...)` / `findList(userId, ...)` 분기.
- 장점: 옵셔널 절 제거, 인덱스 보장.
- 단점: 메서드 중복 일부 발생.

### Option C — QueryDSL 동적 쿼리로 마이그레이션
- 프로젝트가 QueryDSL 채택되어 있으나 newsjournal 미사용.
- 장점: 필터별 다른 SQL 빌드.
- 단점: 도입 비용.

### Option D — 현행 유지 + 모니터링
- 사용자당 사건 수 모니터링, 임계치 도달 시 적용.
- 장점: 즉시 변경 없음.
- 단점: 미래 부담.

## Recommended Action

(Option A 로 측정 후 결정)

## Technical Details

- 영향 파일: `NewsEventJpaRepository.java`, (Option B/C 채택 시) `NewsEventRepositoryImpl.java`
- 측정 SQL 예:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM news_event WHERE user_id = ? AND impact = 'GOOD'
ORDER BY occurred_date DESC LIMIT 20;

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM news_event WHERE user_id = ? AND category_id = ?
ORDER BY occurred_date DESC, id DESC LIMIT 20;
```

## Acceptance Criteria

- [ ] EXPLAIN 결과 기록
- [ ] 인덱스 사용 안 되면 분리 또는 동적 쿼리 적용

## Work Log

- 2026-04-29: ce-review 발견 (performance P2)

## Resources

- `src/main/java/.../newsjournal/infrastructure/persistence/NewsEventJpaRepository.java`
- `src/main/java/.../newsjournal/infrastructure/persistence/NewsEventEntity.java` (인덱스 정의)