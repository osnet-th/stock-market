# 하네스 절대경로 하드코딩 제거 커밋 기록

gate: docs/gates/2026-09-27-harness-path-hardcoding-gates.md

- branch: issue/125-harness-path-hardcoding
- base: main 7fa0914

## 커밋
- `fix(harness): #125 저장소 절대경로 하드코딩 제거 — git 루트 기준 동적 산출`
  - 스크립트 5개, 문서 10개, `.gitignore`
  - 리뷰 F1~F3 조치 및 워크플로 산출물 9종 포함

## 제외한 변경
- `.claude/issues/125/` — worktree 작업 상태. 현재 스킴은 저장소에 커밋하지 않는 관행을 따른다.
- `docs/plans/2026-08-26-001-chore-agent-harness-adoption-plan.md` — 완료 상태 계획 문서의 `worktree:`는 검증기 절대경로 규칙 대상이라 원상 복구했다.

## 확인
- 커밋 메시지에 AI·도구 작성자 정보 없음.
- 빌드 산출물·비밀값 파일 미포함.
