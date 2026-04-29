---
status: complete
priority: p1
issue_id: 031
tags: [code-review, architecture, performance, transactions, newsjournal]
dependencies: []
---

# `NewsEventCategoryService.resolve` 의 race retry 가 동일 트랜잭션에서 동작하지 않음

## Problem Statement

설계 문서가 명시한 race 방어 패턴이 실제로는 무효화된 상태. Spring `@Transactional` 의 propagation 동작과 PostgreSQL 의 트랜잭션 abort 동작이 결합되어, 의도된 1회 재조회가 외곽 트랜잭션을 함께 롤백시킵니다.

## Findings

- 위치: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/application/NewsEventCategoryService.java:38-56`
- 호출 컨텍스트: `NewsEventWriteService.create:35-37`, `NewsEventWriteService.update:54-55`

```java
} catch (DataIntegrityViolationException race) {
    return categoryRepository.findByUserIdAndName(userId, trimmed)
            .orElseThrow(() -> race);
}
```

문제 시퀀스:
1. `NewsEventWriteService.create` 가 `@Transactional` (REQUIRED) 시작
2. `categoryService.resolve` 가 `@Transactional` (REQUIRED) → 동일 트랜잭션에 합류
3. `save(...)` 가 unique 위반 → `DataIntegrityViolationException`
4. Spring 이 외곽 트랜잭션을 **rollback-only 로 마킹**
5. PostgreSQL 은 unique 위반 statement 이후 같은 트랜잭션의 모든 쿼리에 `25P02 in_failed_sql_transaction` 반환
6. 재조회 자체가 실패하거나, 성공해도 외곽 `create` 의 commit 이 `UnexpectedRollbackException` 으로 강제 롤백

결과: race 발생 시 retry 가 동작하지 않고 사건 생성이 실패함. 1인 환경에선 발현 빈도 0 에 가까우나, 코드 주석이 "1회 재조회로 복구" 라고 적힌 만큼 의도-동작 괴리가 P1.

확인 보고:
- architecture-strategist P1 (#2)
- performance-oracle P2 (resolve retry 트랜잭션 함정)
- code-simplicity-reviewer P2 (1인 환경 과한 방어 — 단순화 옵션도 함께 고려)

## Proposed Solutions

### Option A — `insertOrRecover` 만 `REQUIRES_NEW` 분리 (권장)
- `resolve` 는 그대로 REQUIRED, INSERT 시도하는 helper 만 별도 빈 + `@Transactional(propagation=REQUIRES_NEW)` 로 분리.
- 장점: 외곽 트랜잭션 오염 없음, 재조회 시 commit 된 다른 트랜잭션 결과 가시.
- 단점: Spring AOP self-invocation 우회를 위해 별도 빈 (`NewsEventCategoryInsertExecutor`) 분리 필요.
- 효과: race 시 의도대로 복구.
- 참고 패턴: `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`

### Option B — PostgreSQL `INSERT ... ON CONFLICT DO NOTHING RETURNING id` 사용
- JPQL/JPA 로는 표현 불가 → native query.
- 장점: 단일 statement, race-free, 별도 트랜잭션 불필요.
- 단점: native SQL 도입 (현 모듈 미사용 패턴), id 미반환 시 SELECT 폴백 필요.

### Option C — race retry 자체 제거 (1인 환경 단순화)
- 1인 환경 + 멀티탭 동시 저장 우려 없으면 try-catch 제거. unique 제약은 안전망으로 유지.
- 장점: 코드 8줄 감소, 의도-동작 괴리 해소.
- 단점: 다중 사용자 확장 시 사건 생성 실패가 사용자에게 전파됨.

## Recommended Action

**Option C 적용** — race retry 자체 제거 (1인 환경 단순화).
- `insertOrRecover` 메서드 삭제
- `resolve` 가 `findByUserIdAndName(...).orElseGet(() -> categoryRepository.save(...))` 단일 라인으로 단순화
- unique `(user_id, name)` 제약은 안전망으로 유지 (race 시 호출자에게 `DataIntegrityViolationException` 전파)
- 의도-동작 괴리 해소, 코드 8줄 감소
- 트레이드오프: 다중 사용자 확장 시 race 시 사건 생성 실패 노출. 그 시점에 Option A (REQUIRES_NEW + 별도 빈) 재도입 권장.

## Technical Details

- 영향 파일: `NewsEventCategoryService.java`, (Option A 채택 시) 신규 `NewsEventCategoryInsertExecutor.java`
- 영향 트랜잭션: `NewsEventWriteService.{create,update}` 의 외곽 트랜잭션 안정성

## Acceptance Criteria

- [ ] race 시 의도대로 동작 (또는 race retry 자체 제거)
- [ ] 외곽 `create/update` 트랜잭션 commit 가능
- [ ] 코드 주석과 실제 동작 일치
- [ ] 컴파일 + 부팅 회귀 OK

## Work Log

- 2026-04-29: ce-review 발견 (architecture P1 #2, performance P2)
- 2026-04-29: Option C 적용. `insertOrRecover` 제거, `resolve` 단일 라인. 클래스 javadoc 도 race retry 의도 → "예외는 호출자에게 전파" 로 정정. compileJava BUILD SUCCESSFUL.

## Resources

- `src/main/java/.../newsjournal/application/NewsEventCategoryService.java`
- `src/main/java/.../newsjournal/application/NewsEventWriteService.java`
- `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`