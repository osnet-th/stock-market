# [stocknote] pagination 컨벤션 — offset/limit vs page/size 불일치

> ce-review 2026-04-25 P1 #14 (api-contract). plan task: Phase 10 P1.

## 현재 상태

### stocknote — `offset/limit`

| 위치 | 사용 |
|---|---|
| `StockNoteController.findList:72-73` | RequestParam `offset, limit` |
| `StockNoteListFilter` record | `int offset, int limit` (validation) |
| `StockNoteListResult` record | `int offset, int limit` |
| `StockNoteListResponse` record | `int offset, int limit` |
| `StockNoteRepositoryImpl.findList` | `setFirstResult(filter.offset())`, `setMaxResults(filter.limit())` |
| `stocknote.js filters` | `offset: 0, limit: 20` |

### 다른 모듈 — `page/size`

`NewsController:32-33`:
```java
@RequestParam(defaultValue = "0") int page,
@RequestParam(defaultValue = "20") int size
```

### plan L366 — `page/size` 명시

```
GET /api/stock-notes?stockCode=&from=&to=&direction=&character=&judgmentResult=&page=&size=
```

→ stocknote 만 모듈 컨벤션 + plan 모두에서 분기.

## 영향 범위

| 항목 | 영향 |
|---|---|
| 외부 API 사용자 | plan 보고 `page/size` 호출 → 무시되고 `offset/limit` 기본값으로 작동 (silent drift) |
| 프론트 페이지네이션 헬퍼 | 모듈별 다른 키 사용 → 재사용 불가 |
| 타 도메인 일관성 | 사용자가 stocknote 만 다른 키 사용 |

## 해결

### 단일 옵션 — 모두 `page/size` 로 통일 (모듈 컨벤션 + plan 정합)

Backend 4 파일 (Controller/Filter/Result/Response) + Frontend 1 파일.

`RepositoryImpl.findList` 내부에서 `setFirstResult(filter.page() * filter.size())` 변환. JPA 의 firstResult/maxResults 는 변경 없음.

| 변경 전 | 변경 후 |
|---|---|
| `?offset=20&limit=20` | `?page=1&size=20` |
| `StockNoteListFilter(offset, limit)` | `StockNoteListFilter(page, size)` |
| `setFirstResult(offset)` | `setFirstResult(page * size)` |

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| #16 P1 DTO 폭발 정리 | Filter/Result/Response 모두 동일 필드명으로 통일 — DTO 정리 시 더 단순 |

## 코드 위치

| 파일 | 변경 |
|---|---|
| `StockNoteController.findList` | RequestParam `offset/limit` → `page/size` |
| `StockNoteListFilter` record | 필드명 변경 + validation |
| `StockNoteListResult` record | 필드명 변경 |
| `StockNoteListResponse` record | 필드명 + `from()` 변환 |
| `StockNoteRepositoryImpl.findList` | `setFirstResult(page * size)` 변환 |
| `stocknote.js` filters | `offset/limit` → `page/size` |

## 설계 문서

[pagination-page-size](../../../designs/stocknote/pagination-page-size/pagination-page-size.md)
