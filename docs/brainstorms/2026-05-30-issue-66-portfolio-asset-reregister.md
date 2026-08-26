---
date: 2026-05-30
topic: issue-66-portfolio-asset-reregister-409
origin:
  - github issue #66
related_solutions:
  - docs/solutions/architecture-patterns/jpa-version-passthrough-ddl-auto-not-null-backfill-2026-04-28.md
  - docs/solutions/architecture-patterns/deferred-unique-constraint-retry-requires-new-2026-05-10.md
---

# CLOSED 자산이 동일 자산 재등록을 409로 막는 문제 (issue #66)

## Problem Frame

전량 매도로 `CLOSED` 상태가 된 포트폴리오 자산이 DB에 남아 있으면,
동일한 `(user_id, item_name, asset_type)` 조합의 자산을 다시 등록할 때 `409 CONFLICT`가 발생한다.

핵심 불일치:

- **애플리케이션 중복 검사**: `ACTIVE` 항목만 대상 → CLOSED는 통과시킴 (재매수를 새 항목으로 허용하려는 의도)
- **DB unique 제약**: 상태를 보지 않음 → CLOSED row까지 중복으로 판단

사용자는 화면에서 ACTIVE만 보므로 "등록된 자산 없음" 상태인데도 등록이 실패한다.

## 재현 흐름

1. 자산 등록 → `ACTIVE` row 생성
2. 전량 매도 → 같은 row가 `CLOSED`로 상태 변경 (row 유지, 매도 이력 보존)
3. 동일 자산 재등록 시도
   - 앱 검사 `existsBy...AndStatus(ACTIVE)` → `false` (CLOSED만 존재) → 통과
   - 새 `ACTIVE` row INSERT 시도
   - DB `UNIQUE (user_id, item_name, asset_type)` → 기존 CLOSED row와 충돌 → `23505`
   - `DataIntegrityViolationException` → `GlobalExceptionHandler` → `409 CONFLICT`

## Key Context (코드 근거)

- 엔티티/제약: [PortfolioItemEntity.java:11-18](src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemEntity.java) — `@UniqueConstraint(name="uk_portfolio_item", columnNames={"user_id","item_name","asset_type"})`
- 상태 enum: [PortfolioItemStatus.java:3-5](src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/enums/PortfolioItemStatus.java) — `ACTIVE`, `CLOSED`
- 앱 중복 검사: [PortfolioService.java:1399-1404](src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java) `validateDuplicate` (호출부: 106, 152, 174, 199, 229, 282)
- 리포지토리 어댑터: [PortfolioItemRepositoryImpl.java:63-66](src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemRepositoryImpl.java) — `...AndStatus(..., ACTIVE)`
- 도메인 의도: [PortfolioItemRepository.java:43-46](src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/repository/PortfolioItemRepository.java) — "재매수는 새 항목으로 등록 가능하도록 CLOSED는 제외"
- 409 매핑: [GlobalExceptionHandler.java:80-85](src/main/java/com/thlee/stock/market/stockmarket/infrastructure/web/GlobalExceptionHandler.java)
- DDL 전략: `ddl-auto: update` (dev/prod), PostgreSQL. Flyway 자동 실행 없음 — `db/migration/*.sql`은 운영 수동 적용/백업용. 엔티티 어노테이션이 권위.

## 설계 의도 확정

도메인 주석이 명시한다: **재매수는 기존 CLOSED row를 되살리는 게 아니라 새 ACTIVE row로 등록**한다.
즉 (user, item, asset_type)별로 **CLOSED는 여러 개 누적 가능, ACTIVE는 최대 1개**가 올바른 불변식이다.
현재 full unique 제약은 이 불변식보다 과하게 넓다.

## Solution Options

### Option A — Partial unique index `WHERE status = 'ACTIVE'` (추천)

```sql
CREATE UNIQUE INDEX uk_portfolio_item_active
    ON portfolio_item (user_id, item_name, asset_type)
    WHERE status = 'ACTIVE';
```

- DB 제약을 앱 검사 의도와 정확히 일치시킨다: ACTIVE 최대 1개만 강제, CLOSED는 자유롭게 누적.
- 재등록 시 CLOSED row가 충돌을 일으키지 않는다.
- 앱 검사(`existsBy...AndStatus(ACTIVE)`)와 INSERT 사이의 TOCTOU race도 DB가 막아준다 (true 동시 등록은 409가 정답).
- 본 repo 확립 패턴과 정합: Hibernate `@UniqueConstraint`는 partial(WHERE) 표현 불가 → **엔티티에서 `@UniqueConstraint` 제거** + 수동 SQL 마이그레이션. (제거 안 하면 ddl-auto가 full 제약을 silent 재생성해 swap이 깨짐 — `deferred-unique-constraint` 학습과 동일 함정)
- 기존 데이터 안전: 현재 full 제약상 조합별 row는 최대 1개 → partial index 생성 시 ACTIVE 중복 충돌 없음 (backfill 불필요).
- 비용: dev/prod 모두 수동 SQL 적용 필요(ddl-auto는 제약 DROP/partial 생성 불가). 엔티티 어노테이션 제거를 잊으면 안 됨.

### Option B — DB unique 제약 완전 제거, 앱 검사에만 의존

- 가장 단순하나 DB 레벨 무결성 가드 상실. `existsBy` 검사와 INSERT 사이 동시 등록 race에서 ACTIVE 중복 2건 INSERT 가능.
- 데이터 정합성 후퇴. **기각.**

### Option C — 재등록 시 기존 CLOSED row를 ACTIVE로 되살림(reactivate)

- 제약 자체를 우회(새 row 없음).
- 그러나 도메인 의도("새 항목으로 등록")와 충돌, 이전 매도 이력 의미가 꼬임. **기각** (단, 제품이 머지 의미를 원하면 Open Question으로).

### Option D — 전량 매도 시 hard-delete (CLOSED 상태 폐기)

- CLOSED는 매도 이력 보존을 위해 의도적으로 도입(issue #32). **기각** — 이력 보존 요구 위반.

## 추천

**Option A**. 앱 의도와 DB 제약을 일치시키는 최소·정공법이며 repo 패턴과 정합한다.

예상 변경 범위:

1. `PortfolioItemEntity.java` — `@Table`의 `@UniqueConstraint(uk_portfolio_item)` 제거 (uniqueConstraints 비움 또는 partial 의도 주석)
2. `src/main/resources/db/migration/portfolio_item_active_partial_unique.sql` (신규) — 기존 `uk_portfolio_item` DROP + partial unique index 생성, 단일 트랜잭션, 적용 순서/주의 헤더 (favorite SQL 헤더 컨벤션 준수)
3. 앱 코드 변경 불필요 (`validateDuplicate`는 이미 ACTIVE 한정 — 정상)

## Open Questions / Assumptions

- (확인) `(user, item, asset_type)`당 CLOSED 다수 누적 + ACTIVE 1개가 의도된 불변식인가? — 도메인 주석상 yes로 가정.
- (확인) 재매수를 기존 CLOSED 되살림이 아니라 새 row로 등록하는 게 맞는가? — 도메인 주석상 yes로 가정 (Option C 기각 근거).
- partial index 이름 `uk_portfolio_item_active` 컨벤션 OK?
- prod 적용 시점/절차 — 자바 배포(어노테이션 제거)와 SQL 적용 순서. ddl-auto는 기존 `uk_portfolio_item`을 자동 DROP하지 않으므로 SQL 적용 전까지는 dev/prod 모두 기존 제약이 살아있다(재현 지속). 순서 명시 필요.

## Scope

- 변경: 엔티티 어노테이션 1곳 + 마이그레이션 SQL 1개.
- 테스트: 명시 요청 시에만 (재등록 happy path + CLOSED 누적 시나리오).
- plan 단계에서 적용 순서/롤백 SQL 확정.
