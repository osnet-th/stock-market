---
title: "fix: CLOSED 자산이 동일 자산 재등록을 409로 막는 문제"
type: fix
status: active
date: 2026-05-30
issue: 66
branch: fix/issue-66-portfolio-asset-reregister-409
origin: docs/brainstorms/2026-05-30-issue-66-portfolio-asset-reregister.md
---

# fix: CLOSED 자산이 동일 자산 재등록을 409로 막는 문제 (issue #66)

## Overview

`portfolio_item`의 full unique 제약 `UNIQUE(user_id, item_name, asset_type)`을
**partial unique index `WHERE status = 'ACTIVE'`**로 전환해, 애플리케이션 중복 검사 의도
(ACTIVE 최대 1개, CLOSED 다수 누적 허용)와 DB 제약을 일치시킨다. (brainstorm Option A)

앱 로직(`validateDuplicate`)은 이미 ACTIVE 한정이므로 **변경하지 않는다**.
변경은 엔티티 어노테이션 1곳 + 수동 마이그레이션 SQL 1개뿐이다.

## Problem Frame

origin brainstorm의 Problem Frame을 그대로 수용한다.
재현/원인/설계 의도는 origin 참조. 핵심 불변식: `(user_id, item_name, asset_type)`별 **ACTIVE 최대 1개, CLOSED 무제한**.

## Scope Boundaries

- 비목표: `validateDuplicate` 등 앱 중복검사 로직 변경 (이미 ACTIVE 한정 — 정상).
- 비목표: CLOSED row 되살림(reactivate) 또는 hard-delete 정책 (brainstorm Option C/D 기각).
- 비목표: 테스트 신규 작성 — 명시 요청 시에만 (Verification 절의 수동 검증으로 대체).
- 비목표: 다른 엔티티/제약 변경.

## Context & Research

### Relevant Code and Patterns

- 엔티티/제약: [PortfolioItemEntity.java:11-22](src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemEntity.java) — `@Table(uniqueConstraints={@UniqueConstraint(name="uk_portfolio_item", ...)})`. JOINED 상속의 부모 테이블 `portfolio_item`에 제약 존재.
- 앱 중복검사: [PortfolioService.java:1399-1404](src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java) `validateDuplicate` → [PortfolioItemRepositoryImpl.java:63-66](src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemRepositoryImpl.java) `...AndStatus(..., ACTIVE)`. 변경 불필요.
- 409 매핑: [GlobalExceptionHandler.java:80-85](src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java) `DataIntegrityViolationException → 409`.
- DDL 전략: `ddl-auto: update`(dev/prod), PostgreSQL. Flyway 자동 실행 없음 — `db/migration/*.sql`은 운영자 수동 `psql` 적용/롤백 백업용. 엔티티 어노테이션이 권위.
- 마이그레이션 SQL 헤더 컨벤션: [user_favorite_indicator_priority_not_null_unique_deferrable.sql](src/main/resources/db/migration/user_favorite_indicator_priority_not_null_unique_deferrable.sql) — 적용 순서/주의/plan 참조 헤더 + 단일 `BEGIN; ... COMMIT;`.

### Institutional Learnings

- [deferred-unique-constraint-retry-requires-new-2026-05-10.md](docs/solutions/architecture-patterns/deferred-unique-constraint-retry-requires-new-2026-05-10.md) — **핵심 함정**: Hibernate가 표현 못하는 제약(DEFERRABLE/partial)은 엔티티 `@UniqueConstraint`를 두면 `ddl-auto=update`가 NOT-special 중복 제약을 silent 재생성해 SQL 관리를 깨뜨린다. → 엔티티에서 제약 어노테이션을 **반드시 제거**.
- [jpa-version-passthrough-ddl-auto-not-null-backfill-2026-04-28.md](docs/solutions/architecture-patterns/jpa-version-passthrough-ddl-auto-not-null-backfill-2026-04-28.md) — 같은 `portfolio_item` 테이블. `ddl-auto=update`는 제약 DROP/특수 인덱스 생성 불가 → 수동 SQL 필수. (status 컬럼 default 컨벤션 출처)

## Key Technical Decisions

- **Partial unique index 채택**: `CREATE UNIQUE INDEX uk_portfolio_item_active ON portfolio_item (user_id, item_name, asset_type) WHERE status = 'ACTIVE'`. 이름은 기존 `uk_*` 컨벤션 + `_active` 접미사.
- **엔티티에서 `@UniqueConstraint` 제거**: partial(WHERE)은 Hibernate `@UniqueConstraint`로 표현 불가. 어노테이션을 남기면 ddl-auto가 full 제약을 재생성하는 함정(위 learning). `uniqueConstraints` 배열을 비우고, partial index는 SQL로만 관리한다는 주석을 엔티티에 남긴다.
- **앱 코드 무변경**: `validateDuplicate`는 이미 ACTIVE 한정. partial index는 앱 검사와 INSERT 사이 TOCTOU race(동시 ACTIVE 등록)도 DB가 방어 → 그 경우 409가 정답.
- **적용 순서(중요)**: 자바 배포(어노테이션 제거) → 그 다음 SQL 적용.
  - 사유: ddl-auto는 기존 `uk_portfolio_item`을 자동 DROP하지 않는다. 반대 순서(SQL 먼저)면 어노테이션이 남은 구버전 앱이 재기동될 때 ddl-auto가 full 제약을 재생성(silent re-add)할 위험. 어노테이션을 먼저 제거한 버전이 떠 있어야 안전.
  - dev도 동일: 어노테이션 제거만으로는 기존 DB의 `uk_portfolio_item`이 사라지지 않으므로(ddl-auto는 DROP 안 함), 같은 SQL을 수동 적용하거나 dev 스키마 재생성 필요.
- **기존 데이터 안전성**: 현재 full 제약상 조합별 row 최대 1개 → partial index 생성 시 ACTIVE 중복 충돌 없음. backfill 불필요.
- **롤백 제약 명시**: partial index 적용 후 사용자가 새 ACTIVE를 추가하면 (CLOSED + ACTIVE) 중복 조합이 생긴다. 이 시점 이후 full 제약 복원은 실패한다. 롤백은 "재등록 발생 전"에만 무손실. 마이그레이션 헤더에 경고.

## Implementation Units

### Unit 1 — 엔티티 어노테이션 제거

- [ ] [PortfolioItemEntity.java:11-22](src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemEntity.java) `@Table`의 `uniqueConstraints` 제거.
- [ ] `idx_portfolio_item_user_id` 인덱스(`indexes`)는 유지.
- [ ] partial unique index는 `db/migration` SQL로만 관리한다는 한국어 주석 추가 (ddl-auto 재생성 함정 경고 포함).

변경 후 `@Table`:
```java
@Table(
        name = "portfolio_item",
        // (user_id, item_name, asset_type) 유일성은 status='ACTIVE' partial unique index로만 관리한다.
        // Hibernate @UniqueConstraint는 WHERE 절(partial)을 표현 못하므로 여기 두지 않는다.
        // 어노테이션으로 두면 ddl-auto=update가 full unique 제약을 silent 재생성해 issue #66이 재발한다.
        // -> src/main/resources/db/migration/portfolio_item_active_partial_unique.sql
        indexes = {
                @Index(name = "idx_portfolio_item_user_id", columnList = "user_id")
        }
)
```

### Unit 2 — 마이그레이션 SQL 신규

- [ ] `src/main/resources/db/migration/portfolio_item_active_partial_unique.sql` 생성.
- [ ] 헤더: 목적/적용 순서/주의(롤백 제약)/plan 참조. favorite SQL 헤더 컨벤션 준수.
- [ ] 단일 `BEGIN; ... COMMIT;` — full 제약 DROP + partial index 생성을 원자 적용.
- [ ] 별도 롤백 SQL 스니펫(헤더 주석 내)도 명시.

SQL 본문(초안):
```sql
-- portfolio_item: ACTIVE 자산만 유일하도록 partial unique index 전환 (issue #66)
--
-- 배경:
--   기존 full UNIQUE(user_id, item_name, asset_type)는 status를 보지 않아
--   CLOSED row가 남으면 동일 자산 재등록(새 ACTIVE row)이 23505 -> 409로 막힌다.
--   앱 중복검사는 이미 ACTIVE 한정이므로 DB도 ACTIVE만 유일하게 맞춘다.
--
-- 적용 순서(중요):
--   1) 자바 배포 — PortfolioItemEntity 의 @UniqueConstraint(uk_portfolio_item) 제거본
--      (ddl-auto=update 는 기존 uk_portfolio_item 을 DROP 하지 않으며,
--       어노테이션이 남은 구버전이 떠 있으면 full 제약을 silent 재생성한다.)
--   2) 본 SQL 적용 (운영자 수동 psql, dev 도 동일 적용 필요)
--
-- 주의(롤백):
--   본 index 적용 후 사용자가 CLOSED 가 있는 조합에 새 ACTIVE 를 등록하면
--   (CLOSED+ACTIVE) 중복 조합이 생긴다. 그 이후 full 제약 복원은 실패한다.
--   롤백은 재등록 발생 전에만 무손실. 롤백 SQL:
--     BEGIN;
--     DROP INDEX IF EXISTS uk_portfolio_item_active;
--     ALTER TABLE portfolio_item
--         ADD CONSTRAINT uk_portfolio_item UNIQUE (user_id, item_name, asset_type);
--     COMMIT;
--
-- plan: docs/plans/2026-05-30-001-fix-portfolio-asset-reregister-409-plan.md
-- 적용: psql -v ON_ERROR_STOP=1 -f portfolio_item_active_partial_unique.sql

BEGIN;

ALTER TABLE portfolio_item
    DROP CONSTRAINT IF EXISTS uk_portfolio_item;

CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_item_active
    ON portfolio_item (user_id, item_name, asset_type)
    WHERE status = 'ACTIVE';

COMMIT;
```

## Verification

dev에서 SQL 적용 후 수동 검증:

- [ ] 자산 등록 → 전량 매도(CLOSED) → 동일 자산 재등록 → **200/201 성공** (기존엔 409).
- [ ] 등록 → 매도 → 재등록 → 매도 반복 시 CLOSED 다수 누적 + ACTIVE 최대 1개 유지.
- [ ] ACTIVE 상태에서 동일 자산 재등록 시도 → 앱 검사 `IllegalArgumentException`("이미 등록된 포트폴리오 항목입니다.") 또는 DB partial index 위반 → 정상 거부.
- [ ] 부팅 후 ddl-auto가 `uk_portfolio_item`을 재생성하지 않는지 확인 (`\d portfolio_item`).
- [ ] 기존 GET 흐름(보유 자산 목록 등) 회귀 없음.

검증 쿼리:
```sql
\d portfolio_item   -- uk_portfolio_item 없음, uk_portfolio_item_active partial index 존재 확인
SELECT user_id, item_name, asset_type, count(*) FILTER (WHERE status='ACTIVE') active_cnt
FROM portfolio_item GROUP BY 1,2,3 HAVING count(*) FILTER (WHERE status='ACTIVE') > 1;  -- 0행 기대
```

## Risks / Open Questions

- **롤백 비대칭성**: 재등록 발생 후 롤백 불가(위 헤더 경고). 운영 적용 전 인지 필요.
- **적용 순서 누락 위험**: 자바 배포 전 SQL을 먼저 적용 + 구버전 재기동 시 ddl-auto가 full 제약 재생성 가능. 순서 준수 필수.
- **partial index 이름 컨벤션**: `uk_portfolio_item_active` — 확정 필요(plan 검토 시).
- **prod 적용 주체/시점**: 운영자 수동 psql. 배포 파이프라인에 SQL 자동 적용 단계 없음 — 수동 체크리스트로 관리.

## 작업 위치

- worktree: `/Users/app/Documents/subProject/wt-fix-issue-66-portfolio-asset-reregister-409`
- 본 plan/brainstorm 문서는 위 브랜치 diff에 포함되어야 한다 (pre-push documented workflow 검사).
