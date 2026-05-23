---
date: 2026-05-24
topic: glossary-lower-null-bytea-fix
issue: 43
status: fix
---

# fix: glossary 용어 조회 `lower(bytea)` 실패

## 문제

용어 사전 조회 시 다음 에러로 500 발생:

```
ERROR: function lower(bytea) does not exist
Hint: No function matches the given name and argument types. You might need to add explicit type casts.
```

발생 SQL:
```sql
where ... and (? is null or lower(t.name) like lower(?) escape '\')
```

## 원인

PostgreSQL prepared statement 가 `?` 파라미터의 타입을 결정해야 하는데,
`(:q IS NULL OR ... LOWER(:q) ...)` 분기에서 `:q` 가 null 인 경우에도 PG 가
`lower(?)` 호출 식의 인수 타입을 미리 결정. 타입 힌트가 없어 `bytea` 로 추론
→ `lower(bytea)` 함수를 못 찾아 실패.

## 결정

**Java 측에서 미리 소문자 정규화 + SQL 측은 컬럼만 LOWER**.

1. `LikeEscaper.toContainsPattern()` 가 반환 직전 `.toLowerCase()` 적용 — 패턴 측 정규화
2. JPQL 에서 `LOWER(:q)` / `LOWER(:definitionQ)` 제거 — 컬럼만 LOWER 유지

검색 의미(R7 대소문자 무시)는 동일하게 보장. 응답 shape / 권한 검증 / 페이지네이션
모두 변경 없음. 영향 범위는 `GlossaryTermService.list` 경로 한정.

## 검증

- `./gradlew compileJava` BUILD SUCCESSFUL
- `./gradlew bootRun` 정상 부팅
- 브라우저 수동 검증: 용어 사전 페이지 조회 정상 동작 확인

## 후속

운영 후속 작업으로 `docs/solutions/` 에 "PG prepared statement + LOWER(:param) null
타입 추론" 회피 패턴을 정리해 두면 다른 도메인에서 동일 함정 회피 가능.