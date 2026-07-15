# 종목평가 추정 손익 표 가독성 개선 Review

gate: docs/gates/2026-07-12-stock-eval-estimate-income-table-gates.md

## Findings (심각도 순)

명시적 findings 없음 (버그·회귀·설계 위반·컨벤션 위반 없음). 아래는 남은 리스크/가정.

- (info) 색 관례: 증가 ▲ text-red-500 / 감소 ▼ text-blue-500는 `js/utils/format.js:22-24` 기존 관례와 일치. 신규 색 도입 아님.
- (info) generic 분기 마크업은 기존 표 마크업을 그대로 복제 → 기본정보·추정 투자지표 렌더 불변(회귀 없음). 라이브 렌더로 확인.
- (low) 전용 표는 `sec.title === '추정 손익'` 정확 일치에 의존. 백엔드가 섹션 제목을 바꾸면 조용히 generic으로 폴백(깨지지 않음, degrade). 백엔드는 이번 변경 대상 아님.
- (low) 6행을 (금액,증감률) 교대 순서로 가정하고 쌍짓기. 짝수 행 가드는 있으나 순서가 뒤바뀌면 지표 라벨/증감률 매칭이 어긋날 수 있음. 현재 백엔드 `metricTable`이 항상 (금액→증감률) 순서로 생성하므로 유효.

## Open Questions / Assumptions

- 가정: output2 행은 항상 (매출/영업/순이익) × (금액,증감률) 6행, 기간 열은 "YYYY.MM[E]".
- 가정: 증감률 ÷10 정규화는 계속 백엔드(`scaleBy10`)가 담당(프론트는 표시만).

## Change Summary

- 프론트 2파일 수정: `stock-eval.js`(표시 헬퍼 6종 추가) + `stock-eval.html`(추정 손익 섹션 전용 표 분기).
- 백엔드/Entity/API/엔드포인트/DTO 무변경. 재무·신용·일정 탭 무영향.
