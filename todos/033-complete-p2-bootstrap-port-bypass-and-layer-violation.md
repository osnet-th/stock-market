---
status: complete
priority: p2
issue_id: 033
tags: [code-review, architecture, layer-violation, newsjournal]
dependencies: [032]
---

# `NewsJournalBootstrap` 가 Spring Data 직접 의존 + use-case 가 infrastructure 패키지에 위치

## Problem Statement

ARCHITECTURE.md 의 "infrastructure 는 domain.repository 인터페이스 구현" 패턴 위반. Bootstrap 클래스가 use-case 책임(트랜잭션 + backfill 비즈니스 규칙)을 수행하면서 도메인 포트를 우회하고 Spring Data 인터페이스에 직접 의존.

## Findings

- 위치: `src/main/java/.../newsjournal/infrastructure/bootstrap/NewsJournalBootstrap.java:33`

```java
private final NewsEventJpaRepository eventJpaRepository;  // Spring Data 직접 의존
...
@Transactional
public void run(ApplicationArguments args) { ... }  // 트랜잭션 경계는 application 소유 규칙
```

문제:
1. application 레이어 의도(use-case)를 infrastructure 패키지에서 수행 → 책임 경계 모호
2. 도메인 포트 우회 → Spring Data 인터페이스에 직접 결합 → 테스트/포트 시그니처 변경 시 취약

확인 보고:
- architecture-strategist P1 (#1)

## Proposed Solutions

### Option A — application 으로 use-case 추출 (권장)
- `application/bootstrap/NewsJournalBackfillService` 신규: 비즈니스 규칙 보유, 도메인 포트만 사용.
- `NewsEventRepository` 포트에 `findUserIdsWithMissingCategory()`, `assignCategoryToMissing(userId, categoryId)` 두 메서드 추가.
- `NewsJournalBootstrap` 은 infra 의 ApplicationRunner 어댑터로 축소, Service 위임만.
- 장점: 레이어 일관성, 테스트 가능성.
- 단점: 클래스 1~2 개 추가.

### Option B — 클래스 자체 삭제 (032 와 함께)
- 032 가 Option A (Bootstrap 삭제 + Seed SQL 만 사용) 로 결정되면 본 항목 자동 해소.
- 장점: 가장 단순.
- 단점: 운영자 수동 SQL 의존.

### Option C — 현행 유지 + javadoc 으로 한계 명시
- "운영 backfill 후 제거 예정" 명시 + 제거 일정 issue/TODO 등록.
- 장점: 변경 없음.
- 단점: 레이어 위반 잔존.

## Recommended Action

**자동 해소** — 032 가 Option A (Bootstrap 클래스 삭제) 로 결정되어 본 항목의 위반 자체가 사라짐. `NewsJournalBootstrap` 가 더 이상 존재하지 않으므로 Spring Data 직접 의존 / use-case + infra 패키지 혼재 문제 모두 해소.

## Technical Details

- 영향 파일: `NewsJournalBootstrap.java`, `NewsEventRepository.java`(Option A), `NewsEventRepositoryImpl.java`(Option A)

## Acceptance Criteria

- [ ] Bootstrap 이 도메인 포트만 사용하거나 클래스 자체 제거
- [ ] application/infrastructure 레이어 책임 명확
- [ ] 컴파일 + 부팅 OK

## Work Log

- 2026-04-29: ce-review 발견 (architecture P1 #1)
- 2026-04-29: 032 적용으로 자동 해소. `NewsJournalBootstrap` 삭제됨.

## Resources

- `src/main/java/.../newsjournal/infrastructure/bootstrap/NewsJournalBootstrap.java`
- `ARCHITECTURE.md`