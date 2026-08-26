---
date: 2026-05-17
topic: glossary
issue: 43
---

# 개인 용어 사전 (Glossary)

## Problem Frame

사용자가 뉴스, 학습, 데이터 분석 중 마주치는 모르는 경제/주식 용어를 본인이 직접 정리해두고, 나중에 다시 찾아보거나 카테고리별로 묶어 학습 자료로 활용할 수 있는 **개인용 용어 사전**이 필요하다.

관리자 큐레이션 사전이나 사용자 간 공유 사전이 아니라, 사용자별 사적 공간이라는 점이 핵심.

본 문서 전반에서 이슈 #43 제목을 따라 "용어 사전"으로 표기를 통일한다 (학습 노트 의미 포함).

## Requirements

**용어 (Term) CRUD**

- R1. 사용자는 본인 사전에 용어를 등록할 수 있다. 필드: `용어명`(필수), `설명`(선택), `카테고리`(선택, 미지정 시 R5 "미분류"로 처리).
- R2. 사용자는 본인이 등록한 용어를 조회/수정/삭제할 수 있다. 수정은 PUT 전체 교체 방식으로 한다.
- R3. 사용자는 다른 사용자의 용어를 조회/수정/삭제할 수 없다. 모든 용어 API는 인증 필수이며, application 계층에서 `userId` 기반 ownership 검증을 강제한다.

**카테고리 (Category) 관리**

- R4. 사용자는 본인 카테고리를 생성/수정/삭제할 수 있다. 카테고리 API도 ownership 검증 대상이며, 동일 사용자 내 카테고리명은 unique로 한다.
- R5. 사용자에게는 기본 "미분류" 표현이 항상 제공된다. 사용자는 "미분류" 자체를 삭제하거나 이름 변경할 수 없으며, 일반 카테고리명으로 "미분류"를 생성·리네임할 수 없다 (예약어).
- R6. 일반 카테고리 삭제 시 해당 카테고리에 속한 용어는 "미분류"로 자동 이동한다. 카테고리 삭제 + 용어 재배치는 **단일 트랜잭션으로 원자성**을 보장한다. 사용자 확인 다이얼로그에 **영향받는 용어 건수**를 미리 표시한다.

**탐색/검색**

- R7. 용어 목록에서 용어명으로 검색할 수 있다 (부분 일치, 대소문자 무시).
- R8. 용어 목록에서 설명 텍스트로 검색할 수 있다 (부분 일치). **MVP는 LIKE 기반**으로만 구현하며, 향후 데이터 규모가 커지면 인덱싱 전략 재검토.
- R9. 카테고리로 필터링할 수 있다. R7/R8 검색과 R9 필터는 AND로 조합한다.
- R10. 등록일순(최신/오래된순), 가나다순 정렬을 지원한다. 기본값은 등록일 최신순.
- R11. 용어 목록은 페이지네이션을 적용한다 (기본 size·tie-breaker는 인접 도메인 컨벤션을 따른다).

**화면 상태**

- R12. Empty state를 정의한다. 신규 사용자(0건 등록)는 "첫 용어를 등록하세요" CTA를 본다. 검색 결과 0건과 카테고리 0개 상태도 각각의 메시지를 가진다.
- R13. 등록/수정 폼에서 **카테고리 신규 생성을 인라인으로 허용**한다 (별도 화면 이동 없이 폼 안에서 생성·선택). 인라인 생성도 R4(ownership, unique)·R14(길이 상한·plain text) 검증을 동일하게 적용한다.

**보안 / 입력 검증**

- R14. 용어명/설명/카테고리명에 길이 상한을 둔다 (구체 수치는 인접 도메인 컨벤션 참조). 설명은 **plain text로 저장·렌더링**하며 HTML/마크다운을 해석하지 않는다.
- R15. R7/R8 검색 입력은 LIKE 와일드카드(`%`, `_`, `\`)를 이스케이프하며, 입력 길이 상한을 둔다.

## Success Criteria

- 사용자가 모르는 용어를 마주쳤을 때, 본인 사전에 등록하고 나중에 검색/필터로 다시 찾을 수 있다.
- 사용자가 본인 기준대로 카테고리를 정의해 용어를 분류할 수 있다.
- 카테고리 삭제 시 용어 데이터가 손실되지 않으며, 삭제 동작은 트랜잭션 단위로 원자적이다.
- 타 사용자의 용어/카테고리는 어떠한 경로로도 조회·수정·삭제할 수 없다.
- 출시 후 leading indicator로 활성 사용자 1인당 평균 등록 건수와 등록 후 N일 내 재조회율을 관측한다 (구체 임계값은 출시 시점 결정).

## Scope Boundaries

- 관리자 큐레이션 사전, 사용자 간 공유/공개 사전 미포함
- LLM 자동 설명 생성/제안 미포함 (수동 입력만)
- 챗봇 컨텍스트 주입, 뉴스 화면 연동 등 다른 기능과의 연계 미포함 — **본 MVP의 가장 큰 채택 리스크 (Key Decisions의 "수동 입력 마찰 인지" 참조)**
- 출처(URL)/개인 메모/태그/다국어 미포함

## Key Decisions

- **개인 사전(per-user) 설계, MVP에서는 공유·공개 차단**: 이슈 의도가 사용자별 학습 노트. 다만 향후 read-only 공유 가능성을 닫지 않기 위해 Entity는 `userId` 기반으로 설계하고 visibility 컬럼은 **이번에 도입하지 않는다** (필요 시 마이그레이션 추가).
- **카테고리 자유 정의 (사용자 CRUD)**: 사용자마다 분류 기준이 다르므로 고정 enum보다 유연.
- **`newsjournal/` 카테고리 패턴 재사용 우선 평가**: 코드베이스에 이미 `(user_id, name)` unique + find-or-create + userId 스코핑 패턴(`NewsEventCategory*`)이 존재. plan 단계에서 신규 `glossary/` 도메인 vs 기존 도메인 확장 결정 시 1차 참조 모델로 사용한다. **단 재사용 가능 범위는 위 3가지로 한정** — R4의 update/delete 와 R6의 cascade 재배치는 `newsjournal/`에 미구현이며 본 기능에서 신규 구현해야 한다.
- **"미분류"는 가상 표현 (카테고리 null = 미분류)**: 실체 row 보장형은 가입 훅·기존 사용자 backfill 부담이 큼. R6 cascade는 해당 용어들의 `categoryId`를 null로 set하는 단일 update로 처리. R5의 "삭제 불가"는 단순히 null 카테고리가 시스템 표현이라는 의미.
- **R8 검색은 LIKE-only로 못박음**: Elasticsearch 도입을 보류해 plan 부담을 줄임. 데이터 규모가 커지면 별도 이터레이션에서 재평가.
- **LLM 자동채움 미포함**: MVP 단순화를 위해 미루는 선택. 채택률이 임계 미달일 경우 1순위 보완 후보.
- **수동 입력 마찰 인지**: 진입점이 별도 메뉴이고 외부 연동이 없는 본 MVP는 "쓰지 않는다"는 실패 모드가 가장 큰 리스크. 출시 후 leading indicator로 평가 후 후속 이터레이션(뉴스 진입점, LLM 보조)을 결정.
- **진입 경로: 사이드바 메뉴 신규 추가**: 기존 네비게이션 패턴(포트폴리오/뉴스 등)과 동일 계층에 "용어 사전" 메뉴 추가. 발견 가능성과 일관성 우선.
- **도메인 레이아웃: 신규 `glossary/` 도메인 분리**: "용어 사전"은 의미론적으로 독립 도메인. `newsjournal/`의 `(user_id, name)` unique + find-or-create + userId 스코핑 패턴은 참조하되, Entity/Service/Controller는 `glossary/` 패키지에 신규 구현.

## Dependencies / Assumptions

- 기존 사용자 인증/식별 구조(`user/` 도메인)를 따른다.
- 기존 아키텍처 컨벤션(DDD + Layered, `presentation → application → domain ← infrastructure`)을 따른다.
- `newsjournal/`의 사용자별 카테고리 관리 패턴 중 `(user_id, name)` unique, find-or-create, userId 스코핑 부분만 재사용 가능하다고 가정한다 (R4 update/delete, R6 cascade 재배치는 신규 구현 영역; plan 단계에서 검증).

## Outstanding Questions

### Resolve Before Planning

(없음 — 진입 경로와 도메인 레이아웃 결정은 Key Decisions로 격상)

### Deferred to Planning

- [Affects R1][Technical] 동일 사용자 내 용어명 중복 허용 여부. 기본 권고: **허용**(같은 용어를 다른 설명으로 두 번 등록 가능). plan에서 unique 제약 최종 결정.
- [Affects R1, R4, R14][Needs research] 글자수 제한 구체 수치 — `newsjournal/` 등 인접 도메인 entity 컨벤션 확인.
- [Affects R10, R11][Technical] 페이지네이션 default size, max size, secondary tie-breaker — 인접 도메인 DTO 패턴 답습.
- [Affects R6][Technical] 트랜잭션 실패 시 사용자 피드백 정책 (재시도 안내 / 에러 메시지 카피).
- [Affects R12, R13][Design] empty state 카피, 인라인 카테고리 생성 UX 디테일 — design 단계에서 결정.
- [Affects R3, R4][Security] R3는 application 계층 강제를 명시했고, 그 안에서 application service vs repository query predicate 중 어느 구체 위치에서 ownership 검증을 책임질지 — 컨벤션 결정.
- [Affects R3, R4][Security] 타 사용자 리소스 접근 시 응답 코드 정책 — 존재 누설 방지를 위한 404 통일 vs 403 분기. plan 시 결정.

## Next Steps

→ `/ce:plan` 으로 구현 계획 수립