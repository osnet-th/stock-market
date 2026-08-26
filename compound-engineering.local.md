---
review_agents: [code-simplicity-reviewer, security-sentinel, performance-oracle, architecture-strategist]
plan_review_agents: [code-simplicity-reviewer]
workflow: start-brainstorm-issue-plan-work-review-validation-commit-push
---

# Compound Engineering Context

이 문서는 이 저장소에서 compound-engineering workflow를 적용할 때의 공식 컨텍스트입니다.

## Workflow Contract
- 표준 흐름은 `start -> brainstorm -> issue -> plan -> work -> review -> validation -> commit -> push`
- 모든 작업은 같은 흐름을 따르며, 차이는 산출물 보존 방식만 있다
- 각 단계 시작 전 태형님에게 무엇을 할지 제시하고, 다음 단계 진행 여부를 확인한다
- brainstorm 완료 후 plan 진입 전 대응 GitHub Issue를 확인하고, 없으면 brainstorm 내용으로 Issue를 등록한다
- documented workflow:
  - `docs/brainstorms/*.md`, `docs/issues/*.md`, `docs/plans/*.md`, `docs/works/*.md`, `docs/reviews/*.md`, `docs/validations/*.md`, `docs/commits/*.md`, `docs/pushes/*.md`, `docs/gates/*.md`를 사용
  - 로직, API, Entity, 구조 변경 또는 고리스크 작업에 적용
  - current plan은 `docs/plans/*.md`
  - 각 단계 산출물은 `gate: docs/gates/*.md`로 게이트 로그를 참조
  - worktree 생성은 Issue 번호를 포함해 `scripts/create-worktree.sh --issue <number> ...`로 진행
- lightweight workflow:
  - 같은 순서를 따르되 문서를 파일로 남기지 않을 수 있음
  - 오타, 명백한 컴파일 에러, 문서 수정, 국소적 비로직 변경에 적용
  - current plan은 현재 대화에서 명시된 작업 범위와 단계
  - 범위 확대나 해석 필요 시 documented workflow로 승격
- 공식 산출물 경로:
  - brainstorm: `docs/brainstorms/*.md`
  - issue: `docs/issues/*.md`
  - plan: `docs/plans/*.md`
  - work: `docs/works/*.md`
  - review: `docs/reviews/*.md`
  - validation: `docs/validations/*.md`
  - commit: `docs/commits/*.md`
  - push: `docs/pushes/*.md`
  - gate: `docs/gates/*.md`
  - solution: `docs/solutions/**/*.md`
- `.claude/analyzes/**`, `.claude/designs/**`는 레거시 참고 자료이며 신규 기본 경로가 아님

## Context Priority
- 사용자 최신 지시
- `ARCHITECTURE.md`
- `docs/policies/code-convention.md`
- `docs/policies/git-worktree.md`
- 현재 작업의 gate
- 현재 작업의 plan
- 현재 작업의 issue
- 관련 brainstorm
- 현재 작업의 work/review/validation/commit/push 산출물
- `docs/solutions/**`
- 레거시 analyze/design 문서

# Review Context

본 프로젝트는 Java 21 + Spring Boot 4 기반의 주식/경제지표 포트폴리오 플랫폼입니다.
리뷰 에이전트는 아래 컨벤션을 반드시 고려합니다.

## 아키텍처
- 단일 Gradle 모듈. 패키지 레이어: `presentation → application → domain / infrastructure`
- 레이어 책임/의존성 방향은 `ARCHITECTURE.md` 가 원본. 위반 의심 시 반드시 참조
- JPA Entity 는 연관관계 없이 **ID 기반 참조만** 허용. 연관관계 매핑 제안 금지
- DTO/Entity 경계: presentation DTO 는 application 에서만 생성 → domain/infra 로 누수 금지

## 코드 스타일 / 규칙
- 코드 컨벤션의 원본 기준은 `docs/policies/code-convention.md`
- worktree 사용 원본 기준은 `docs/policies/git-worktree.md`
- Lombok 필수 (`@Getter`, `@Setter`, `@RequiredArgsConstructor`). 수동 getter/setter 금지
- YAGNI 원칙 엄수. 이번 작업 범위에 없는 메서드/클래스/인터페이스 신규 생성 금지
- 테스트는 명시 요청 시에만. 코드 구현 시 테스트 가능성(의존성 주입 등) 확보만 요구
- 한국어 리뷰 코멘트 가능. 작업 중 업데이트, 리뷰 코멘트, 최종 응답에서 사용자 호칭은 "태형님"

## 외부 연동 / 캐시
- 외부 API: RestClient(동기) + WebClient(Gemini SSE 스트리밍)
- 캐싱: Caffeine. TTL/size 설정 일관성 확인
- Spring Security + JWT Stateless. dev 환경은 `permitAll`, 운영은 인증 강제

## 안전장치
- 문제 발견 시 즉시 수정 금지. 같은 `brainstorm -> plan -> work` 순서를 따르되, documented workflow면 `docs/brainstorms/`와 `docs/plans/`를 남기고 lightweight workflow면 현재 대화에서 정리
- `docs/plans/*.md`, `docs/solutions/*.md`, `docs/brainstorms/*.md` 는 파이프라인 산출물 — 삭제/무시 권고 금지
- 정적 프론트 스택은 Alpine.js + Tailwind. 신규 JS 라이브러리 도입 제안 시 사전 승인 필요 명시

## 포커스 영역
- 동시성/레이스(ReentrantLock, 캐시 put/evict 타이밍)
- 외부 스크래핑 실패 격리(카테고리/카드 단위 try-catch)
- 권한/레이트리밋 우회 가능성
- N+1, JPA batch_size=1000, open-in-view=false 전제 유지
