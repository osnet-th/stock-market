---
title: "chore: dataworks 에이전트 하네스 GitHub 기준 이식"
type: chore
issue: TBD
status: done
date: 2026-08-26
origin: dataworks docs/ai 하네스 (bigx 스킬·Jira·Bitbucket 종속 제거판)
branch: chore/agent-harness-adoption
worktree: /Users/thlee/Documents/personal/stock-market
test_plan_status: none
schema_plan_status: none
docs_only: true
allowed_paths:
  - docs/ai/**
  - scripts/**
  - .claude/settings.json
  - CLAUDE.md
  - AGENTS.md
  - docs/plans/2026-08-26-001-chore-agent-harness-adoption-plan.md
blocked_paths:
  - src/**
  - build.gradle
  - docker-compose.yml
  - .github/**
---

# chore: dataworks 에이전트 하네스 GitHub 기준 이식

## Overview

dataworks에서 운영 중인 단계 하네스(게이트 문서 + hook 스크립트 + stage 마커)를 stock-market에 이식한다. Jira/Bitbucket/CodeRabbit/Notion/bigx 스킬/팀 산출물 어댑터 종속부는 제거하거나 GitHub(gh CLI) 기준으로 치환한다.

## 확정된 결정

| 항목 | 결정 | 사유 |
|------|------|------|
| 작업 단위 | 이슈별 worktree (`../stock-market-issue-{N}`) | dataworks와 동일 운영감, 스크립트 수정량 최소, 병렬 작업 가능 |
| PR 후 자동 리뷰 게이트 | 없음 | 태형님 결정. 리뷰는 review 단계 `/ce:review`로 종결, 병합은 태형님 승인 → `gh pr merge` 직행 |
| bigx-* 스킬 | 전부 제외 | 리뷰 단계는 `/ce:review` 이슈 제시 → 태형님 선택 → 선택분만 수정으로 축소 |
| Notion 동기화 | dataworks와 동일(A안) 포함 | 태형님 결정. 루트 페이지는 프로젝트 플래너 > stock-market(page_id `3c87ae85-b3ab-80a4-a39b-d8db0fe3f49c`) 하위 고정 |
| 팀 산출물(adapter/sdd/phases) | 전부 제외 | 개인 프로젝트, 팀 공유 산출물 불필요 |
| 산출물 경로 | `docs/brainstorms`, `docs/plans` (기존 유지) | 이미 이 컨벤션으로 운영 중. `.claude/designs` 승인 프로세스는 하네스로 대체(기존 문서는 보존) |
| 이슈 완료 코멘트 | 생략 | 변경 요약은 PR 본문에 작성, 이슈는 PR `Closes #N`으로 자동 close — 중복 제거 |
| 이슈 착수 표시 | worktree 생성 + stage 마커 생성 + 이슈 본인 assign | GitHub 이슈에는 진행 상태가 없으므로 assign으로 갈음 |

## 단계 구조

stage 마커: `{worktree}/.claude/issues/{이슈번호}/stage`

| 순서 | 단계 | stage 값 | 핵심 의무 |
|------|------|----------|-----------|
| 0 | 진행(착수) | (마커 생성 시점) | `scripts/start-issue-worktree.sh {N}` — worktree 생성, 이슈 assign, stage=brainstorm |
| 1 | 브레인스토밍 | `brainstorm` | docs/brainstorms 문서 작성. 없이는 plan 진입 금지 |
| 2 | 기획 | `plan` → `plan-approval` | `/ce:plan` → docs/plans 초안(draft) → validate-plan → 실행 브리핑·plan-to-code 매핑 제시 → 승인 대기 |
| 3 | 구현 | `implement` | 승인 후 active 전환 → check-plan-conflicts → `/ce:work`, allowed_paths 안에서 한 번에 하나의 task, 큰 작업 단위마다 checkpoint-guard + 체크포인트 브리핑 |
| 4 | 리뷰 | `review` | `/ce:review` → actionable issue 제시 → 태형님 선택분만 plan 범위 안에서 수정 |
| 5 | 구현 이해 게이트 | `explain` | check-implementation-explainer 구조로 구현 설명 + 요구사항 커버리지 제시, 미충족 1건이라도 있으면 중단, 범위 제외·부분 충족은 건별 GAP 결정 |
| 6 | 검증 | `verify` | "검증은 어떤 걸로 실행할까요?" 필수. 후보: `./gradlew test`(단위테스트) / `bootRun`+curl 실검증 / 정적 확인 / 미실행 사유 기록 |
| 7 | PR | `pr` | 커밋(AI 작성자 정보 금지) → push → `gh pr create`(본문에 변경 요약 + `Closes #N`) |
| 8 | 병합 | `merge` | 태형님 승인 후 `gh pr merge`(방식은 병합 시점 선택) → worktree 정리 → main 최신화 |

dataworks 대비 변경: review에 포함돼 있던 구현 이해 게이트를 `explain` 단계로 분리, complete를 `pr`/`merge`로 분리, verify 검증 후보를 gradle 기준으로 교체.

## Notion 동기화 (dataworks 동일)

- 루트 페이지: 프로젝트 플래너 > stock-market (page_id `3c87ae85-b3ab-80a4-a39b-d8db0fe3f49c`). 모든 작업 페이지는 이 하위에만 생성.
- 작업 페이지 제목: `#{이슈번호} {이슈 제목}`, 비 이슈 작업은 `{YYYY-MM-DD}-{topic}`. 하위 페이지는 `Brainstorm`/`Plan`/`Implementation`/`Review` 고정.
- 원칙: 문서별 "검토 요청 시점 생성 1회 + 확정 시점 업데이트 1회". 진행 상태·수정 이력·검증 증거·분석 문서는 동기화하지 않는다.

| 문서 | 생성 시점 | 업데이트 시점 |
|------|-----------|---------------|
| Brainstorm | 작성 완료 후 확인 요청 시(작업 페이지+개요 함께 생성) | 확인 반영 후 plan 진입 전 |
| Plan | validate-plan 통과 후 승인 요청 시 | 승인 과정 수정 반영, draft→active 전환 시 최종본 |
| Review | `/ce:review`·선택 이슈 수정 종료 시 1회 (dataworks의 팀 컨벤션·시큐어코딩 절은 없음) | 리뷰 재수행 시 |
| Implementation | explain 단계(구현 이해 게이트) 진입 시 | Gate 중 설명 보완 시, 이후에는 코드 변경 시에만(merge 전 확인) |

## 작업 리스트

### 1. docs/ai 게이트 문서 생성 (dataworks 이식 + 치환)

- [x] 1-1. `docs/ai/agent-harness.md` — 골격 이식. Agent Contract에서 bigx·Jira·팀 어댑터 조항 제거, Notion 동기화 조항은 유지(notion-guide.md 참조), 단계 구조를 위 9단계로 교체, Gate Routing 표를 신규 문서 기준으로 재작성
- [x] 1-2. `docs/ai/brainstorm-harness.md` — 이식 (Jira 입력 언급을 GitHub 이슈로 치환)
- [x] 1-3. `docs/ai/briefing-harness.md` — 이식 (실행 브리핑·plan-to-code 매핑·체크포인트 3-1절 유지)
- [x] 1-4. `docs/ai/stop-gates.md` — 이식
- [x] 1-5. `docs/ai/test-planning-harness.md` — 이식 (test_plan_status 게이트 유지)
- [x] 1-6. `docs/ai/gates/planning-gate.md` — 이식 (DB Schema Review Gate 포함, PostgreSQL·JPA 기준으로 문구 조정)
- [x] 1-7. `docs/ai/gates/github-issue-gate.md` — jira-gate 대체 신규 작성: `gh issue view/edit --add-assignee`, 착수·완료 규칙(완료 코멘트 생략, PR Closes 자동 close)
- [x] 1-8. `docs/ai/gates/git-pr-gate.md` — gh 기반 재작성: 커밋 규칙(AI 작성자 정보 금지), push, `gh pr create`, merge 단계 절차. 자동 리뷰 게이트 조항 없음
- [x] 1-9. `docs/ai/gates/review-implementation-gate.md` — 이식 (bigx 컨벤션·시큐어코딩 병렬 점검 절 제거)
- [x] 1-10. `docs/ai/gates/verification-gate.md` — 이식 (검증 후보를 `./gradlew test` / `bootRun`+curl로 교체)
- [x] 1-11. `docs/ai/notion-guide.md` — 이식: 루트 페이지를 프로젝트 플래너 > stock-market(page_id `3c87ae85-b3ab-80a4-a39b-d8db0fe3f49c`)으로 교체, 페이지 제목 `#{이슈번호} {이슈 제목}`(비 이슈 `{YYYY-MM-DD}-{topic}`), Review 페이지 구성에서 팀 컨벤션·시큐어코딩 절 제거(`/ce:review` 결과만), Jira 언급을 GitHub 이슈로 치환, 실패 처리(`notion_sync: pending`)·기록 금지 규칙 유지

### 2. scripts 이식

- [x] 2-1. `scripts/harness-stage-reminder.sh` — ROOT=`/Users/thlee/Documents/personal`, worktree glob `stock-market-issue-*`, 티켓 패턴 `#?\d+`→이슈 번호, stage 경로 `.claude/issues/{N}/stage`, VALID_STAGES에 `explain`/`pr`/`merge` 추가, 단계 메시지에서 bigx·Jira·CodeRabbit 제거(Notion 동기화 의무는 brainstorm·plan-approval·review·explain·merge 단계 메시지에 유지)
- [x] 2-2. `scripts/checkpoint-guard.sh` — 경로 치환 이식 (`.claude/jira` → `.claude/issues`)
- [x] 2-3. `scripts/validate-plan.sh` — 경로 치환 이식, Jira 관련 검사 제거, 스키마 키워드·test_plan_status 게이트 유지
- [x] 2-4. `scripts/check-plan-conflicts.sh` — 단일 모듈 기준 파일 단위 충돌 검사로 단순화 (인자: 이슈번호)
- [x] 2-5. `scripts/start-issue-worktree.sh` — start-ticket-worktree 대체: `gh issue view {N}` 확인 → `git worktree add ../stock-market-issue-{N} -b issue/{N}-{slug}` → 본인 assign → stage=brainstorm 마커 생성
- [x] 2-6. `scripts/test-gate-reminder.sh` — 경로 치환 이식

### 3. hook 등록·래퍼 문서

- [x] 3-1. `.claude/settings.json` 생성 — UserPromptSubmit: harness-stage-reminder.sh, SessionStart: test-gate-reminder.sh (jira-ticket-skill-reminder는 이식하지 않음)
- [x] 3-2. `CLAUDE.md` 개정 — 최상단에 필수 하네스 절(agent-harness.md 참조) 추가, "설계 및 구현 프로세스"·"버그 및 문제 발견 시 프로세스" 절을 하네스 참조로 교체. 프로젝트 개요·빌드·아키텍처·테스트 규칙 절은 유지
- [x] 3-3. `AGENTS.md` 전면 재작성 — 현재 dataworks 내용이 복사된 stale 상태. CLAUDE.md와 동일한 참조 구조의 래퍼로 교체

### 4. 검증·정리

- [x] 4-1. 스크립트 단독 실행 검증 — stage 마커 임시 생성 후 harness-stage-reminder 출력 확인, checkpoint-guard·validate-plan 정상/오류 케이스 각 1회
- [x] 4-2. 본 plan 체크리스트 갱신, status 전환
- [x] 4-3. branch 커밋 → push → `gh pr create` → 태형님 승인 후 병합 (새 하네스 pr·merge 절차의 첫 적용)

## 주의사항

- dataworks 원본 문서·스크립트는 수정하지 않는다(읽기 전용 참조).
- `src/**` 등 애플리케이션 코드는 이번 작업에서 건드리지 않는다.
- 기존 `.claude/designs`·`.claude/analyzes`·`.claude/issues` 문서는 삭제·이동하지 않는다.
