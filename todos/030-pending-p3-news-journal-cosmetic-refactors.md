---
status: pending
priority: p3
issue_id: 030
tags: [code-review, refactor, simplicity, newsjournal]
dependencies: []
---

# 가독성/단순화 잡 ─ Mapper 오버로드 명시화 + 검증 헬퍼 추출 + Link DTO 분리 + replaceAll 잔여 정리

## Problem Statement

다음 네 가지 가독성 단순화 항목을 한 todo 로 묶음. 모두 위험 없음, 효과 작음~중간.

## Findings

1. **NewsEventMapper 4개 오버로드 모호**: `toEntity(NewsEvent)` / `toEntity(NewsEventLink)` 동일 이름. 호출부에서 import만 보면 어느 매퍼인지 즉시 인지 어려움.
   - 위치: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/infrastructure/persistence/mapper/NewsEventMapper.java`

2. **`NewsEvent.create` / `updateBody` 검증 중복**: 동일한 7~8줄 검증 블록을 두 메서드가 반복.
   - 위치: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/domain/model/NewsEvent.java:60-101`

3. **`NewsEventLinkDto` 입출력 공용 의도 불명**: Bean Validation 어노테이션이 응답에서는 inert. 코드 의도 가독성 떨어짐.
   - 위치: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/presentation/dto/NewsEventLinkDto.java`

4. **`replaceAll` 의 `assignId` 호출이 사실상 dead side-effect**: 호출자가 반환값을 사용하지 않고 메서드도 void.
   - 위치: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/infrastructure/persistence/NewsEventLinkRepositoryImpl.java:58-64`

## Proposed Solutions

### Option A — 항목별 점진 적용 (Recommended)
1. Mapper: `toEventEntity / toEventDomain / toLinkEntity / toLinkDomain` 4개 명시 이름.
2. 검증: `private static void validateBody(...)` 헬퍼로 추출.
3. LinkDto: 후속 확장 시점에 `LinkRequestItem` / `LinkResponseItem` 분리. 현재는 코멘트로 의도 명시 (즉시 분리 안 함).
4. replaceAll: `assignId` 호출 제거 (반환 미사용).

### Option B — 일괄 PR 한 번에 처리
- 작은 변경들 합쳐 한 번에. 리뷰 코스트 줄지만 롤백 단위 큼.

## Recommended Action

A. 우선순위는 1 > 2 > 4 > 3.

## Technical Details

- 위험 없음. 단위 테스트 없는 환경(현 프로젝트)에서는 컴파일 + 부팅 검증.

## Acceptance Criteria

- [ ] Mapper 4개 메서드 이름 명시화 + 호출부 일괄 갱신
- [ ] `NewsEvent` 검증 블록 헬퍼 추출
- [ ] `replaceAll` `assignId` 제거
- [ ] (선택) LinkDto 분리 또는 의도 코멘트 추가

## Work Log

- 2026-04-28: 발견 (ce-review 단순성 #4, 아키텍처 P3, 단순성 #8)

## Resources

- code-simplicity-reviewer #4 (Mapper 오버로드)
- code-simplicity-reviewer #8 (검증 중복)
- architecture-strategist P3 #3 (LinkDto 분리)
- architecture-strategist P3 #5 (replaceAll assignId 제거)