---
status: pending
priority: p2
issue_id: 017
tags: [code-review, architecture, domain-model, documentation, portfolio]
dependencies: []
---

# 도메인 `version` 패스스루 필드의 의도 javadoc 미명시

## Problem Statement

워킹 디렉토리 hotfix로 `PortfolioItem`에 `private Long version` 패스스루 필드가 도입됨(`PortfolioItem.java:24`). 도메인은 순수 비즈니스 규칙만 표현하는 것이 원칙이고 `@Version`은 인프라(JPA) 관심사. 트레이드오프는 명확하지만 javadoc/주석 부재로 후속 개발자가 mutate 메서드를 추가할 위험.

## Findings

- 위치: `PortfolioItem.java:24, 86`, `PortfolioItemMapper.java:87, 110~`
- plan 보정 노트(`L268-272`)는 의도를 정확히 기술 → 코드에는 미반영
- `version`은 read-only 패스스루 — 도메인 메서드에서 절대 사용/변경하지 말아야 함

## Proposed Solutions

### Option A — `version` 필드에 javadoc 추가
```java
/**
 * JPA `@Version` 패스스루용. 영속성 라운드트립에서 손실되지 않도록 도메인이 보유하지만,
 * 도메인 규칙(검증/계산)에는 절대 사용하지 않는다. mutate 메서드 추가 금지.
 */
private Long version;
```

### Option B — 장기적으로 도메인↔Entity 매핑에서 `EntityManager.merge`로 우회
- 장기 과제. 본 PR 범위 외.

## Recommended Action

A 즉시 적용. B는 별도 검토.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PortfolioItem.java`

## Acceptance Criteria

- [ ] javadoc 추가
- [ ] PR 리뷰 시 후속 개발자 인지 가능

## Work Log

- 2026-04-27: ce-review 발견 (architecture-strategist P2-2)