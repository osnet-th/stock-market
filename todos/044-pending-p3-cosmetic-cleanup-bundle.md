---
status: pending
priority: p3
issue_id: 044
tags: [code-review, refactor, simplicity, newsjournal]
dependencies: []
---

# 잡 단순화 묶음 — `assignId` 부수효과 / 검증 상수 참조 / 카테고리 단독 CRUD 의도 명시

## Problem Statement

위험 없는 가독성/단순화 항목 4건을 한 todo 로 묶음. 모두 트레이드오프 인정 후 선택 가능.

## Findings

1. **`NewsEvent.assignId` / `NewsEventCategory.assignId` 부수효과**
   - 위치: `NewsEvent.java:112`, `NewsEventCategory.java:53`, 사용처 `NewsEventRepositoryImpl.save:31-33`, `NewsEventCategoryRepositoryImpl.save:25-27`
   - 문제: save 후 `toDomain(saved)` 으로 새 도메인을 만들어 반환하면서 동시에 입력 도메인의 id 도 변이. 호출자가 둘 중 무엇을 쓸지 헷갈림.
   - 출처: code-simplicity-reviewer P3

2. **검증 상수 참조 — `@Size(max = 50)` 대신 `@Size(max = NewsEventCategory.NAME_MAX_LENGTH)`**
   - 위치: `CreateNewsEventRequest.java:23`, `UpdateNewsEventRequest.java:23`
   - 문제: 도메인 상수와 DTO 어노테이션 매개변수가 분리되어 동기화 부담.
   - 출처: code-simplicity-reviewer P3

3. **카테고리 단독 CRUD 의도 명시 (OpenAPI tag/description)**
   - 위치: 설계 문서 `news-event-category.md:75-78` (보류 항목)
   - 문제: 외부 에이전트가 카테고리 rename/delete API 를 시도하다 막혀서 헤맴. 의도적 미제공임을 OpenAPI tag/description 에 한 줄 명시 권장.
   - 출처: agent-native-reviewer P3 (#5)

4. **응답 페이로드 — list 응답에 본문 (what/why/how) 항상 포함**
   - 위치: `NewsEventListResponse.java:29-52`
   - 문제: 페이지당 ~30KB. 1인 환경 + SSR 동일 도메인이라 무관하나, 무한 스크롤 도입 시 누적.
   - 출처: performance-oracle P3 (#5)

## Proposed Solutions

### Option A — 항목별 점진 적용
1. `assignId` 호출 제거 (`save` 가 `toDomain(saved)` 만 반환).
2. `@Size(max = NewsEventCategory.NAME_MAX_LENGTH)` 로 상수 참조.
3. OpenAPI tag description 에 "카테고리는 사건 저장 시 자동 생성, 단독 CRUD 미제공" 한 줄 명시.
4. 무한 스크롤 도입 시점에만 list 응답 본문 truncate 또는 lazy load.

### Option B — 보류 (모두 정보성)
- 모두 트레이드오프 인정 가능, 즉시 처리 불필요.

## Recommended Action

(Option A 의 1, 2 정도만 즉시 적용 권장. 3, 4 는 향후)

## Technical Details

- 영향 파일: 위 위치 참조

## Acceptance Criteria

- [ ] assignId 부수효과 제거 (선택)
- [ ] 검증 상수 참조 (선택)
- [ ] OpenAPI 보류 의도 명시 (선택)
- [ ] list 응답 페이로드 정책 결정 (선택)

## Work Log

- 2026-04-29: ce-review 발견 (simplicity P3, agent-native P3, performance P3)

## Resources

- `src/main/java/.../newsjournal/domain/model/NewsEvent.java`
- `src/main/java/.../newsjournal/domain/model/NewsEventCategory.java`
- `src/main/java/.../newsjournal/presentation/dto/Create/UpdateNewsEventRequest.java`
- `src/main/java/.../newsjournal/presentation/dto/NewsEventListResponse.java`