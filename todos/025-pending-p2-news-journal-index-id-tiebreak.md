---
status: pending
priority: p2
issue_id: 025
tags: [code-review, performance, index, newsjournal]
dependencies: []
---

# 인덱스에 `id` 미포함 → 동일일자 다발 시 sort key 추가 정렬

## Problem Statement

`news_event` 의 인덱스 두 개가 `(user_id, occurred_date DESC)` 와 `(user_id, category, occurred_date DESC)` 로 정의되어 있는데, JPQL 정렬은 `ORDER BY occurredDate DESC, id DESC` 로 `id` 를 tiebreak 로 사용한다. PostgreSQL 의 ROWID 는 ordering key 가 아니므로, 한 사용자가 같은 일자에 다수 사건을 쌓는 경우 그룹 내 추가 sort 가 발생한다. 일반 사용 패턴(하루 수건)에서는 무시 가능하나, deep page scan 에서 ms 단위 비용 증가.

## Findings

- Entity 인덱스 정의: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/infrastructure/persistence/NewsEventEntity.java:27-35`
- JPQL 정렬: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/infrastructure/persistence/NewsEventJpaRepository.java:24`

## Proposed Solutions

### Option A — 인덱스 컬럼에 `id DESC` 추가 (Recommended)
- `idx_news_event_user_date` → `(user_id, occurred_date DESC, id DESC)`
- `idx_news_event_user_category` → `(user_id, category, occurred_date DESC, id DESC)`
- ddl-auto=update 는 기존 인덱스 갱신 안 하므로 운영 DB 는 수동 `CREATE INDEX CONCURRENTLY` + 기존 인덱스 drop 필요.

### Option B — 정렬을 `occurred_date DESC, id ASC` 로 변경
- id ASC tiebreak 도 인덱스에서 동일 sort 비용 발생 (역방향). 본질 해결 아님.

### Option C — 현 상태 유지
- 1인 1일 다수 등록 케이스가 드물다고 판단되면 무시 가능. P3 로 다운그레이드.

## Recommended Action

A 적용. 분석/설계 문서에 운영 DB 수동 DDL 절차 명시.

## Technical Details

- Entity `@Table(indexes=...)` 만 변경하면 신규 환경은 자동 반영.
- 운영 DB:
  ```sql
  CREATE INDEX CONCURRENTLY idx_news_event_user_date_v2
    ON news_event (user_id, occurred_date DESC, id DESC);
  DROP INDEX CONCURRENTLY idx_news_event_user_date;
  ALTER INDEX idx_news_event_user_date_v2 RENAME TO idx_news_event_user_date;
  ```
  (카테고리 인덱스 동일 패턴)

## Acceptance Criteria

- [ ] EXPLAIN 결과에 `Sort` 노드 미발생 (top-N heap 만)
- [ ] 운영 DDL 절차 문서화

## Work Log

- 2026-04-28: 발견 (ce-review 성능 P2-1)

## Resources

- performance-oracle 보고 P2-1
- PostgreSQL docs: index-only scan ordering