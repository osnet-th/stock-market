# git worktree 정책 문서 추가 계획

## 작업 리스트
- [x] `docs/policies/git-worktree.md` 작성
- [x] worktree 사용 기준, 생성 규칙, 작업 규칙, 종료 규칙, 금지 사항 정의
- [x] `CLAUDE.md`에 git worktree 정책 문서 참조 추가
- [x] `AGENTS.md`에 작업 격리 규칙 참조 추가
- [x] `compound-engineering.local.md`에 workflow 컨텍스트 참조 추가

## 구현 범위
- 신규 문서: `docs/policies/git-worktree.md`
- 참조 변경: `CLAUDE.md`, `AGENTS.md`, `compound-engineering.local.md`

## 주의사항
- 상위 하네스 문서는 세부 사용법을 길게 적지 않고 정책 문서를 참조만 한다.
- documented workflow와 lightweight workflow의 차이를 worktree 사용 기준에도 일관되게 반영한다.
