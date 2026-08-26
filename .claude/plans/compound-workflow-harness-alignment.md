# compound workflow harness alignment

## 작업 리스트
- [x] `CLAUDE.md`를 `brainstorm -> plan -> work -> review` 워크플로우로 재구성
- [x] `AGENTS.md`의 설계/분석 강제 규칙을 compound workflow 기준으로 치환
- [x] `compound-engineering.local.md`를 공식 컨텍스트 문서로 승격하고 레거시 analyze/design 경로 제거
- [x] 관련 `.claude/commands/*.md`의 설계 문서 강제 표현을 plan/brainstorm 기준으로 정리
- [x] 변경 문서 간 참조 우선순위와 용어 일관성 점검

## 배경
현재 저장소 문서는 과거 `analyze -> design -> approve -> implement` 흐름을 강제하지만,
실제 운영은 compound-engineering 기반의 `brainstorm -> plan -> work -> review` 흐름으로 진행됩니다.

## 핵심 결정
- `ARCHITECTURE.md`와 `compound-engineering.local.md`를 상위 컨텍스트로 둡니다.
- `docs/brainstorms/**`, `docs/plans/**`, `docs/solutions/**`를 공식 파이프라인 산출물로 취급합니다.
- `.claude/analyzes/**`, `.claude/designs/**`는 필수 게이트가 아닌 레거시 참고 자료로 낮춥니다.
