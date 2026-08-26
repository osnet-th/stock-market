---
title: "fix: glossary 용어 조회 lower(null) bytea 회피"
type: fix
status: completed
date: 2026-05-24
origin: docs/brainstorms/2026-05-24-glossary-lower-null-bytea-fix.md
issue: 43
---

# fix: glossary 용어 조회 `lower(null)` bytea 회피

## Overview

용어 사전 조회 시 PostgreSQL 이 prepared statement 의 null 파라미터를 `bytea` 로
추론해 `lower(?)` 호출이 실패하던 문제를 수정. JPQL 패턴 측 `LOWER(:q)` 호출을
제거하고 application 계층에서 미리 소문자 정규화된 패턴을 넘기는 방식으로 전환.

## Requirements Trace

- R7. 용어명 부분 일치 검색 (대소문자 무시) — 의미 보존, SQL 실행 정상화

## Scope Boundaries

- 검색 결과 의미 변경 없음 (Java `toLowerCase` ↔ PG `LOWER` 결과는 ASCII/한글 입력에서 동일)
- 응답 shape, 권한 검증, 페이지네이션, 정렬 변경 없음
- 다른 도메인 영향 없음 (`glossary/` 한정)

## Implementation Units

- [x] **Unit 1: LikeEscaper 가 lowercase 패턴을 반환하도록 수정**

**Files:**
- Modify: `src/main/java/com/thlee/stock/market/stockmarket/glossary/application/support/LikeEscaper.java`

**Approach:**
- `toContainsPattern()` 반환 직전 `.toLowerCase()` 적용. JavaDoc 에 PG 회피 사유 명시.

- [x] **Unit 2: JPQL 에서 `LOWER(:q)` / `LOWER(:definitionQ)` 제거**

**Files:**
- Modify: `src/main/java/com/thlee/stock/market/stockmarket/glossary/infrastructure/persistence/GlossaryTermJpaRepository.java`

**Approach:**
- `findList`/`countList` 두 쿼리에서 `LOWER(:q)` → `:q`, `LOWER(:definitionQ)` → `:definitionQ`
- 컬럼 측 `LOWER(t.name)` / `LOWER(t.definition)` 는 유지 (PG 함수형 인덱스 도입 시 호환)

## Verification

- `./gradlew compileJava` BUILD SUCCESSFUL
- `./gradlew bootRun` 정상 부팅 (PID 90368)
- 브라우저 수동 검증: 용어 사전 조회/검색 정상 동작

## Risks & Dependencies

| Risk | Mitigation |
|------|------------|
| Java `toLowerCase` 와 PG `LOWER` 의 locale 차이 | ASCII/한글 입력에서 동일. Turkish locale 등 코너 케이스는 사용자 입력 범위 밖 |
| 향후 함수형 인덱스 도입 시 호환성 | 컬럼 측 `LOWER` 유지로 `CREATE INDEX ... ON glossary_term (LOWER(name))` 패턴 그대로 호환 |