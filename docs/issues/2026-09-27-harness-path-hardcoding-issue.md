# #125 하네스 스크립트·문서의 저장소 절대경로 하드코딩 제거

gate: docs/gates/2026-09-27-harness-path-hardcoding-gates.md

- issue_number: 125
- issue_url: https://github.com/osnet-th/stock-market/issues/125
- 등록: 2026-09-27
- 등록 사유: 이슈 #124 착수 절차가 `start-issue-worktree.sh` 실패로 차단됨

## 배경
하네스 스크립트 5개와 가이드 문서 11개에 존재하지 않는 절대경로가 박혀 있어 이슈 착수·체크포인트 가드·stage 리마인더 hook이 모두 실패하고 있었다.

## 확정 범위
- 스크립트의 저장소 루트를 git에서 동적 산출
- 상위 디렉터리·plans 경로를 산출된 루트에서 파생
- 문서의 실행 안내를 저장소 기준 상대경로로 변경

## 제외
- worktree 디렉터리·브랜치 이름 규칙 변경
- 스크립트 검사 로직·실행 순서·출력 형식 변경
- 하네스 정책 변경
- 완료 상태 계획 문서의 `worktree:` 메타데이터 (검증기 절대경로 규칙 대상)
