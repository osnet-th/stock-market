# [stocknote] DashboardResponse contract drift — plan vs 구현 불일치

> ce-review 2026-04-25 P1 #13 (api-contract). plan task: Phase 10 P1.

## 현재 상태

### plan L400-401 명시 (의도)

```
characterDistribution: [{ character: 'FUNDAMENTAL', count, hitRate }, ...]
topTagCombinations:    [{ tags: [...], count, hitRate }, ...]
```

### 실제 구현 (`DashboardResponse` + 프론트)

| 필드 | plan | 구현 | 프론트 사용 |
|---|---|---|---|
| `characterDistribution` | array of object | `Map<String, Long>` | `Object.entries(...)` (index.html:2738) |
| `characterDistribution.hitRate` | 명시 | **누락** | 미사용 |
| `topTagCombinations.tags` | `tags` | **`tagValues` 리네이밍** | `combo.tagValues` (index.html:2752) |
| `topTagCombinations.hitRate` | 명시 | **누락** | 미사용 |

## 영향 범위

### 외부 API 사용자 (가상)

plan 을 contract 로 받아 구현한 외부 클라이언트 → array 기대했는데 object 받음 + `tags` 기대했는데 `tagValues` 받음 → 파싱 실패.

### 프론트 (실제)

이미 구현된 stocknote.js / index.html 은 **현재 코드 형식 사용 중** — `Object.entries(characterDistribution)`, `combo.tagValues`. plan 과 어긋난 형식이라도 동작.

### 본 PR 범위

- 외부 API 소비자 미발견 (단일 사용자 본인 프론트만 호출)
- plan 작성자(태형님) 가 직접 plan/구현 결정 가능

## 해결 옵션

### 옵션 A — plan 을 구현에 맞춰 갱신 (권장)

plan L400-401 을 현재 구현 형식으로 수정 + hitRate 미구현 명시 ("후속 작업").

| 장점 | 단점 |
|---|---|
| 변경 범위 최소 (plan 1개 파일) | hitRate 라는 plan 핵심 가치 일부 보류 |
| 프론트 무영향 | |
| API 안정성 유지 | |

### 옵션 B — 코드를 plan 에 맞춰 수정 (Backend + Frontend)

`DashboardResponse` 응답 형식 변경 (array + tags + hitRate) + DashboardResult / DashboardRepository 도 hitRate 계산 추가 + 프론트 stocknote.js / index.html 수정.

| 장점 | 단점 |
|---|---|
| plan 의 분석 가치 (character 별/combo 별 hitRate) 구현 | 변경 범위 광범위 (Backend 4 파일 + Frontend 2 파일) |
| | hitRate 쿼리 추가 (대시보드 응답 latency 증가) |

### 옵션 C — 절충 (필드명/형식만 정리, hitRate 제외)

`characterDistribution` 만 array 로 변경 + `tagValues` → `tags` 리네이밍. hitRate 는 별도 task.

| 장점 | 단점 |
|---|---|
| plan 에 부분 정합 | 프론트 변경 필요 |
| hitRate 는 후속으로 | 절반 변경 — 정합 완성도 약함 |

## 추천: 옵션 A

근거:
- 외부 API 소비자 0명 (본인 프론트만)
- plan 은 작성자 결정 가능 (태형님 직접)
- 본 PR 범위 최소화 → 다른 P1/P2 진행에 자원 배분
- hitRate 분석은 별도 plan 으로 가치 있는 후속 작업
- 프론트가 이미 동작 중인 형식 유지 → 회귀 위험 0

## 후속 task 와의 관계

| Task | 정합 |
|---|---|
| 신규 (별건 plan) | character/combo 별 hitRate 분석 — 사용자가 가치 인정하면 후속 plan |

## 코드 위치

| 파일 | 변경 |
|---|---|
| `docs/plans/2026-04-23-001-feat-stock-note-plan.md` L400-401 | 현재 구현 형식으로 갱신 + hitRate 후속 명시 |

## 설계 문서

[dashboard-contract-drift](../../../designs/stocknote/dashboard-contract-drift/dashboard-contract-drift.md)
