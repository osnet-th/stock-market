---
date: 2026-04-28
topic: news-journal
related_issue: "#33"
---

# 뉴스 기록 페이지 (News Journal)

## What We're Building

사용자가 직접 정리한 "사건(Event)"을 일자별 타임라인으로 누적·조회하는 개인용 뉴스 저널 기능.
하나의 사건은 WHAT/WHY/HOW로 정리된 본문 + 카테고리(악재/호재/중립) + 관련 기사 URL 리스트로 구성된다.
1차 MVP는 CRUD와 세로 타임라인 조회까지, 결과 추가·학습 메모는 2차로 분리한다.

## Why This Approach

이슈 #33의 요구사항을 살펴본 결과, 핵심 모델은 "사건(Event)" 한 개로 충분히 표현된다.
별도의 키워드/태그 도메인이나 기존 `news` 도메인과의 결합은 도메인 복잡도만 키우고
"개인이 손으로 정리한 저널"이라는 본질적 요구를 흐릴 위험이 있다.
따라서 **새 도메인을 분리하고, 기존 `news`와는 결합하지 않으며, 키워드 모델을 만들지 않는다.**

기능을 한 번에 모두 구현하는 대신 핵심 MVP를 먼저 검증한 뒤,
실제 사용 경험에 기반해 결과 추가·학습 메모·고급 조회를 2차로 붙인다 (YAGNI).

## Key Decisions

### 모델
- **기록의 단위**: 사건(Event) 1엔티티 중심. 한 사건이 여러 URL을 포함.
- **사건 ↔ 기사**: 외부 URL 자유 입력만 지원. 기존 `news` / `NewsEntity`와는 결합하지 않음. 추후 필요 시 ID 참조로 확장 가능.
- **카테고리**: `BAD` / `GOOD` / `NEUTRAL` 3종 enum. 강도/세분류는 도입하지 않음.
- **키워드 묶기**: 별도 키워드 모델을 만들지 않음. "한 사건 안의 여러 URL을 함께 보는 것" 자체가 묶음 역할을 수행.
- **소유권**: `Long userId` 필드 + `@AuthenticationPrincipal` 패턴 (`portfolio` / `favorite`와 동일).
- **JPA 연관관계 금지**: ARCHITECTURE.md 규칙대로 ID 기반 참조만 사용.

### 1차 MVP 범위
- 사건 CRUD: 발생일자(`occurredDate`), 제목, WHAT/WHY/HOW, 카테고리, URL 리스트.
- 세로 타임라인 조회 (Tailwind 마크업, 수직선 + 도트 + 카드, 카테고리별 색상).
- 단일 사용자 본인 데이터만 조회/수정.

### 2차 (이슈에는 있으나 MVP 보류)
- 사건의 "결과" 추가 (`resultDate`, `resultText`) — 사후 평가용.
- 학습 메모 필드: 모르는 용어, 의문점, 투자 적용 인사이트.
- 고급 조회: 카테고리/기간 필터, 텍스트 검색.

### 위치 및 구조
- **백엔드 도메인**: `com.thlee.stock.market.stockmarket.newsjournal` 신규 패키지. ARCHITECTURE.md의 4계층(`presentation` / `application` / `domain` / `infrastructure`).
- **DB**: Flyway 마이그레이션으로 신규 테이블 추가 (`src/main/resources/db/migration/`).
- **프론트엔드**:
  - `static/index.html` 메뉴/섹션 추가
  - `static/js/components/news-journal.js` 신규 컴포넌트
  - `static/js/api.js`에 API 래퍼 메서드 추가
  - 시각화는 Chart.js 미사용, Tailwind 마크업으로 직접 구현

## Open Questions

(없음 — Phase 1 대화에서 모든 핵심 결정 해소)

## Next Steps

→ `/ce:plan` 으로 진행하여 다음을 확정한다:
- 정확한 엔티티/DTO 스키마와 컬럼 타입
- Flyway 마이그레이션 SQL
- API 엔드포인트 시그니처
- 작업 단위 분해 및 구현 순서