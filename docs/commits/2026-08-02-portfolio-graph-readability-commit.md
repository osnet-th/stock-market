# 포트폴리오 그래프 가독성 개선 Commit 기록 (#107)

gate: docs/gates/2026-08-02-portfolio-graph-readability-gates.md

## 승인
- 태형님 "하나로 커밋 푸시 main 병합까지 진행해" (2026-08-02) — 단일 커밋·push·PR·main 병합 일괄 승인

## 포함 파일
- `src/main/resources/static/js/components/portfolio.js` — 도넛 범례 비활성, 편차 헬퍼 9종 신규, 미사용 헬퍼 3종 제거, `getSalesSummary()` 추가
- `src/main/resources/static/partials/portfolio.html` — 도넛 오버레이 정중앙, 비중 막대 radius, 배분 카드 다이버징 재구성, 매도 이력 합계 카드
- `docs/brainstorms/2026-08-02-portfolio-graph-readability-brainstorm.md`
- `docs/issues/2026-08-02-portfolio-graph-readability-issue.md`
- `docs/plans/2026-08-02-002-feat-portfolio-graph-readability-plan.md`
- `docs/works/2026-08-02-portfolio-graph-readability-work.md`
- `docs/reviews/2026-08-02-portfolio-graph-readability-review.md`
- `docs/validations/2026-08-02-portfolio-graph-readability-validation.md`
- `docs/commits/2026-08-02-portfolio-graph-readability-commit.md`
- `docs/pushes/2026-08-02-portfolio-graph-readability-push.md`
- `docs/gates/2026-08-02-portfolio-graph-readability-gates.md`

## 제외 파일
- 없음 (worktree 내 변경 전체 포함)

## 커밋 메시지
```
feat(portfolio): #107 그래프 가독성 개선 — 도넛 중앙 정렬·편차 중심 배분 카드·매도 합계 카드

- 도넛 캔버스 내장 범례 제거, 중앙 총평가 텍스트 원 중심 정렬 (자산 9종 시 겹침 해소)
- 자산 비중 미니 막대 rounded-full → rounded (소수% 뭉개짐 해소)
- 목표 자산 배분 카드를 0 기준 다이버징 편차 막대로 재설계
  (색=행동 방향, 만원 축약·행동 언어 라벨, 리밸런싱 요약 1줄, 허용밴드 음영)
- 매도 이력 탭 상단 전체 합계 카드 (건수·실입금·실현손익·실현 수익률)
- workflow 문서 일체 (brainstorm/issue/plan/work/review/validation/commit/push/gate)
```
