---
title: "fix: 하네스 저장소 절대경로 하드코딩 제거"
type: fix
issue: 125
issue_url: https://github.com/osnet-th/stock-market/issues/125
status: done
date: 2026-09-27
branch: issue/125-harness-path-hardcoding
worktree: /Users/tang/Documents/workspace/stock-market-issue-125
gate: docs/gates/2026-09-27-harness-path-hardcoding-gates.md
brainstorm: docs/brainstorms/2026-09-27-harness-path-hardcoding-brainstorm.md
issue_doc: docs/issues/2026-09-27-harness-path-hardcoding-issue.md
test_plan_status: none
schema_plan_status: none
docs_only: true
allowed_paths:
  - scripts/**
  - docs/ai/**
  - docs/analyzes/**
  - .gitignore
  - docs/plans/2026-09-27-001-fix-harness-path-hardcoding-plan.md
blocked_paths:
  - src/**
  - build.gradle
  - docs/plans/2026-08-26-001-chore-agent-harness-adoption-plan.md
---

# 하네스 저장소 절대경로 하드코딩 제거 (#125)

## 작업 리스트
- [x] `start-issue-worktree.sh` — `ROOT`/`PARENT` 동적 산출
- [x] `harness-stage-reminder.sh` — `_repo_root()` 추가, `ROOT`/`WORKTREE_PARENT` 동적 산출
- [x] `checkpoint-guard.sh` — `ROOT` 동적 산출
- [x] `test-gate-reminder.sh` — `PLANS_DIR` 동적 산출
- [x] `validate-plan.sh` — usage 예시 문자열 상대경로화
- [x] `docs/ai/` 8개 문서 실행 안내 상대경로화
- [x] 절대경로 실행을 전제하던 문장 정정
- [x] 과거 분석 문서 경로 표기 정리
- [x] `.gitignore`에 `.context/compound-engineering/` 추가
- [x] 문법 검사·루트 산출 검증·스모크 테스트

## 요구사항 원장

| ID | 요구사항 | 출처 |
| --- | --- | --- |
| REQ-1 | 저장소를 어느 경로에 두더라도 하네스 스크립트가 동작한다 | 이슈 #125 완료 조건 |
| REQ-2 | 메인 저장소와 worktree 양쪽에서 실행해도 같은 메인 저장소 루트를 가리킨다 | 이슈 #125 완료 조건 |
| REQ-3 | `scripts/`와 `docs/ai/`에 하드코딩된 절대경로가 남지 않는다 | 이슈 #125 완료 조건 |
| REQ-4 | 이슈 #124 착수 절차가 정상 수행된다 | 이슈 #125 완료 조건 |
| REQ-5 | worktree·브랜치 이름 규칙과 스크립트 검사 로직을 바꾸지 않는다 | 이슈 #125 하지 않는 것 |
| REQ-6 | git 조회 실패 시 기존 가드가 그대로 동작하고 새로운 실패 모드를 만들지 않는다 | brainstorm 권장 접근 |

## 목표와 범위
저장소 위치와 무관하게 하네스 스크립트가 동작하게 한다. 이름 규칙, 검사 로직, 출력 형식, 하네스 정책은 바꾸지 않는다.

## 핵심 결정
- 루트 산출은 `git rev-parse --git-common-dir` 기준이다. worktree 안에서 실행해도 메인 저장소 루트를 얻는다.
- git 조회 실패 시 빈 값이 되어 기존 가드가 그대로 동작한다. 새로운 실패 모드를 만들지 않는다.
- 완료 상태 계획 문서의 `worktree:`는 `validate-plan.sh`가 절대경로를 요구하므로 대상에서 제외한다 (`blocked_paths`).

## 주의사항
- `harness-stage-reminder.sh`는 매 턴 실행되는 hook이므로 루트 산출 실패가 예외로 번지지 않아야 한다. `try/except`로 감싸고 실패 시 빈 문자열을 반환한다.
- worktree 안의 스크립트 사본은 메인보다 오래됐을 수 있다. 문서에서 "절대경로로 실행"을 요구하던 문장은 "메인 저장소의 것을 실행하며 스크립트가 루트를 스스로 판별한다"로 정정한다.
