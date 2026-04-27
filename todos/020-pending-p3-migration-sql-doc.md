---
status: pending
priority: p3
issue_id: 020
tags: [code-review, deployment, documentation, portfolio]
dependencies: []
---

# 운영 마이그레이션 SQL 명시 부족 — `ddl-auto: update` 의존

## Problem Statement

plan L800에 "운영 배포 시 마이그레이션 SQL을 별도 검토"라고만 적혀있고 실제 SQL이나 별도 파일 링크 없음. 본 PR에서 `columnDefinition DEFAULT 'ACTIVE'/0`으로 ddl-auto:update 부팅 실패는 회피했으나, 운영 DB가 별도 migration 도구(Flyway 등)를 안 쓰면 절차 모호.

## Findings

- 위치: `docs/plans/2026-04-26-001-feat-portfolio-stock-sale-plan.md` (L795-797)
- 변경 영향:
  - `portfolio_item.status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'`
  - `portfolio_item.version BIGINT NOT NULL DEFAULT 0`
  - 신규 `stock_sale_history` 테이블

## Proposed Solutions

### Option A — PR description 또는 plan에 운영 1회 실행 SQL 명시
```sql
-- One-shot migration (run before first deploy with feat/portfolio-sale)
ALTER TABLE portfolio_item ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE portfolio_item ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
-- stock_sale_history는 ddl-auto:update가 신규 테이블이라 안전
```

### Option B — Flyway 도입 (별도 PR/스코프)

## Recommended Action

A 즉시 (배포 직전).

## Technical Details

- 영향 파일:
  - `docs/plans/2026-04-26-001-feat-portfolio-stock-sale-plan.md` 또는 PR description

## Acceptance Criteria

- [ ] 배포 절차에 마이그레이션 SQL 명시
- [ ] ddl-auto:update 부팅이 운영에서도 안전

## Work Log

- 2026-04-27: ce-review 발견 (architecture-strategist P3-1)