---
status: pending
priority: p2
issue_id: 024
tags: [code-review, security, audit, newsjournal]
dependencies: []
---

# DELETE 반환 행 수 미검사 + 감사 로그 부재

## Problem Statement

`NewsEventWriteService.delete` 가 본체 삭제 결과(`int deleteByIdAndUserId` 반환값)를 검사하지 않는다. 또한 hard delete 만 지원하며 감사 테이블/로그가 없어 사고 발생 시 복구 경로가 없다.

## Findings

- 서비스: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/application/NewsEventWriteService.java:58-64`
- 리포지토리: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/infrastructure/persistence/NewsEventJpaRepository.java:48-50`
- 흐름: `findByIdAndUserId` 로 사전 조회 → `deleteByEventId(links)` → `deleteByIdAndUserId(event)`. 사전 조회와 실제 삭제 사이 경합 시 0건 삭제가 가능하나 호출자에게 통지되지 않음.
- 단순화 에이전트도 사전 조회 중복으로 지적 (todo 028 와 연계 — 함께 해결).

## Proposed Solutions

### Option A — 반환값 검사로 단일화 (Recommended)
- 사전 조회 제거. `int affected = jpaRepository.deleteByIdAndUserId(id, userId)` → `affected == 0` 이면 `NewsEventNotFoundException`.
- 자식 `linkRepository.deleteByEventId(id)` 는 본체 삭제 전에 호출하되, 단일 트랜잭션 안에서 본체 0건 시 롤백.
- 또는 선검증 유지 + 본체 affected 추가 검증으로 이중 안전.

### Option B — Soft delete 도입
- `deleted_at` 컬럼 추가. 모든 조회에 `deleted_at IS NULL` 필터.
- 복구/감사 가능. 다만 본 프로젝트 다른 도메인 정책과 일관성 필요.

### Option C — 감사 테이블 별도
- `news_event_audit(event_id, user_id, action, snapshot, occurred_at)` 신설. delete 시 spec 보존.
- 사후 복구 가능. 추가 테이블 + insert 부하.

## Recommended Action

A 적용 (즉시). B/C 는 정책 차원 별도 검토.

## Technical Details

- 변경: `NewsEventWriteService.delete`, 트랜잭션 안에서 자식·본체 순서.
- todo 028 (delete 사전조회 중복) 와 통합 적용 가능.

## Acceptance Criteria

- [ ] `delete(존재하지 않는 id, userId)` → `NewsEventNotFoundException`
- [ ] `delete(타사용자 id, userId)` → `NewsEventNotFoundException`
- [ ] 본체와 자식 모두 단일 트랜잭션에서 일관 삭제 (실패 시 롤백)

## Work Log

- 2026-04-28: 발견 (ce-review 보안 P2-3 + 단순성)

## Resources

- security-sentinel 보고 P2-3
- code-simplicity-reviewer #2 (delete 사전조회 중복)