# [stocknote] DTO 중복 정리 — Similar/Dashboard Response 제거 (옵션 A)

> 분석: [dto-duplication-cleanup](../../../analyzes/stocknote/dto-duplication-cleanup/dto-duplication-cleanup.md). plan task: Phase 10 P1 #16.

## 의도

명백한 1:1 복제 케이스(`SimilarPatternResponse`, `DashboardResponse`) 만 제거하고 Controller 가 application Result 를 직접 반환. ChartDataResponse / DetailResponse / ListResponse 는 가공이 의미 있어 유지.

## 변경 사항

### 1. `StockNoteAnalyticsController` — 반환 타입 Result 로 변경

```java
@GetMapping("/dashboard")
public ResponseEntity<DashboardResult> dashboard() {
    return ResponseEntity.ok(dashboardService.getDashboard(StockNoteSecurityContext.currentUserId()));
}

@GetMapping("/{id}/similar-patterns")
public ResponseEntity<SimilarPatternResult> similarPatterns(
        @PathVariable Long id,
        @RequestParam(required = false) NoteDirection directionFilter
) {
    return ResponseEntity.ok(
            patternMatchService.findSimilar(id, StockNoteSecurityContext.currentUserId(), directionFilter));
}
```

### 2. 파일 삭제

- `presentation/dto/SimilarPatternResponse.java`
- `presentation/dto/DashboardResponse.java`

### 3. import 정리

- `StockNoteAnalyticsController` 의 `DashboardResponse` / `SimilarPatternResponse` import 제거
- `DashboardResult` / `SimilarPatternResult` import 추가

## 응답 contract 호환

| 응답 필드 | DashboardResult | DashboardResponse (제거 전) | 동일? |
|---|---|---|---|
| thisMonthCount | long | long | ✅ |
| verifiedCount | long | long | ✅ |
| pendingVerificationCount | long | long | ✅ |
| hitRate | HitRate(correct,wrong,partial,total) | HitRateDto(동일) | ✅ |
| characterDistribution | Map<String,Long> | Map<String,Long> | ✅ |
| topTagCombinations | List<TagComboEntry(tagValues,count)> | List<TagComboDto(동일)> | ✅ |

| 응답 필드 | SimilarPatternResult | SimilarPatternResponse (제거 전) | 동일? |
|---|---|---|---|
| basisTags | List<TagPair(source,value)> | List<TagPayload(source,value)> | ✅ (record name 만 다름) |
| matches | List<Match(...)> | List<MatchDto(동일)> | ✅ |
| aggregate | Aggregate(total,correct,wrong,partial,avgD7Percent,avgD30Percent) | AggregateDto(동일) | ✅ |

JSON 직렬화 시 record component 이름이 그대로 필드명 → 프론트 무영향.

## 회귀 위험

| 위험 | 영향 | 완화 |
|---|---|---|
| `presentation` 가 `application/dto` 에 의존 (정상 — 이미 그렇게 사용 중) | 의존 방향 정합 | n/a |
| record 명 변경 (TagPayload → TagPair, MatchDto → Match 등) | record 클래스 이름은 JSON 직렬화에 무영향 | n/a |
| 다른 caller 가 SimilarPatternResponse 참조 | 본 PR 내 해당 컨트롤러만 사용 (grep 검증) | 컴파일러 |
| Caffeine 직렬화 — DashboardResult 가 응답에도 사용됨 | 이미 Serializable 명시 | OK |

## 작업 리스트

- [ ] `StockNoteAnalyticsController.dashboard` 반환 타입 → `DashboardResult` + 호출 단순화
- [ ] `StockNoteAnalyticsController.similarPatterns` 반환 타입 → `SimilarPatternResult` + 호출 단순화
- [ ] import 정리
- [ ] `presentation/dto/DashboardResponse.java` 삭제
- [ ] `presentation/dto/SimilarPatternResponse.java` 삭제
- [ ] 컴파일 확인
- [ ] plan checkbox 갱신 (P1 #16)

## ChartDataResponse / DetailResponse / ListResponse 보존 사유

- ChartDataResponse: NotePoint.summary ↔ triggerTextSummary 1자 차이 + DailyPrice → PricePoint 평면화
- StockNoteDetailResponse: locked 계산, NoteDto 평면화, snapshot enum→String, verification 변환
- StockNoteListResponse: Item.from 가공 (verified/locked/summarize/snapshot 요약)

## 후속 task 정합

- #36 P2 SimilarPattern aggregate 누락 필드 추가 시 SimilarPatternResult 만 수정 → 자동 응답 노출
- #29 P2 ListResponse verified/locked 의미 분리 — 본 task 와 독립

## 승인 대기

태형님 승인 후 구현 진행.
