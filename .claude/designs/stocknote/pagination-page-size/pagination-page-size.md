# [stocknote] pagination page/size 통일

> 분석: [pagination-page-size](../../../analyzes/stocknote/pagination-page-size/pagination-page-size.md). plan task: Phase 10 P1 #14.

## 의도

stocknote 의 `offset/limit` 을 `page/size` 로 일괄 변경. News/AdminLog 컨벤션 + plan L366 정합. RepositoryImpl 안에서 `firstResult = page * size` 변환.

## 변경 사항

### 1. `StockNoteController.findList`

```java
@GetMapping
public ResponseEntity<StockNoteListResponse> findList(
        @RequestParam(required = false) String stockCode,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(required = false) NoteDirection direction,
        @RequestParam(required = false) RiseCharacter character,
        @RequestParam(required = false) JudgmentResult judgmentResult,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
) {
    StockNoteListFilter filter = new StockNoteListFilter(
            stockCode, from, to, direction, character, judgmentResult, page, size);
    ...
}
```

### 2. `StockNoteListFilter` record

```java
public record StockNoteListFilter(
        String stockCode,
        LocalDate fromDate,
        LocalDate toDate,
        NoteDirection direction,
        RiseCharacter character,
        JudgmentResult judgmentResult,
        int page,
        int size
) {
    public StockNoteListFilter {
        if (page < 0) {
            throw new IllegalArgumentException("page 는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > 200) {
            throw new IllegalArgumentException("size 는 1~200 범위여야 합니다.");
        }
    }
}
```

### 3. `StockNoteListResult` record

```java
public record StockNoteListResult(
        List<StockNoteListItemResult> items,
        long totalCount,
        int page,
        int size
) { }
```

### 4. `StockNoteListResponse` record + `from()`

```java
public record StockNoteListResponse(
        List<Item> items,
        long totalCount,
        int page,
        int size
) {
    public static StockNoteListResponse from(StockNoteListResult r) {
        return new StockNoteListResponse(
                r.items().stream().map(Item::from).toList(),
                r.totalCount(),
                r.page(),
                r.size()
        );
    }
    ...
}
```

### 5. `StockNoteRepositoryImpl.findList` — JPA 변환

```java
query.setFirstResult(filter.page() * filter.size());
query.setMaxResults(filter.size());
```

### 6. `StockNoteReadService.findList` — Result 조립

```java
return new StockNoteListResult(items, totalCount, filter.page(), filter.size());
```

### 7. `stocknote.js` filters

```javascript
filters: {
    stockCode: '',
    from: '',
    to: '',
    direction: '',
    character: '',
    judgmentResult: '',
    page: 0,
    size: 20
}
```

## 변경 동작

| 시나리오 | 변경 전 | 변경 후 |
|---|---|---|
| `?offset=20&limit=20` | 두 번째 페이지 | 무시 (defaultValue 적용 → `page=0, size=20`) |
| `?page=1&size=20` | 무시 | 두 번째 페이지 정상 |
| Response | `{items, totalCount, offset, limit}` | `{items, totalCount, page, size}` |

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| 기존 외부 호출자가 offset/limit 사용 중일 때 | 본 PR 첫 도입이라 외부 사용자 0명 | n/a |
| 프론트 변경 누락 | 다음 페이지 로드 안 됨 | stocknote.js + index.html(미사용 — 페이지네이션 UI 부재) 확인 |
| Filter 도메인 record 필드명 변경 — 호출 측 컴파일 에러 | StockNoteController + StockNoteReadService.findList 두 곳만 사용 | 컴파일러 검증 |

## 작업 리스트

- [ ] `StockNoteController.findList` RequestParam page/size
- [ ] `StockNoteListFilter` record 필드 + validation
- [ ] `StockNoteListResult` record 필드
- [ ] `StockNoteListResponse` record 필드 + `from()`
- [ ] `StockNoteRepositoryImpl.findList` `setFirstResult(page * size)`
- [ ] `StockNoteReadService.findList` Result 조립 page/size
- [ ] `stocknote.js` filters
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #14)

## 승인 대기

태형님 승인 후 구현 진행.