# [stocknote] Plan 갱신 — 구현 형식 반영 + hitRate 후속 명시 (옵션 A)

> 분석: [dashboard-contract-drift](../../../analyzes/stocknote/dashboard-contract-drift/dashboard-contract-drift.md). plan task: Phase 10 P1 #13.

## 의도

plan L400-401 의 dashboard 응답 스펙을 현재 구현 형식으로 갱신하고, hitRate 미구현은 후속 작업으로 명시.

## 변경 사항

### 단일 파일 변경: `docs/plans/2026-04-23-001-feat-stock-note-plan.md`

**변경 전** (L400-401):
```
characterDistribution: [{ character: 'FUNDAMENTAL', count, hitRate }, ...],
topTagCombinations: [{ tags: [...], count, hitRate }, ...]
```

**변경 후**:
```
characterDistribution: { 'FUNDAMENTAL': count, 'EXPECTATION': count, ... },   # Map<String, Long>, hitRate 별건 후속
topTagCombinations: [{ tagValues: [...], count }, ...]                        # hitRate 별건 후속
```

추가 메모:
- character/combo 별 `hitRate` 는 별도 plan (RiseCharacter × verification.judgmentResult JOIN 집계).
- characterDistribution Map 형식은 프론트 `Object.entries` 패턴과 일관 — array 변환은 hitRate 도입 시 함께.

## 후속 task 권장

신규 별건 plan 후보:
- "[stocknote] character/combo 별 hitRate 분석 — DashboardResponse 확장" (P3 권장 우선순위)

## 코드 변경 없음

DashboardResponse / DashboardResult / 프론트 모두 무변경. plan 만 정합화.

## 회귀 위험

없음 — plan 문서만 수정.

## 작업 리스트

- [ ] plan L400-401 갱신
- [ ] plan checkbox 갱신 (P1 #13)

## 승인 대기

태형님 승인 후 진행.