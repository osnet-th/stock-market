# [stocknote] List 필터 character/judgmentResult EXISTS 적용 (옵션 B)

> 분석: [list-filter-dead-fields](../../../analyzes/stocknote/list-filter-dead-fields/list-filter-dead-fields.md). plan task: Phase 10 P1 #10.

## 의도

`StockNoteRepositoryImpl.buildWhereClause` 에 character / judgmentResult 두 필터의 EXISTS subquery 를 추가해 Filter 정의 contract 와 실제 쿼리를 정합화. 미완료 "Phase 5" 코멘트 제거.

## 변경 사항

### 단일 파일 변경: `StockNoteRepositoryImpl.buildWhereClause`

```java
private String buildWhereClause(Long userId, StockNoteListFilter filter, Map<String, Object> params) {
    StringBuilder sb = new StringBuilder(" WHERE n.userId = :userId");
    params.put("userId", userId);
    if (filter.stockCode() != null && !filter.stockCode().isBlank()) {
        sb.append(" AND n.stockCode = :stockCode");
        params.put("stockCode", filter.stockCode());
    }
    if (filter.fromDate() != null) {
        sb.append(" AND n.noteDate >= :fromDate");
        params.put("fromDate", filter.fromDate());
    }
    if (filter.toDate() != null) {
        sb.append(" AND n.noteDate <= :toDate");
        params.put("toDate", filter.toDate());
    }
    if (filter.direction() != null) {
        sb.append(" AND n.direction = :direction");
        params.put("direction", filter.direction());
    }
    if (filter.character() != null) {
        sb.append(" AND EXISTS (SELECT 1 FROM StockNoteTagEntity t "
                + "WHERE t.noteId = n.id "
                + "  AND t.tagSource = com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.TagSource.CHARACTER "
                + "  AND t.tagValue = :character)");
        params.put("character", filter.character().name());
    }
    if (filter.judgmentResult() != null) {
        sb.append(" AND EXISTS (SELECT 1 FROM StockNoteVerificationEntity v "
                + "WHERE v.noteId = n.id "
                + "  AND v.judgmentResult = :judgmentResult)");
        params.put("judgmentResult", filter.judgmentResult());
    }
    return sb.toString();
}
```

> **참고**: TagSource enum 위치/이름 확인 필요. 현재 코드 패턴(StockNoteDashboardRepositoryImpl `t.tagSource = 'CHARACTER'` 문자열 비교) 와 일관 적용. 어댑터에서 enum 이 STRING 으로 매핑되므로 문자열 리터럴 사용해도 OK.

## 단순화 (실제 적용 코드)

`StockNoteDashboardRepositoryImpl` 의 패턴 그대로 문자열 리터럴 사용:

```java
if (filter.character() != null) {
    sb.append(" AND EXISTS (SELECT 1 FROM StockNoteTagEntity t "
            + "WHERE t.noteId = n.id AND t.tagSource = 'CHARACTER' AND t.tagValue = :character)");
    params.put("character", filter.character().name());   // RiseCharacter enum → String
}
if (filter.judgmentResult() != null) {
    sb.append(" AND EXISTS (SELECT 1 FROM StockNoteVerificationEntity v "
            + "WHERE v.noteId = n.id AND v.judgmentResult = :judgmentResult)");
    params.put("judgmentResult", filter.judgmentResult());   // JudgmentResult enum 그대로 (Hibernate STRING 매핑)
}
```

문자열 리터럴 'CHARACTER' 은 stock_note_tag.tag_source 가 STRING enum 매핑이라 정상 동작 (DashboardRepositoryImpl 검증된 패턴).

## 변경 동작

| 시나리오 | 변경 전 | 변경 후 |
|---|---|---|
| `?character=FUNDAMENTAL` | 결과 변화 없음 (silent ignore) | 해당 character 태그 보유 노트만 |
| `?judgmentResult=CORRECT` | 결과 변화 없음 | 검증 결과 CORRECT 노트만 |
| `?character=FUNDAMENTAL&judgmentResult=WRONG` | 결과 변화 없음 | AND 조건으로 둘 다 만족 |
| `?direction=UP` 만 | 정상 동작 (변경 없음) | 정상 동작 |

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| EXISTS subquery 추가로 인한 성능 영향 | 미미 (인덱스 idx_stock_note_tag_user_src_val_note + verification.note_id unique 활용) | n/a |
| count 쿼리도 동일 WHERE 절 사용 — 일관 동작 | 정합 ✅ | n/a |
| 한 노트에 같은 character 태그 여러 개일 때 EXISTS 는 중복 없음 | OK | JOIN 안 쓰는 이유 |
| RiseCharacter.name() 이 tag_value 와 일치 가정 | RiseCharacter (FUNDAMENTAL/EXPECTATION/...) 가 그대로 tag_value 로 저장됨 (StockNoteTag.normalizeFixed 패턴) | 기존 저장 패턴 검증 |

## 작업 리스트

- [ ] `StockNoteRepositoryImpl.buildWhereClause` 에 character / judgmentResult EXISTS subquery 추가
- [ ] "Phase 5 에서 JOIN 쿼리로 확장" 주석 제거 (javadoc 도)
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #10)

## 승인 대기

태형님 승인 후 구현 진행.
