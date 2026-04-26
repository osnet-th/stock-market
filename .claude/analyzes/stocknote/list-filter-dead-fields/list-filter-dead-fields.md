# [stocknote] StockNoteListFilter character/judgmentResult dead 필터

> ce-review 2026-04-25 P1 #10 (maintainability). plan task: Phase 10 P1.

## 현재 상태

`StockNoteController.findList` 가 `character` (RiseCharacter), `judgmentResult` (JudgmentResult) RequestParam 을 받아 `StockNoteListFilter` 로 채운다.
`StockNoteListFilter` record 에도 두 필드 정의됨 (L17-18).

`StockNoteRepositoryImpl.buildWhereClause` (L95-116) 에서:
```java
// character / judgmentResult 필터는 Phase 5 에서 JOIN 쿼리로 확장.
```
주석만 두고 실제 WHERE 절 추가 없음.

`findList` / `countList` 모두 두 필드 무시 → 클라이언트가 `character=FUNDAMENTAL` 을 넘겨도 결과는 필터 없는 것과 동일.

## 영향 범위

| 항목 | 동작 |
|---|---|
| 클라이언트 | API 가 정상 동작하는 것처럼 보이지만 실제 결과는 필터 없음 (silent contract violation) |
| 미래 개발자 | "Phase 5" 주석 보고 이미 끝났다고 추정 → 다시 손대지 않을 위험 |
| 프론트 | 현재 stocknote.js 에 character/judgmentResult 필터 UI 가 없음 — 외부 호출(Postman/curl) 만 영향 |

## 해결 옵션

### 옵션 A — 필드 제거 (Filter/Controller/Frontend)

`StockNoteListFilter` 에서 두 필드 제거 + Controller RequestParam 제거. 프론트는 미사용이라 영향 없음.

| 장점 | 단점 |
|---|---|
| 단순. dead code 제거 | 사용자가 잠재적으로 원할 수 있는 필터 기능 포기 |
| Filter/Controller/RepositoryImpl 모두 일관 | API 사용자가 character/judgmentResult 를 보내고 있었다면 contract 변경 |

### 옵션 B — JPQL EXISTS subquery 추가 (실제 동작)

`buildWhereClause` 에 `EXISTS (SELECT 1 FROM StockNoteTagEntity t WHERE t.noteId = n.id AND t.tagSource = 'CHARACTER' AND t.tagValue = :character)` + verification 동등 패턴 추가.

| 장점 | 단점 |
|---|---|
| Filter 정의 의도 살림 | EXISTS subquery 추가 (성능 영향 미미) |
| API 사용자에게 약속한 동작 제공 | 단위 테스트 가능 영역 추가 |
| 프론트가 향후 필터 UI 추가 시 그대로 동작 | |

JOIN 대신 EXISTS 선택: 같은 노트에 같은 source 의 태그가 여러 개일 때 JOIN 은 중복 row 생성 → DISTINCT/GROUP BY 보강 필요. EXISTS 가 깔끔.

## 추천: 옵션 B

근거:
- API/Filter contract 가 이미 정의되어 있고 사용자 기대치 명확 (character/judgmentResult 필터)
- 프론트 미사용은 일시적 — UI 확장 시 그대로 동작해야
- "Phase 5 에서 확장" 코멘트의 미완료 약속을 실제 구현으로 정리
- EXISTS subquery 비용 미미 (PostgreSQL 옵티마이저가 효율적 처리)

## 코드 위치

| 파일 | 변경 |
|---|---|
| `StockNoteRepositoryImpl.buildWhereClause` | character / judgmentResult EXISTS subquery 추가 + 코멘트 제거 |
| `StockNoteListFilter`, `StockNoteController.findList` | 변경 없음 (이미 정의됨) |

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #14 P1 pagination offset/limit → page/size | 별건. Filter record 변경 시 함께 처리 가능 |
| #13 P1 DashboardResponse contract drift | 별건. 응답 스키마 |

## 설계 문서

[list-filter-dead-fields](../../../designs/stocknote/list-filter-dead-fields/list-filter-dead-fields.md)
