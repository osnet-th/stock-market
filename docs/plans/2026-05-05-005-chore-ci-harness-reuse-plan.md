# CI 하네스 재사용 계획

## 작업 리스트
- [x] `scripts/run-harness-checks.sh` 추가
- [x] `.githooks/pre-push`를 공용 엔트리포인트 호출 방식으로 단순화
- [x] `.github/workflows/harness-checks.yml` 추가
- [x] documented workflow 검사 스크립트가 CI에서 동작하도록 checkout/fetch 조건 정리
- [x] 셸 문법 검증과 workflow 기본 구조 점검

## 구현 범위
- 신규 스크립트: `scripts/run-harness-checks.sh`
- 신규 workflow: `.github/workflows/harness-checks.yml`
- 수정: `.githooks/pre-push`

## 주의사항
- CI에서는 worktree 종류 검사를 하지 않는다.
- CI는 documented workflow 문서 존재 검사만 재사용한다.
