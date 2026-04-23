---
review_agents: [code-simplicity-reviewer, security-sentinel, performance-oracle, architecture-strategist]
plan_review_agents: [code-simplicity-reviewer]
---

# Review Context

본 프로젝트는 Java 21 + Spring Boot 4 기반의 주식/경제지표 포트폴리오 플랫폼입니다.
리뷰 에이전트는 아래 컨벤션을 반드시 고려합니다.

## 아키텍처
- 단일 Gradle 모듈. 패키지 레이어: `presentation → application → domain / infrastructure`
- 레이어 책임/의존성 방향은 `ARCHITECTURE.md` 가 원본. 위반 의심 시 반드시 참조
- JPA Entity 는 연관관계 없이 **ID 기반 참조만** 허용. 연관관계 매핑 제안 금지
- DTO/Entity 경계: presentation DTO 는 application 에서만 생성 → domain/infra 로 누수 금지

## 코드 스타일 / 규칙
- Lombok 필수 (`@Getter`, `@Setter`, `@RequiredArgsConstructor`). 수동 getter/setter 금지
- YAGNI 원칙 엄수. 이번 작업 범위에 없는 메서드/클래스/인터페이스 신규 생성 금지
- 테스트는 명시 요청 시에만. 코드 구현 시 테스트 가능성(의존성 주입 등) 확보만 요구
- 한국어 리뷰 코멘트 가능. 사용자 호칭은 "태형님"

## 외부 연동 / 캐시
- 외부 API: RestClient(동기) + WebClient(Gemini SSE 스트리밍)
- 캐싱: Caffeine. TTL/size 설정 일관성 확인
- Spring Security + JWT Stateless. dev 환경은 `permitAll`, 운영은 인증 강제

## 안전장치
- 문제 발견 시 즉시 수정 금지. `docs/analyzes/` → `docs/designs/` → 승인 → 수정
- `docs/plans/*.md`, `docs/solutions/*.md`, `docs/brainstorms/*.md` 는 파이프라인 산출물 — 삭제/무시 권고 금지
- 정적 프론트 스택은 Alpine.js + Tailwind. 신규 JS 라이브러리 도입 제안 시 사전 승인 필요 명시

## 포커스 영역
- 동시성/레이스(ReentrantLock, 캐시 put/evict 타이밍)
- 외부 스크래핑 실패 격리(카테고리/카드 단위 try-catch)
- 권한/레이트리밋 우회 가능성
- N+1, JPA batch_size=1000, open-in-view=false 전제 유지