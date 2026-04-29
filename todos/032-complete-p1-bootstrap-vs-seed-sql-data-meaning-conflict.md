---
status: complete
priority: p1
issue_id: 032
tags: [code-review, simplicity, migration, newsjournal]
dependencies: []
---

# Bootstrap "기타" backfill 과 Seed SQL "경제" 매핑이 데이터 의미 충돌 — 단일 경로로 통일 필요

## Problem Statement

부팅 시 `NewsJournalBootstrap` 이 NULL 사건을 사용자별 "기타" 카테고리로 자동 backfill 하고, 운영자가 별도로 `news_event_category_seed_economy.sql` 을 실행하면 모든 사건이 "경제" 로 재매핑됨. 두 경로가 서로를 덮어쓰며, 1차 적용 결과 빈 "기타" 카테고리가 영구 잔존하고 다음 부팅에서도 정리되지 않음.

## Findings

- 위치 1: `src/main/java/.../newsjournal/infrastructure/bootstrap/NewsJournalBootstrap.java:31` (`DEFAULT_CATEGORY_NAME = "기타"`)
- 위치 2: `src/main/resources/db/migration/news_event_category_seed_economy.sql:13` (`'경제'`)
- 시나리오: Phase B 자바 배포 → 부팅 → Bootstrap 이 "기타" 자동 생성 + backfill → 운영자가 Seed SQL 적용 → 모든 사건이 "경제" 로 이동, "기타" 카테고리는 참조 0개로 잔존.
- UX 영향: `GET /api/news-journal/categories` 응답에 빈 "기타" 카테고리 노출 → 사용자 혼란.
- 데이터 영향: 다음 부팅에 새 NULL 사건이 추가되면 다시 "기타" 로 들어감 (의도와 다름).

확인 보고:
- code-simplicity-reviewer P1 (#1)
- data-migration-expert P1 (Bootstrap 와 Seed SQL 충돌)
- architecture-strategist P2 (Bootstrap 풀스캔 영구 잔존)

## Proposed Solutions

### Option A — Bootstrap 삭제 + Seed SQL 만 사용 (권장)
- `NewsJournalBootstrap` 클래스 삭제, `NewsEventJpaRepository.findDistinctUserIdsWithNullCategoryId` / `updateCategoryIdForNullByUserId` 메서드 삭제 (Bootstrap 전용이므로).
- 운영 절차: Phase B 자바 배포 → `news_event_category_seed_economy.sql` 적용 → NOT NULL DDL.
- 장점: 단일 경로, 빈 카테고리 잔존 없음, LOC -75, 운영 단순화, 부팅 사이드이펙트 제거.
- 단점: 운영자가 SQL 실행을 잊으면 NULL 사건이 영구 NULL.

### Option B — Bootstrap 의 기본명을 "경제" 로 통일 + Seed SQL 삭제
- `DEFAULT_CATEGORY_NAME = "경제"` 변경, Seed SQL 삭제.
- 장점: 자동 처리, 운영자 액션 불필요.
- 단점: 기본 카테고리명을 코드에 박는 것이 컨벤션상 부자연스러움.

### Option C — 두 경로 모두 유지하되 Seed SQL 에 "기타" 정리 SQL 동봉
- Seed 적용 후 빈 "기타" DELETE 옵션 SQL 주석 추가.
- 장점: 운영자가 선택할 수 있음.
- 단점: 충돌 자체는 그대로, 운영자 인지 부담.

## Recommended Action

**Option A 적용** — `NewsJournalBootstrap` 삭제 + `news_event_category_seed_economy.sql` 만 사용.
- `infrastructure/bootstrap/NewsJournalBootstrap.java` 파일 삭제 + 빈 패키지 정리
- `NewsEventJpaRepository` 의 backfill 전용 메서드 2개 (`findDistinctUserIdsWithNullCategoryId`, `updateCategoryIdForNullByUserId`) 삭제
- 운영 절차: Phase B 자바 배포 → `news_event_category_seed_economy.sql` 적용 → NULL=0 확인 → `news_event_category_not_null.sql` 적용
- LOC -75, 운영 단일 경로화, 빈 "기타" 잔존 시나리오 자동 제거
- 트레이드오프: 운영자가 Seed SQL 실행을 잊으면 NULL 사건이 영구 NULL — 1인 환경 + NOT NULL 강화가 게이트로 동작하므로 수용

## Technical Details

- 영향 파일:
  - `NewsJournalBootstrap.java` (Option A: 삭제, B: 상수 변경)
  - `NewsEventJpaRepository.java` (Option A: backfill 메서드 2개 삭제)
  - `news_event_category_seed_economy.sql` (Option B: 삭제, C: 주석 보강)
- 운영 영향: 이미 "기타" 가 적용된 환경이라면 Option A 채택 시 Seed SQL 한 번 실행으로 정리 + 빈 "기타" 수동 DELETE.

## Acceptance Criteria

- [ ] 데이터 의미가 단일 경로로 결정됨
- [ ] 빈 카테고리 잔존 시나리오 없음
- [ ] 운영 절차가 SQL 주석 / 설계 문서에 명확
- [ ] 컴파일 + 부팅 OK

## Work Log

- 2026-04-29: ce-review 발견 (simplicity P1, data-migration P1)
- 2026-04-29: Option A 적용. Bootstrap 클래스/패키지 삭제, JpaRepository backfill 메서드 2개 삭제. compileJava BUILD SUCCESSFUL. P2 인 033 (layer 위반), 037 (tx scope) 도 자동 해소됨.

## Resources

- `src/main/java/.../newsjournal/infrastructure/bootstrap/NewsJournalBootstrap.java`
- `src/main/java/.../newsjournal/infrastructure/persistence/NewsEventJpaRepository.java`
- `src/main/resources/db/migration/news_event_category_seed_economy.sql`
- `.claude/designs/newsjournal/news-event-category/news-event-category.md`