---
status: pending
priority: p2
issue_id: 039
tags: [code-review, agent-native, validation, newsjournal]
dependencies: []
---

# `NewsJournalController.findList` — categoryId 가 잘못된 값일 때 묵묵히 빈 결과 반환

## Problem Statement

외부 에이전트 또는 클라이언트가 잘못된 `categoryId` (타사용자 소유 / 존재하지 않는 id) 를 보내면 `findList` 가 0건을 반환하고 끝남. "사건이 없는지" vs "카테고리 id 가 잘못됐는지" 구분이 안 되어 디버깅 루프에 빠짐.

## Findings

- 위치 1: `src/main/java/.../newsjournal/presentation/NewsJournalController.java:62`
- 위치 2: `src/main/java/.../newsjournal/application/NewsEventReadService.java:50`

확인 보고:
- agent-native-reviewer P2 (#1)

## Proposed Solutions

### Option A — categoryId 존재 검증 후 400 응답 (권장)
- `findList` 진입 시 `categoryId != null` 인 경우 `categoryRepository.findByIdAndUserId(categoryId, userId)` 로 존재 검증.
- 없으면 400 (`BAD_REQUEST: 존재하지 않는 categoryId`) 또는 404.
- 장점: 디버깅 명확, IDOR 차단.
- 단점: 1쿼리 추가.

### Option B — 의도 명시 (현행 유지)
- "조용히 빈 리스트" 가 의도라면 OpenAPI `description` 에 명시.
- 장점: 변경 없음.
- 단점: 외부 에이전트 디버깅 부담.

## Recommended Action

(triage 후 결정. Option A 권장.)

## Technical Details

- 영향 파일: `NewsJournalController.java` 또는 `NewsEventReadService.java`

## Acceptance Criteria

- [ ] 잘못된 categoryId 가 명확한 에러 응답
- [ ] 자기 사용자의 정상 categoryId 는 영향 없음

## Work Log

- 2026-04-29: ce-review 발견 (agent-native P2)

## Resources

- `src/main/java/.../newsjournal/presentation/NewsJournalController.java`
- `src/main/java/.../newsjournal/application/NewsEventReadService.java`