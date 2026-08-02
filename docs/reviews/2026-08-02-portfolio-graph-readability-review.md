# 포트폴리오 그래프 가독성 개선 Review 기록 (#107)

gate: docs/gates/2026-08-02-portfolio-graph-readability-gates.md
plan: docs/plans/2026-08-02-002-feat-portfolio-graph-readability-plan.md
work: docs/works/2026-08-02-portfolio-graph-readability-work.md

셀프 리뷰 (2026-08-02). 대상: `static/partials/portfolio.html`, `static/js/components/portfolio.js`.

## Findings (심각도순)

### M1 — `formatKrwCompact` 억 단위 캐리 누락 (수정 완료)
- 증상: 만원 단위 반올림이 억 자리로 넘어가는 경계에서 오표시 — 99,996,000원 → "10,000만원", 199,995,000원 → "1억 10,000만원".
- 원인: `Math.round((abs % 1억) / 1만)`이 10000으로 반올림돼도 억 자리에 캐리되지 않음.
- 수정: 만원 단위로 선반올림한 뒤 억/만을 분리하는 방식으로 교체 (`portfolio.js` formatKrwCompact).
- 재검증: 브라우저 함수 호출 — 99,996,000 → "1억원", 199,995,000 → "2억원", 기존 케이스("996만원"·"1억 2,346만원"·"4,500원") 불변 확인.

### 그 외 명시적 findings 없음

## 수용한 관찰 (수정 안 함)

- L1: `getRebalanceSummary` 10줄·`formatDeviationLabel` 9줄 — 컨벤션 기본(5줄) 초과이나 guard clause 구조의 단일 책임 메서드로 상한(10줄) 이내, 파일 내 기존 관례와 일치.
- L2: "밴드 초과" 배지 제거 — 방향은 "부족/많음" 단어가, 밴드 상태는 색·볼드가 전달. 색 없이도 의미 전달 가능해 수용 (brainstorm 확정 사항).
- N1: 범례 제거로 도넛 카드 높이가 우측 카드보다 낮아짐 — 시각적 경미, plan 리스크 항목에서 예상한 범위.

## 회귀 위험 점검

- 제거 헬퍼 3종(`formatAllocDeviation`·`getAllocationBucketBarStyle`·`getAllocationTargetMarkerStyle`) 잔존 참조 없음 — `static/` 전체 grep 확인.
- 목표 설정 모달(`openAllocationTargetModal`)·대시보드 카드(`home.js`)·보유 자산 목록 무변경.
- 서버 응답 필드 사용은 기존과 동일 (`currentRatio`·`targetRatio`·`deviationAmount`·`deviationPctPoint`·`bandExceeded`·`bandPctPoint`).

## 누락 테스트

- 프로젝트 계약상 테스트는 명시 요청 시에만 작성. 프론트 JS 하네스 부재 — 브라우저 런타임 실측(작업·리뷰 단계)으로 대체, validation 단계에 기록.

## Open Questions / Assumptions

- 리밸런싱 요약의 조사 "으로"는 버킷명이 "안전자산/투자자산" 고정이라 항상 자연스러움 — 버킷명 변경 시 재검토.
- 허용밴드 표기는 소수 1자리 고정(`±5.0%p`).

## Change Summary

프론트 2개 파일: 도넛 내장 범례 제거·중앙 오버레이 정중앙화, 자산 비중 막대 radius 축소, 목표 배분 카드를 0 기준 다이버징 편차 막대 + 행동 언어 라벨 + 리밸런싱 요약으로 재구성. 서버 변경 없음.
