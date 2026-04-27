---
title: JPA @Version 도메인 패스스루 + ddl-auto NOT NULL 컬럼 backfill 함정
date: 2026-04-28
module: portfolio
tags: [jpa, hibernate, version, optimistic-lock, ddl-auto, postgresql, domain-model, entity, migration]
problem_type: runtime-error
related_pr: feat/portfolio-sale (Unit 2)
related_issues: ["#32"]
---

# JPA `@Version` 도메인 패스스루 + ddl-auto NOT NULL 컬럼 backfill 함정

JPA Entity에 `status` enum 컬럼 + `@Version` 컬럼을 추가하면서 두 가지 문제가 연속 발생했다. 둘 다 *이론적으로* plan 단계에서 인지했지만 실제 부팅/매도 시도에서야 노출되었다.

## Problem

**증상 1 — 부팅 후 모든 portfolio API 500:**
```
ERROR: column pie1_0.status does not exist
  Position: 230
org.springframework.dao.InvalidDataAccessResourceUsageException: JDBC exception executing SQL [...]
```
보유 자산 목록(`/api/portfolio/items`), 자산 배분(`/api/portfolio/allocation`) 모두 깨짐. 매도 기능이 아닌 *기존* 흐름까지 영향.

**증상 2 — 매도 1회 시도에 즉시 409:**
```
DataIntegrityViolationException: Detached entity with generated id '25' has an uninitialized version value 'null'
  for entity ...CashItemEntity.version
```
글로벌 OptimisticLock 핸들러가 catch해서 409로 변환 → 사용자가 "동시 매도 충돌"로 오해.

## Root Cause

### 증상 1: ddl-auto:update의 NOT NULL 컬럼 추가 한계

PostgreSQL은 **기본값 없는 NOT NULL 컬럼**을 *기존 행이 있는 테이블*에 추가할 수 없다. Hibernate `ddl-auto: update`는 `ALTER TABLE ... ADD COLUMN ... NOT NULL`을 시도하지만 실패하면 **silent skip**한다. 그 결과:

- DB 스키마: 컬럼 없음
- 엔티티 매핑: 컬럼 있음 (`@Column(name = "status", nullable = false)`)
- JPQL 쿼리는 `WHERE p.status = ?`를 발행 → "column does not exist" 에러

### 증상 2: 도메인 ↔ Entity 매핑에서 `version` 손실

`@Version`은 인프라 관심사라 도메인에 두지 않으려는 의도였다(plan Unit 2 원안). 그러나 Mapper의 `toDomain` → 도메인 mutate → `toEntity` 왕복에서:

```
DB row (version=0)
  → JpaRepo fetch (entity.version=0)
  → mapper.toDomain (도메인에 version 필드 없음 → 정보 손실)
  → 도메인 mutate (cashItem.restoreAmount 등)
  → mapper.toEntity (새 entity 인스턴스, version=null)
  → save 시도
  → JPA: "id 있고 version null → detached, merge 실패"
  → DataIntegrityViolation
```

새 엔티티 인스턴스의 version이 null이라 JPA의 detached-vs-new 판정이 잘못된다. 이전엔 컬럼이 nullable이라 우연히 통과했을 수도 있는데, 이번엔 NOT NULL DEFAULT 0으로 추가되면서 명확히 깨졌다.

## Solution

### Fix 1 — Entity 컬럼에 `columnDefinition`으로 DEFAULT 명시

```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false, length = 20,
        columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'")
private PortfolioItemStatus status;

@Version
@Column(name = "version", nullable = false,
        columnDefinition = "BIGINT NOT NULL DEFAULT 0")
private Long version;
```

ddl-auto:update가 다음 형태로 ALTER를 발행 → 기존 행 backfill까지 자동:
```sql
ALTER TABLE portfolio_item ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE portfolio_item ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
```

### Fix 2 — 도메인에 `version` 패스스루 필드 추가 (read-only)

```java
@Getter
public class PortfolioItem {
    private PortfolioItemStatus status;
    private Long version;  // JPA @Version 패스스루용 — read-only, mutate 메서드 X
    // ...

    // reconstruction constructor에 version 인자 추가 (16-arg → 17-arg)
    public PortfolioItem(..., PortfolioItemStatus status, Long version,
                         LocalDateTime createdAt, LocalDateTime updatedAt, ...) {
        this.version = version;
        // ...
    }

    // 새 항목 factory(create*)에서 version = 0L 초기화
    private PortfolioItem(...) {
        this.version = 0L;
    }
}
```

Mapper의 `toDomain`/`toEntity` 양쪽에 version 매핑 추가:
```java
// toDomain
return new PortfolioItem(..., entity.getStatus(), entity.getVersion(), ...);

// toEntity (9개 sub-entity case 모두)
yield new StockItemEntity(..., item.getStatus(), item.getVersion(), ...);
```

도메인은 version을 **읽기만** 한다 (mutate/setter 절대 X). 도메인 캡슐화 정신은 유지하면서 영속성 라운드트립의 정보 손실만 회피.

## Prevention

NOT NULL 컬럼을 *기존 데이터가 있는 테이블*에 추가하거나 `@Version`을 도입하는 모든 PR에서 다음 체크리스트를 강제:

- [ ] **`@Column(nullable = false)`인 경우 `columnDefinition`에 `DEFAULT` 포함했는가?** dev에서 ddl-auto가 자동 backfill (운영은 plan에 별도 마이그레이션 SQL 명시)
- [ ] **`@Version` 도입 시 도메인 모델에도 read-only 패스스루 필드 + reconstruction constructor 인자 추가했는가?**
- [ ] **Mapper의 toDomain/toEntity가 신규 필드를 양방향으로 보존하는가?** 9개 sub-entity case 누락 주의 (JOINED 상속의 함정)
- [ ] **단위 테스트의 reconstruction constructor 호출도 신규 인자에 맞게 갱신했는가?** (`new PortfolioItem(...)` 직접 호출하는 모든 테스트)
- [ ] **부팅 직후 기존 흐름 (보유 자산 목록 등) 회귀 테스트** — 신규 기능만 보지 말고 기존 GET을 한 번이라도 호출해 본다

## References

- **이번 PR**: `feat/portfolio-sale` Unit 2
- **plan 문서 보정 노트**: `docs/plans/2026-04-26-001-feat-portfolio-stock-sale-plan.md` Unit 2 Approach (2026-04-27 보정)
- **관련 institutional learning**: `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` (트랜잭션 안 외부 HTTP 격리 — 본 PR에서 함께 보강)
- **Hibernate User Guide**: `@Version` optimistic locking 동작
- **이번 fix 코드**:
  - Entity DEFAULT: `PortfolioItemEntity.java:53-61`
  - 도메인 패스스루: `PortfolioItem.java:23-25`
  - Mapper 보강: `PortfolioItemMapper.java:87, 110~`
