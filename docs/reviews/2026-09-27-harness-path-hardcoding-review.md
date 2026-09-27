# 하네스 절대경로 하드코딩 제거 리뷰

gate: docs/gates/2026-09-27-harness-path-hardcoding-gates.md

## Findings

| # | 심각도 | 내용 | 조치 |
| --- | --- | --- | --- |
| F1 | high | 완료 상태 계획 문서의 `worktree:` 값을 비경로 문자열로 바꿔 `validate-plan.sh`의 "worktree must be an absolute path" 규칙을 위반. 해당 문서 검증이 실패하게 됨 | 수정 — 해당 문서를 원상 복구하고 `blocked_paths`에 명시 |
| F2 | medium | 문서 일괄 치환으로 "하네스 스크립트는 절대 경로로 실행한다" 문장이 남아, 상대경로로 바뀐 다른 안내와 모순 | 수정 — "메인 저장소의 것을 실행하며 스크립트가 루트를 스스로 판별한다"로 정정 |
| F3 | low | `.gitignore` 말미에 개행이 없어 append 시 `.env-local### Compound Engineering ###`로 한 줄에 붙음 | 수정 — 개행 삽입 후 메인 작업트리 사본과 내용 일치 확인 |

## 보류
없음.

## 검토 관점
- 새로운 실패 모드가 생기지 않는지: git 조회 실패 시 빈 값 → 기존 가드가 처리. 확인.
- hook 안정성: 매 턴 실행되는 `harness-stage-reminder.sh`의 루트 산출을 `try/except`로 감쌈. 확인.
- 범위 이탈: worktree·브랜치 이름 규칙, 검사 로직, 출력 형식 미변경. 확인.
