# Notion Sync Guide

이 문서는 하네스 산출물을 Notion에 동기화하는 규칙이다. 최상위 계약은 [agent-harness.md](agent-harness.md)이며, 충돌 시 더 보수적인 규칙을 따른다.

---

## 목적과 대상

- 동기화 대상은 아래 4종이다.
  - Harness Brainstorm 문서 (`docs/brainstorms/`)
  - root plan 문서 (`docs/plans/`)
  - 리뷰 결과 (`/ce:review` 결론과 선택 이슈 수정 결과)
  - 구현 설명 (구현 이해 확인 Gate 산출 — 계층별 구현, 판정 정책, 저장 구조, 유지보수 포인트)
- 원칙은 문서별 "검토 요청 시점 생성 1회 + 확정 시점 업데이트 1회"다. 작성 중 드래프트를 반복 동기화하지 않는다.
- 위 4종 외 산출물(진행 상태, 수정 이력, 검증 증거, 분석 문서)은 동기화하지 않는다.

## 루트 페이지

- 위치: 프로젝트 플래너 > stock-market
- page_id: `3c87ae85-b3ab-80a4-a39b-d8db0fe3f49c`
- 모든 작업 페이지는 이 페이지 하위에만 생성한다.

## 페이지 구조와 네이밍

```text
stock-market
└── #{이슈번호} {이슈 제목}       # 비 이슈 작업은 {YYYY-MM-DD}-{topic}
    ├── (본문) 개요               # 확정된 요구사항 + 구현 방향성 요약 (상태 표기 금지)
    ├── Brainstorm               # 하위 페이지, brainstorm 수행 시에만
    ├── Plan                     # 하위 페이지, 다중 plan이면 "Plan n/m — {제목}"
    ├── Implementation           # 하위 페이지, 구현 이해 확인 Gate 진입 시 생성
    └── Review                   # 하위 페이지
```

- 작업 페이지 제목: `#{이슈번호} {이슈 제목}`. 비 이슈 작업은 `{YYYY-MM-DD}-{topic}`.
- 하위 페이지 제목은 `Brainstorm`, `Plan`, `Implementation`, `Review`로 고정한다.
- 한 작업에 plan이 여러 개면 `Plan n/m — {plan 제목}` 형식으로 만든다.

## 동기화 트리거

| 문서 | 생성 시점 | 업데이트 시점 |
|------|-----------|---------------|
| Brainstorm | 문서 작성 완료 후 태형님 확인 요청 시점. 작업 페이지 생성 + 개요 작성 + Brainstorm 페이지 생성 | 확인 반영 후 `/ce:plan` 진입 전 |
| Plan | `validate-plan.sh` 통과 후 승인 요청 시점. 태형님이 Notion에서 plan을 읽고 승인 판단할 수 있어야 한다 | 승인 과정 수정 반영 후 `draft -> active` 전환 시점 최종본 |
| Review | `/ce:review`와 선택 이슈 수정이 모두 끝난 시점 1회. 구현 이해 확인 Gate 진입 전 | 리뷰 재수행 시 |
| Implementation | 구현 이해 확인 Gate(`explain` 단계) 진입 시점. 구현 설명을 제시하는 시점에 페이지 생성 | Gate 진행 중 설명이 보완되면 즉시. 이후에는 코드가 바뀐 경우에만(병합 전 확인) |

## 상위 페이지 보장

- 모든 트리거는 동기화 시점에 작업 페이지가 없으면 먼저 생성하고 개요를 채운 뒤 하위 페이지를 만든다.
- brainstorm을 생략한 작업은 plan 승인 요청 시점에 작업 페이지가 처음 생성된다. 이때 개요는 이슈 본문 요약 + plan의 배경/핵심 결정으로 작성한다.
- Brainstorm 하위 페이지는 brainstorm을 수행한 경우에만 만든다.

## 페이지 내용 규칙

- 개요(작업 페이지 본문): GitHub 이슈 내용과 brainstorm에서 확정된 "요구사항"과 "구현 방향성"만 요약해 작성한다. brainstorm 생략 시 이슈 본문 요약 + plan의 배경/핵심 결정으로 대체한다.
- 개요에는 진행 상태를 쓰지 않는다. 금지 예: plan/승인/테스트 상태(`draft`, `test_plan_status`, `schema_plan_status` 등), 이슈 진행 상태, 일정/estimate, 작업 단계 체크리스트.
- Brainstorm: `docs/brainstorms/` 문서 본문을 Notion 형식으로 변환해 작성한다.
- Plan: root plan 문서 본문을 Notion 형식으로 변환해 작성한다. frontmatter는 옮기지 않는다.
- Review: `/ce:review` finding 표, 선택/보류 이슈, 수정 결과, GAP 결정 결과를 요약해 작성한다.
- Implementation: 구현 이해 확인 Gate에서 설명한 구현 내용을 옮긴다. 변경 요약, 요구사항 커버리지(REQ별 판정·구현 위치·충족 근거), 호출 체인, 계층별 클래스 카드(담당·입출력·처리 단계·핵심 규칙·구조 이유·변경 시 영향), 판정·정책 규칙, 데이터 저장 구조, 설정값과 상수, 응답·에러 계약, 확장 구조, 실패·부분 성공·트랜잭션, 유지보수 포인트를 포함한다. 검증 실행 결과·미실행 사유·잔여 위험과 GAP 결정 결과는 옮기지 않는다. GAP 결정은 리뷰 결과(Review 페이지)가 담당한다. 검증은 별도 산출물이며, 이 페이지는 현재 코드가 어떻게 동작하는지만 담는다.
- 로컬 md 파일 경로는 페이지에 쓰지 않는다.

## 갱신 규칙

- 확정, plan 이탈 재승인, 리뷰 재수행으로 내용이 바뀌면 기존 페이지를 업데이트한다.
- 같은 문서에 대한 신규 페이지 재생성은 금지한다.

## 기록 금지

- 토큰, 비밀번호, 인증 정보.
- 내부 작업용 md 파일 절대경로.
- [gates/github-issue-gate.md](gates/github-issue-gate.md)의 문서 경로 금지 기준과 동일하게 적용한다.

## 실패 처리

- Notion MCP 미연결 등으로 동기화가 실패하면 작업을 차단하지 않는다.
- plan frontmatter에 `notion_sync: pending`을 기록하고 진행한 뒤, 다음 세션 또는 연결 복구 후 보충 동기화한다.
- 보충 동기화가 끝나면 `notion_sync: pending` 표기를 제거한다.
