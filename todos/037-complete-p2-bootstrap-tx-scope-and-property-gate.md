---
status: complete
priority: p2
issue_id: 037
tags: [code-review, security, performance, transactions, newsjournal]
dependencies: [032, 033]
---

# `NewsJournalBootstrap` — 단일 트랜잭션 long-running + 매 부팅 풀스캔 + 운영 비활성화 토글 부재

## Problem Statement

Bootstrap 이 모든 사용자에 대한 backfill 을 단일 클래스-수준 `@Transactional` 에서 직렬 처리. 1인 환경에선 무관하나 사용자 N 명에 대해 트랜잭션 hold time 비례 증가 + lock 점유. 또한 매 부팅마다 `findDistinctUserIdsWithNullCategoryId` 풀스캔이 발생하며 (NULL 사건 0건이어도 stats/scan 비용), 1회성 코드인데 영구 부착되어 있고 운영 NOT NULL 강화 후에도 비활성화 옵션이 없음.

## Findings

- 위치 1: `src/main/java/.../newsjournal/infrastructure/bootstrap/NewsJournalBootstrap.java:36-51` (단일 `@Transactional` + per-user 루프)
- 위치 2: 동일 파일 38행 (`findDistinctUserIdsWithNullCategoryId` 풀스캔)
- 위치 3: 설계 문서 `news-event-category.md:175-176` (제거 계획 없음)

확인 보고:
- security-sentinel P2 (#3 부팅 보정 트랜잭션)
- architecture-strategist P2 (영구 잔존)
- performance-oracle P3 (#4 부팅 비용)

## Proposed Solutions

### Option A — 사용자별 트랜잭션 분리 + Property gate (권장)
1. 사용자별 backfill 을 별도 빈 `NewsJournalBackfillExecutor` + `@Transactional(propagation=REQUIRES_NEW)` 분리.
2. `application.yml` 에 `newsjournal.bootstrap.enabled: true` 추가, `@ConditionalOnProperty(value="newsjournal.bootstrap.enabled", havingValue="true", matchIfMissing=true)` 적용.
3. 운영 NOT NULL 강화 후 `false` 전환 → 코드 잔존하되 비용 0.
- 장점: 사용자 단위 격리 + 운영 비활성화.
- 단점: 빈 1개 추가.

### Option B — `category_id IS NULL` partial index 추가 + Bootstrap 유지
- `CREATE INDEX idx_news_event_null_category_user ON news_event(user_id) WHERE category_id IS NULL;`
- 장점: 풀스캔 → 인덱스 스캔.
- 단점: NOT NULL 강화 후 인덱스 의미 없음 → 또 정리해야 함.

### Option C — Bootstrap 자체 삭제 (032 채택 시)
- 032 의 Option A (Bootstrap 삭제 + Seed SQL 만 사용) 결정 시 본 항목 자동 해소.
- 장점: 가장 단순.
- 단점: 운영자 수동 SQL 의존.

## Recommended Action

**자동 해소** — 032 가 Option A (Bootstrap 클래스 삭제) 로 결정되어 본 항목의 long-running TX / 풀스캔 / property gate 모두 무관해짐. NULL 사건 backfill 은 운영자가 수동 SQL 로 1회 실행 (`news_event_category_seed_economy.sql`) 후 NOT NULL 강화로 마무리.

## Technical Details

- 영향 파일: `NewsJournalBootstrap.java`, (Option A) 신규 `NewsJournalBackfillExecutor.java`, `application.yml`

## Acceptance Criteria

- [ ] 사용자 단위 트랜잭션 격리 (Bootstrap 유지 시)
- [ ] 운영 환경에서 비활성화 토글 가능
- [ ] 부팅 시 풀스캔 비용 측정/완화

## Work Log

- 2026-04-29: ce-review 발견 (security P2, architecture P2, performance P3)
- 2026-04-29: 032 적용으로 자동 해소. `NewsJournalBootstrap` 삭제됨.

## Resources

- `src/main/java/.../newsjournal/infrastructure/bootstrap/NewsJournalBootstrap.java`
- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`