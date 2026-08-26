---
date: 2026-05-03
module: favorite
problem_ref: .claude/analyzes/favorite/graph-toggle-not-firing/graph-toggle-not-firing.md
title: 관심지표 토글 버튼 disabled 영구 박힘 수정
---

# 관심지표 토글 버튼 disabled 영구 박힘 수정

## 배경

분석 문서 결론:
- `:disabled="card._displayModePending"` 가 `undefined` 값에 대해 `disabled` 속성을 부착함.
- 카드 DTO 에는 `_displayModePending` 필드가 없으므로 첫 클릭 전까지는 영영 undefined → 클릭 자체가 막힘.
- ECOS / GLOBAL × GRAPH / INDICATOR 의 4개 버튼 모두 동일 문제.

## 채택안

**(안1) 바인딩에 boolean 강제 변환 (`!!`) 적용**

이유:
- 변경 범위가 가장 작음 (HTML 4줄 수정).
- DTO 변경 없음, 백엔드 영향 없음, 다른 컴포넌트 영향 없음.
- Alpine 의 boolean attribute 바인딩 관례에 맞춤 (undefined 도 falsy 로 정상 처리).
- 기존 `_displayModePending` 의 의미(클릭 진행 중 잠금) 그대로 유지.

탈락 후보:
- (안2) `=== true` 비교: 동작 동일하나 의도 표현이 약함.
- (안3) JS 에서 카드 사전 초기화: 카드 객체 fetch 시점이 여러 곳(`loadHomeSummary`, `getEnrichedFavorites` 갱신 등)이라 누락 위험. DTO 의미를 클라이언트 상태로 오염.

## 변경 사항

`src/main/resources/static/index.html` 4개소만 수정:

| 라인 | 변경 전 | 변경 후 |
|---|---|---|
| 427 | `:disabled="card._displayModePending"` | `:disabled="!!card._displayModePending"` |
| 476 | `:disabled="card._displayModePending"` | `:disabled="!!card._displayModePending"` |
| 560 | `:disabled="card._displayModePending"` | `:disabled="!!card._displayModePending"` |
| 614 | `:disabled="card._displayModePending"` | `:disabled="!!card._displayModePending"` |

JS / Java / DTO 변경 없음.

## 작업 리스트

- [x] `index.html:427` (ECOS GRAPH 카드) `:disabled` 바인딩 수정
- [x] `index.html:476` (ECOS INDICATOR 카드) `:disabled` 바인딩 수정
- [x] `index.html:560` (GLOBAL GRAPH 카드) `:disabled` 바인딩 수정
- [x] `index.html:614` (GLOBAL INDICATOR 카드) `:disabled` 바인딩 수정
- [ ] 브라우저 강력 새로고침 후 토글 동작 검증

## 테스트 계획

수동 검증 (테스트 코드 미작성):
1. 강력 새로고침 후 토글 버튼이 활성 상태로 렌더링되는지 (`document.querySelector('button.right-9').disabled === false`)
2. INDICATOR → GRAPH 토글 시 PUT 요청 1회 발생, 응답 후 GRAPH 영역으로 카드 이동
3. GRAPH → INDICATOR 토글 시 동일 동작 (역방향)
4. 연타 시 첫 요청만 처리, 후속 클릭은 in-flight 동안 disabled 로 차단

## 주의사항

- 검증용 강제 클릭 시 `Alpine.$data(body).homeSummary.enrichedFavorites.ecos[0]._displayModePending = false` 로 설정 후 클릭하면 단발 동작 가능 (전체 수정 전 임시 검증용).
- 본 변경 후에도 DTO 에는 `_displayModePending` 추가하지 않음 (UI-only 상태).