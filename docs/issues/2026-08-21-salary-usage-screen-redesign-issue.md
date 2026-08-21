# 월급 사용 비율 화면 목업 기반 재설계 Issue 기록

gate: docs/gates/2026-08-21-salary-usage-screen-redesign-gates.md

## GitHub Issue
- status: created (2026-08-21, 세션 후반 GitHub 인증 복구 후 등록 — 구현 완료 뒤 소급 등록)
- issue_number: 116
- issue_url: https://github.com/osnet-th/stock-market/issues/116
- title: [enhancement] 월급 사용 비율 화면 목업 기반 재설계 — 카테고리 하위 항목·예산·일괄 저장·SVG 추이 3모드
- label: enhancement

## 근거
- brainstorm: docs/brainstorms/2026-08-21-salary-usage-screen-redesign-brainstorm.md (Status: Decided)
- 태형님이 Claude Design 목업(단일파일 HTML) 첨부 — "월급 사용 비율 화면을 첨부한 html 파일과
  동일하게 맞춰주고 부족한 기능은 백엔드 코드 수정해서 동작하도록 해줘" (원격 자율 세션 지시)

## Branch
- branch: claude/salary-usage-ratio-screen-udb2mz (원격 세션 지정 브랜치)
- base: main
- worktree: 해당 없음 (Claude Code 원격 컨테이너 — 지정 브랜치 단독 사용)

## 작업 범위 요약
1. 백엔드 — 카테고리 하위 항목 모델(spending_item_set / spending_item, 월 단위 스냅샷 상속),
   spending_config.budget 추가, 항목 보유 카테고리의 amount=항목 합계 파생 저장
2. 백엔드 — `PUT /api/salary/monthly/{yearMonth}` 일괄 저장(월급+카테고리 금액·예산+항목 세트,
   NOOP 의미론 유지), 월별 응답에 items/budget/previous(전월 요약) 확장,
   trend 포인트에 categoryTotals 추가
3. 프론트 — salary 화면을 목업과 동일 구성으로 재구현: 요약 카드 3장(실수령액+배분 리본 /
   저축률 다크 카드 / 고정·변동), 계층 사용처 테이블(접기/펼치기·항목 추가/삭제·고정/변동 토글·
   예산 열 토글), dirty 추적 일괄 저장/되돌리기, 지난달 복사, SVG 추이 3모드(비율/금액/구성),
   지난달 대비 diverging 바, 눈에 띄는 것 인사이트
4. 프론트 — Chart.js 도넛/바/라인 제거(SVG 직접 렌더로 대체), app.js/api.js 연동 갱신

## 범위 제외 (후속 이슈 제안)
- 커스텀 카테고리 추가/삭제 (`+ 카테고리`, 카테고리 ×) — SpendingCategory enum 고정 8종 유지.
  enum→사용자 정의 테이블 전환 + 데이터 이관 규모라 별도 이슈로 분리.

## 참고 자료
- 목업: 업로드 파일 `49eb149f-….html` (Claude Design 번들, template + DCLogic 추출 분석 완료)
- 선행 이슈: #114 (홈 리디자인 — dc-* 토큰·IBM Plex 폰트 도입), #110 (포트폴리오 리디자인)
- 홈 우측 패널(home-side)이 `GET /api/salary/monthly` 응답을 소비 — 응답 확장은 additive 로만.
