# AGENTS.md

이 파일은 Codex 계열 에이전트가 이 저장소에서 작업할 때 가장 먼저 읽는 래퍼 문서입니다.

## 필수 하네스

- 모든 작업 제어 규칙은 [docs/ai/agent-harness.md](docs/ai/agent-harness.md)를 따른다.
- 작업 시작 전 반드시 [docs/ai/agent-harness.md](docs/ai/agent-harness.md)를 읽고 적용한다.
- 이 문서와 공통 하네스 문서가 충돌하면 더 보수적인 규칙을 따른다.
- `AGENTS.md`와 `CLAUDE.md`는 동일한 참조 구조를 유지한다.
- 공통 정책 변경은 [docs/ai/agent-harness.md](docs/ai/agent-harness.md)에 반영한다.
- 래퍼 문서 변경이 필요하면 `AGENTS.md`와 `CLAUDE.md`에 동일하게 반영한다. 의도적으로 차이를 둘 경우 plan에 차이, 사유, 승인 여부를 명시한다.

## 빠른 확인

- 응답 시 항상 "태형님"이라고 호칭한다.
- 코드 구현이나 수정은 승인된 `docs/plans/` plan 문서가 있을 때만 진행한다. 오타·컴파일 에러·문서 수정·로직 무변경 국소 수정은 agent-harness의 lightweight workflow로 대화 승인 후 진행할 수 있다.
- 작업 흐름은 착수 → brainstorm → plan/plan-approval → implement → review → explain(구현 이해 게이트) → verify → pr → merge 단계를 따르고, 단계는 `{worktree}/.claude/issues/{이슈번호}/stage` 마커로 관리한다.
- 비 이슈로 시작한 documented 작업은 brainstorm 완료 후 plan 진입 전에 GitHub 이슈 등록 여부를 확인한다(github-issue-gate 이슈 등록 Gate).
- issue 착수는 `scripts/start-issue-worktree.sh {이슈번호}`, PR/병합은 `gh` CLI를 사용한다. 병합은 태형님 승인 후에만 수행한다.
- 커밋 메시지와 PR 본문에는 AI/도구 작성자 정보(`Co-Authored-By` 등)를 포함하지 않는다.
- brainstorm/plan/리뷰 결과/구현 설명 4종은 [docs/ai/notion-guide.md](docs/ai/notion-guide.md)에 따라 Notion(프로젝트 플래너 > stock-market)에 동기화한다.

## 프로젝트 참고

- 프로젝트 개요, 기술 스택, 개발 규칙, 테스트 가이드는 [CLAUDE.md](CLAUDE.md)를 따른다.
- 패키지 구조, 계층 규칙, 의존성 방향은 [ARCHITECTURE.md](ARCHITECTURE.md)를 따른다.
- 문서 작성 규칙은 [MD_WRITE_GUIDE.md](MD_WRITE_GUIDE.md)를 따른다.
