---
status: pending
priority: p3
issue_id: 042
tags: [code-review, agent-native, documentation, newsjournal]
dependencies: []
---

# OpenAPI `@Schema` 어노테이션 보강 — enum 한글 라벨 / 페이지 의미 / nullable 의도

## Problem Statement

API 가 외부 에이전트에게 노출될 때 enum 의미 / 페이지 규약 / nullable 의도가 schema 에 반영되지 않아 discoverability 약함. UI 는 코드로 매핑하므로 무관하나 외부 클라이언트가 한국어 라벨, 페이지 0-base, category nullable 여부 등을 알 수 없음.

## Findings

- `EventImpact` 의 한글 라벨(호재/악재/중립)이 enum 코드와 분리되어 schema 에 안 노출
- `NewsEventListResponse.{totalCount, page, size}` 의 0-base / max=200 의미 미문서
- `CategoryDto category` 필드는 backfill 후 사실상 항상 non-null 인데 schema 에 nullable 의도 미명시
- 분류명 매칭 규칙 (case-sensitive, trim, find-or-create, max 50) 이 OpenAPI 에 미노출

확인 보고:
- agent-native-reviewer P2 (#3) + P3 (#4)

## Proposed Solutions

### Option A — `@Schema` 어노테이션 보강
- `EventImpact` 에 `@Schema(description = "GOOD=호재, BAD=악재, NEUTRAL=중립")`
- `CreateNewsEventRequest.category` / `UpdateNewsEventRequest.category` / `CategoryDto.name` 에 `@Schema(description = "사건 주제 분류명. trim 후 case-sensitive 매칭, find-or-create. 최대 50자.")`
- `NewsEventListResponse` 의 페이지 필드에 `@Schema(description = "page 는 0-base, size 는 1~200")`
- 장점: springdoc 자동 노출, 외부 에이전트 친화.
- 단점: 어노테이션 추가만큼 코드 늘어남.

### Option B — 별도 API 가이드 문서
- `docs/api/news-journal.md` 신규.
- 장점: 자유 형식.
- 단점: 코드와 문서 동기화 부담.

## Recommended Action

(Option A 권장 — springdoc 가 dev 에서 활성화되어 있다면 즉시 효과)

## Technical Details

- 영향 파일: `EventImpact.java`, `Create/UpdateNewsEventRequest.java`, `CategoryDto.java`, `NewsEventListResponse.java`

## Acceptance Criteria

- [ ] swagger-ui 에서 enum 한글 라벨 노출
- [ ] 페이지 / nullable / 매칭 규칙 문서화

## Work Log

- 2026-04-29: ce-review 발견 (agent-native P2 #3, P3 #4)

## Resources

- `src/main/java/.../newsjournal/domain/model/EventImpact.java`
- `src/main/java/.../newsjournal/presentation/dto/*.java`