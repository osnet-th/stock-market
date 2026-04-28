---
status: pending
priority: p2
issue_id: 028
tags: [code-review, simplicity, performance, newsjournal]
dependencies: [024]
---

# `WriteService.delete` 사전 조회 중복 — `affected` 검사로 단순화

## Problem Statement

`delete` 가 `findByIdAndUserId` 로 존재 검증 후 `deleteByIdAndUserId` 를 호출한다. 본체 삭제는 이미 `id+userId` 조건으로 IDOR 방어가 되며, 반환 행 수로 존재/권한 부재를 동시에 판정 가능. 사전 SELECT 1회 제거 가능.

## Findings

- 위치: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/application/NewsEventWriteService.java:59-64`
- 본 todo 는 todo 024 (DELETE 반환값 미검사) 와 같은 흐름을 다른 각도에서 본 것이라 함께 해결.

## Proposed Solutions

### Option A — 반환값 기반 단일화 (Recommended)
```java
linkRepository.deleteByEventId(id);
int affected = eventRepository.deleteByIdAndUserId(id, userId);
if (affected == 0) {
    throw new NewsEventNotFoundException(id);
}
```
- SELECT 1회 절감.
- `linkRepository.deleteByEventId` 는 본체가 없을 때도 no-op 이지만, 트랜잭션 롤백으로 무해.

### Option B — `findByIdAndUserId` 유지하되 자식 삭제 후 본체 affected 검사 추가
- 이중 방어. 약간의 비용 + 단순성 손실.

## Recommended Action

A. todo 024 와 묶어 분석/설계 한 문서로 처리.

## Technical Details

- `NewsEventRepository.deleteByIdAndUserId` 가 현재 `void` 반환. 포트 시그니처를 `int` 또는 `boolean` 으로 변경 필요.
- 호출자(서비스)만 영향.

## Acceptance Criteria

- [ ] 포트 `deleteByIdAndUserId` 반환 타입 `int` 또는 `boolean`
- [ ] 서비스 단일 트랜잭션 안에서 자식·본체 삭제 + affected 0 시 NotFound

## Work Log

- 2026-04-28: 발견 (ce-review 단순성 #2 + 보안 P2-3)

## Resources

- code-simplicity-reviewer #2
- 의존: todo 024
