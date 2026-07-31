# 포트폴리오 목표 자산 배분 비율 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-07-30-asset-allocation-target-gates.md`로 참조한다.

## Stage Decisions
- start: approved
- brainstorm: approved (2026-07-30, 설계 선택 4건 확정 — 문서 확인 대기)
- issue: pending
- plan: pending
- work: pending
- review: pending
- validation: pending
- commit: pending
- push: pending

## Stage Log
- start: 2026-07-30, 태형님 "전체 금액 비율을 지정하고 해당 비율대로 투자, 안전/투자자산 비율 초과 시 금액·퍼센트 표시, 투자자산 내 자산군별 비율" — 기능 요청 접수, 기존 구조(AssetType 9종·PortfolioEvaluationService) 탐색
- brainstorm: 완료 (2026-07-30, docs/brainstorms/2026-07-30-asset-allocation-target-brainstorm.md)
  - 설계 선택 4건 질의 → 태형님 확정: 유형 고정 매핑 / 평가액 기준 / 허용밴드 초과 시 강조 / 투자자산 내부만 세부 비율
  - 추가 확정 3건 (2026-07-30): 시세 반영 확대는 금만(KRX 금시세 신규 연동) / 암호화폐는 배분 기능에서 제외(목표·집계·표시 미노출, 등록 기능 불변) / 대시보드 포트폴리오 카드에 배분 요약 추가

## Approval Gate 항목
- 신규 Entity(목표 배분 설정) 생성 필요 — plan 단계에서 상세 설계 후 승인 대상
- 신규 공개 API(설정 조회/저장, 배분 현황 조회) 추가 — plan 단계 승인 대상
- 신규 외부 연동(KRX 금시세) 추가 — plan 단계에서 데이터 소스·호출 방식 설계 후 승인 대상
- `PortfolioEvaluationService`에 GOLD 평가 추가(비즈니스 로직 동작 변경) — plan 단계 승인 대상
- worktree: 본 세션은 원격 실행 환경의 지정 브랜치 `claude/portfolio-auto-payment-check-b6ecy4` 사용 (`scripts/create-worktree.sh` 대체, 세션 하네스 제약)

## Notes
- 선행 맥락: 같은 세션에서 자동납입 기능 main 반영 여부 확인(읽기 전용). 본 작업은 별개 신규 기능.
- 2026-07-30: brainstorm·gate 문서는 세션 하네스(stop hook, 미추적 파일 금지) 제약으로 지정 브랜치에 선커밋·푸시. 기능 구현의 commit/push 단계 승인과는 별개이며 해당 단계는 pending 유지.
