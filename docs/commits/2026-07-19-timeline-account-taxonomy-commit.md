# 재무 타임라인 taxonomy + 조 버림 Commit 기록

gate: docs/gates/2026-07-19-timeline-account-taxonomy-gates.md
issue: https://github.com/osnet-th/stock-market/issues/85
branch: fix/issue-85-timeline-account-taxonomy

## 승인
- 태형님 승인 (2026-07-19, "커밋 3개로 분리해서 진행해") — #85는 2개 커밋으로 분리(taxonomy / 조버림).

## 커밋 A — taxonomy 보강
메시지: fix(stock-financial): 연도별 DART 계정 taxonomy 불일치 값 누락 보강 — 영업CF·당기순이익·영업이익·매출액 (#85)
포함:
- src/main/java/.../stock/application/FinancialTimelineAssembler.java
- docs/brainstorms·issues·plans·works·reviews·validations·gates·commits/2026-07-19-timeline-account-taxonomy-*.md

## 커밋 B — 조 단위 표시 2자리 버림 (번들, 무관)
메시지: fix(ui): 금액 조 단위 표시 반올림 → 2자리 버림 — 종목평가·기업리포트 (#85)
포함:
- src/main/resources/static/js/utils/format.js (truncTo2 + compactNumber trunc 옵션)
- src/main/resources/static/js/components/company-report.js (crAmt)
- src/main/resources/static/js/components/stock-eval.js (fmtAmountByUnit)

## 제외 파일
- .env(비추적), 기타 없음.

## 비고
- 커밋 순서: A(taxonomy) → B(조버림).
- 푸시는 별도 게이트(미승인) — 커밋까지만.
