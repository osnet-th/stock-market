---
status: pending
priority: p2
issue_id: 026
tags: [code-review, performance, hibernate, newsjournal, project-wide]
dependencies: []
---

# `@Modifying` flush/clear 옵션 누락 + Hibernate 전역 성능 프로퍼티 미설정

## Problem Statement

(1) `NewsEventLinkJpaRepository.deleteByEventId` 의 `@Modifying` 에 `flushAutomatically` / `clearAutomatically` 가 미설정이다. 현 흐름에서는 안전하나, 향후 자식 영속화 흐름이 추가되면 1차 캐시 stale 로 conflict 가능.
(2) 전역 Hibernate 성능 프로퍼티(`order_inserts`, `order_updates`, `in_clause_parameter_padding`) 가 미설정이라 batch insert 가 환경 의존적이고, 가변 페이지 사이즈로 prepared statement 캐시 미스 가능.

## Findings

- @Modifying 미설정: `src/main/java/com/thlee/stock/market/stockmarket/newsjournal/infrastructure/persistence/NewsEventLinkJpaRepository.java:18-20`
- 동일 패턴: `NewsEventJpaRepository.java:49-50`
- 글로벌 설정 부재: `src/main/resources/application.yml`

## Proposed Solutions

### Option A — 두 영역 함께 보완 (Recommended)
1. `@Modifying(clearAutomatically = true, flushAutomatically = true)` 두 군데 추가.
2. `application.yml` 에:
   ```yaml
   spring.jpa.properties.hibernate:
     order_inserts: true
     order_updates: true
     query.in_clause_parameter_padding: true
   ```
- 효과: replace-all 흐름 안정성 + 모든 도메인 batch insert 일관 적용 + IN 절 plan cache 재사용.

### Option B — `@Modifying` 옵션만 추가
- 글로벌 설정은 별도 todo. 본 todo 는 newsjournal 한정.

## Recommended Action

A 적용. 글로벌 설정은 다른 도메인에도 이득이라 일괄 도입 가치 있음. 분석/설계 단계에서 portfolio/stocknote 영향도 검토.

## Technical Details

- 변경 파일: 두 JpaRepository + application.yml
- 호환성: order_inserts 도입 후 기존 batch 동작 변화 없음(개선만).
- in_clause_parameter_padding 은 IN 절 파라미터 수를 2의 거듭제곱으로 패딩 → plan 재사용.

## Acceptance Criteria

- [ ] @Modifying 두 군데 옵션 추가
- [ ] application.yml 전역 프로퍼티 추가
- [ ] 부팅 후 회귀 없음 (포트폴리오/stocknote 통합 동작)

## Work Log

- 2026-04-28: 발견 (ce-review 성능 P2-2 + P3-1)

## Resources

- performance-oracle 보고 P2-2, P3-1
- Hibernate ref: https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#configurations-batch