# 용어 사전 마스터-디테일 리디자인 Commit 기록

gate: docs/gates/2026-08-21-glossary-redesign-gates.md

Status: Done (2026-08-21)

## 승인

- 태형님 개별 승인 없음 — 원격 자율 세션의 포괄 승인 해석 (게이트 로그 '세션 특성' 참조).
  최종 수용은 푸시된 브랜치 검토로 판단.

## 커밋 구성 (2건)

### 1. feat(glossary) — 코드

포함 파일:
- `src/main/java/com/thlee/stock/market/stockmarket/glossary/**` (도메인/애플리케이션/영속성/DTO 12파일)
- `src/main/resources/db/migration/glossary_term_detail_fields_2026_08_21.sql`
- `src/main/resources/static/partials/glossary.html`
- `src/main/resources/static/js/components/glossary.js`
- `src/main/resources/static/css/custom.css`

메시지(요지): 용어 사전 마스터-디테일 리디자인 — 구조화 필드·함께 볼 용어·초성 통합 검색·
인라인 편집·초안 보관. 실측 수정 2건(FK NO_CONSTRAINT, @OrderColumn null 필터) 포함.

### 2. docs(glossary) — documented workflow 기록

포함 파일:
- `docs/gates/2026-08-21-glossary-redesign-gates.md`
- `docs/brainstorms/2026-08-21-glossary-redesign-brainstorm.md`
- `docs/issues/2026-08-21-glossary-redesign-issue.md` (bootstrap-exception — GitHub MCP 자격 오류, 사후 등록 예정)
- `docs/plans/2026-08-21-001-feat-glossary-redesign-plan.md`
- `docs/works/2026-08-21-glossary-redesign-work.md`
- `docs/reviews/2026-08-21-glossary-redesign-review.md`
- `docs/validations/2026-08-21-glossary-redesign-validation.md`
- `docs/commits/2026-08-21-glossary-redesign-commit.md`
- `docs/pushes/2026-08-21-glossary-redesign-push.md`

## 제외 파일

- 임시 스모크 테스트 (`GlossaryRelationSmokeTest.java`) — 실행 검증 후 삭제 (테스트 미작성 정책)
- 스크래치패드 하네스 일체 (저장소 외부)
- `build/**`, 로컬 PG 데이터 등 산출물

## 사전 점검

- `scripts/check-documented-workflow.sh --through push` PASS 후 커밋
- `git status` 로 의도 외 파일 미포함 확인
