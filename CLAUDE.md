# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소의 코드를 작업할 때 참고할 가이드를 제공합니다.

---

# 🚧 Section 0: 필수 하네스

- 모든 작업 제어 규칙은 [docs/ai/agent-harness.md](docs/ai/agent-harness.md)를 따른다.
- 작업 시작 전 반드시 [docs/ai/agent-harness.md](docs/ai/agent-harness.md)를 읽고 적용한다.
- 이 문서와 공통 하네스 문서가 충돌하면 더 보수적인 규칙을 따른다.
- `AGENTS.md`와 `CLAUDE.md`는 동일한 참조 구조를 유지한다.
- 공통 정책 변경은 [docs/ai/agent-harness.md](docs/ai/agent-harness.md)에 반영한다.
- 래퍼 문서 변경이 필요하면 `AGENTS.md`와 `CLAUDE.md`에 동일하게 반영한다. 의도적으로 차이를 둘 경우 plan에 차이, 사유, 승인 여부를 명시한다.

## 빠른 확인

- 응답 시 항상 "태형님"이라고 호칭한다.
- 코드 구현이나 수정은 승인된 `docs/plans/` plan 문서가 있을 때만 진행한다. 오타·컴파일 에러·문서 수정·로직 무변경 국소 수정은 agent-harness의 lightweight workflow로 대화 승인 후 진행할 수 있다.
- 작업 흐름은 착수 → brainstorm → plan/plan-approval → implement → review → explain(구현 이해 게이트) → verify → pr → merge 단계를 따르고, 단계는 `{worktree}/.claude/issues/{이슈번호}/stage` 마커로 관리한다.
- 비 이슈로 시작한 documented 작업은 brainstorm 완료 후 plan 진입 전에 GitHub 이슈 등록 여부를 확인한다(github-issue-gate 이슈 등록 Gate).
- issue 착수는 `scripts/start-issue-worktree.sh {이슈번호}`, PR/병합은 `gh` CLI를 사용한다. 병합은 태형님 승인 후에만 수행한다.
- 커밋 메시지와 PR 본문에는 AI/도구 작성자 정보(`Co-Authored-By` 등)를 포함하지 않는다.
- brainstorm/plan/리뷰 결과/구현 설명 4종은 [docs/ai/notion-guide.md](docs/ai/notion-guide.md)에 따라 Notion(프로젝트 플래너 > stock-market)에 동기화한다.

---

# 📋 Section 1: 프로젝트 개요

## 프로젝트 정보

- 단일 모듈 Gradle 프로젝트 (Spring Boot)
- 한국/해외 주식 포트폴리오 관리 및 분석 플랫폼
- 주요 도메인: stock, portfolio, chatbot, economics, news, overseasnews, favorite, salary, notification, user

## 문서화된 솔루션

`docs/solutions/` — 과거 해결된 문제(버그, 베스트 프랙티스, 워크플로우 패턴)를 카테고리별로 정리한 문서. YAML 프론트매터(`module`, `tags`, `problem_type`)로 검색 가능. 구현이나 디버깅 시 관련 영역의 기존 솔루션을 참고할 수 있음.

## 도메인 목적

- **stock**: 종목 정보 관리, DART 재무제표/지표 조회, KIS 주가 조회, 가치��가 계산
- **portfolio**: 사용자 포트폴리오 관리, 자산 배분, 수익률 추적
- **chatbot**: AI 챗봇 (Gemini API), 포트폴리오/재무/경제 분석 대화
- **economics**: 한국은행 ECOS 경제지표, 글로벌 경제지표 (Trading Economics 스크래핑)
- **news**: ��내 뉴스 (네이버 API, Google RSS), 종목별 뉴스 수집
- **overseasnews**: 해외 뉴스 (GNews, NewsAPI)
- **favorite**: 관심 지표 대시보드
- **salary**: 월급 사용 비율 관리
- **notification**: 이메일 알림 (Gmail SMTP)
- **user**: 사용자 인증, 카카오 OAuth, JWT 토큰 관리

## 기술 스택

- **Java 21**, Spring Boot 4 (Jakarta EE)
- **ORM**: JPA + QueryDSL
- **보안**: JWT (Stateless, Bearer 토큰), 카카오 OAuth, Spring Security
- **DB**: PostgreSQL
- **비동기**: Spring Scheduler, WebFlux (Gemini API 스트리밍)
- **캐싱**: Caffeine
- **빌드**: Gradle, Docker + Nginx + Cloudflare Tunnel 배포
- **외부 API**: DART, KIS, Finnhub, ECOS, Naver/GNews/NewsAPI, Gemini, 한국수출입은행
- **프론트엔드**: Alpine.js + Tailwind CSS (서버 사이드 렌더링, static resources)

---

# 🏗️ Section 2: 빌드 및 배포

## 빌드

- 단일 모듈 Gradle 빌드
- Java 21 컴파일
- Docker + Nginx + Cloudflare Tunnel 배포
- `./gradlew bootJar`로 빌드, `docker-compose up`으로 배포

---

# 🔧 Section 3: 인프라 구성

## 외부 API 통신

- **RestClient (Spring 7)**: DART, KIS, ECOS, 뉴스 API 등 동기 HTTP 호출
- **WebClient (WebFlux)**: Gemini API SSE 스트리밍 전용
- 환경변수(`.env`)로 API 키 관리

## DB 전략

**JPA + QueryDSL** 단일 ORM 사용:
- Entity는 연관관계 없이 ID 기반 참조만 허용
- PostgreSQL 방언
- `hibernate.jdbc.batch_size=1000`
- `open-in-view: false`

## 구성 관리

- `application.yml` + `.env` 파일로 외부 API 키, DB 접속 정보, JWT 설정 등 관리
- 프로파일: `dev` (기본)

---

# 💻 Section 4: 개발 규칙

## 코드 작성 원칙

1. **아키텍처 규칙 준수**: [ARCHITECTURE.md](ARCHITECTURE.md) 참조 (레이어 책임, 의존성 방향, DTO/Entity 경계)
2. **Entity 작성 원칙**: JPA Entity는 ID 기반 참조만 허용, 연관관계 금지
3. **행위 중심 모델링**: Anemic Model 지양, 도메인 메서드로 규칙 표현
4. **요청받지 않은 리팩토링/API 변경 금지**
5. **구조 변경 시 허락 필수**
6. **작업 계획 필수**: 코드 작성 전 `docs/plans/`에 plan 문서 작성 → 승인 후 구현 ([docs/ai/gates/planning-gate.md](docs/ai/gates/planning-gate.md))
7. **Entity 작성 시 사전 승인 필수**
8. **테스트 가능성** : 코드는 테스트 가능성을 고려하여 테스트 작성이 가능한 코드로 구현합니다.
9. **커뮤니케이션**: 응답 시 항상 "태형님" 호칭 사용
10. **YAGNI 원칙**: 현재 설계 문서의 작업 범위에 포함되지 않은 메서드, 클래스, 인터페이스를 미리 만들지 않습니다.
11. **Lombok 사용**: getter/setter는 Lombok 애노테이션(`@Getter`, `@Setter`, `@RequiredArgsConstructor` 등)을 사용하며, 수동으로 getter/setter 메서드를 작성하지 않습니다.

## 설계 및 구현 프로세스

**CRITICAL: 코드 구현이나 수정은 승인된 `docs/plans/` plan 문서가 없는 경우 절대 진행하지 않습니다.**

계획·승인·구현·리뷰·검증·PR·병합 절차는 [docs/ai/agent-harness.md](docs/ai/agent-harness.md)의 Mandatory Workflow와 단계별 gate 문서를 따릅니다.

- 산출물 경로: 브레인스토밍은 `docs/brainstorms/`, plan은 `docs/plans/` ([docs/ai/gates/planning-gate.md](docs/ai/gates/planning-gate.md))
- 기존 `.claude/designs/`, `.claude/analyzes/` 문서는 legacy reference로만 읽고 신규 문서는 작성하지 않습니다.
- plan 이탈 금지: plan과 다른 구현 절대 금지, 변경 필요 시 plan 갱신 후 재승인 필요

## 버그 및 문제 발견 시 프로세스

**CRITICAL: 문제 발견 시 즉시 수정 절대 금지**. 분석 내용을 plan 문서의 "배경 / 현재 상태 / 문제점" 섹션에 통합하고 승인 후 수정합니다.

```
문제 발견 → docs/brainstorms/ Harness Brainstorm → docs/plans/ plan 작성/업데이트 → 승인 대기 → 수정
```

구현 중 문제를 발견하면 즉시 중단하고 plan 갱신·재승인 후 재개합니다. 즉시 수정 가능한 예외는 [docs/ai/stop-gates.md](docs/ai/stop-gates.md)의 Exceptions(린터/포맷터 자동 수정, 명백한 컴파일 에러)만 인정합니다.

## 작업 리스트 작성 규칙

TaskCreate 사용 시 다음 규칙을 준수:
- **subject**: `[모듈명] 구체적인 작업 내용` 형식 (예: `[ground] ConnectionValidator 테스트 코드 작성`)
- **activeForm**: 현재 진행형으로 명확하게 (예: `ConnectionValidator 테스트 코드 작성 중`)
- 애매한 표현 금지 (예: "작업 중", "처리 중" 등)

## 작업 진행 규칙

- **한 번에 하나의 작업만 진행**: 작업 리스트의 작업은 순서대로 한 단계씩만 수행
- **완료 후 대기**: 현재 작업이 완료되면 즉시 다음 작업으로 넘어가지 않고, 사용자에게 다음 작업 진행 여부를 반드시 확인
- **동시 진행 금지**: 여러 작업을 동시에 진행하지 않음
- **작업 완료 시 체크 표시**: plan 문서의 작업 리스트에서 작업을 완료하면 `- [ ]`를 `- [x]`로 변경하여 완료 처리

## 아키텍처 및 계층 규칙

본 프로젝트의 패키지 구조, 레이어 책임, 의존성 방향, DTO/Entity 경계, 트랜잭션 규칙은
**[ARCHITECTURE.md](ARCHITECTURE.md) 파일의 기준을 반드시 따릅니다.**

- 레이어 책임 및 의존성 방향 규칙
- DTO / Entity / Domain 경계 규칙
- 트랜잭션 경계 규칙
- 예외 처리 및 외부 연동 규칙

구조 변경 또는 계층 규칙 위반 가능성이 있는 작업은
**[ARCHITECTURE.md](ARCHITECTURE.md)를 우선 참고하고, 명시적 요청 없이는 구조를 변경하지 않습니다.**

---

## 도구 사용 규칙

- **코드 수정**: Edit 툴 사용 (IDE에서 변경사항 바로 확인 가능)
- **코드 탐색/조회**: Serena 사용 (심볼 검색, 레퍼런스 추적 등)
- Serena의 `replace_content`, `replace_symbol_body` 등은 코드 수정에 사용하지 않음

---

## 테스트 코드 가이드

### 코드 구현 원칙

- **코드 구현 시에는 테스트를 작성하지 않으나, 테스트가 가능한 코드로 구현합니다.**
- 테스트 가능성을 고려한 설계 (의존성 주입, 단일 책임 원칙 등)
- 명시적 요청이 있을 경우에만 테스트 코드를 작성합니다.

### 테스트 작성 원칙

본 프로젝트는 현재 테스트가 부족한 상태입니다. 테스트 작성 요청 시 다음 규칙을 준수합니다:

- **테스트는 단위테스트(Unit Test)로 범위를 제한합니다.**
  - 통합테스트(Integration Test)나 E2E 테스트는 작성하지 않습니다.
  - 외부 의존성(DB, 외부 API 등)은 Mock으로 대체합니다.
  - 각 클래스/메서드의 동작을 독립적으로 검증합니다.
  - 테스트는 상태 기반의 테스트를 작성합니다. 예상 결과가 일치하는지에 대한 결과 값 검증 테스트만 작성하고 메서드 호출 여부에 테스트는 절대 작성하지 않습니다.

### 테스트 작성 스타일

- 테스트는 **비즈니스 동작과 규칙 중심**으로 작성합니다.
- Given / When / Then 스타일을 권장합니다.
- 기존 아키텍처, 계층 규칙, 코딩 스타일을 반드시 유지합니다.
- 명시적 요청 없이는 public API 변경 또는 신규 API 추가를 하지 않습니다.

---

## 성능 최적화

- **DB 쿼리**: JPA 배치 조회, QueryDSL 활용, 페이징 필수, batch_size=1000
- **캐싱**: Caffeine 적극 활용, TTL 설정, 캐시 무효화 처리
- **비동기**: 외부 API/대량 처리 비동기화, Spring Scheduler 활용
- **API**: DTO 최소화, 대용량 스트리밍/페이징
- **연결 풀**: HikariCP 제한, 커넥션 누수 방지
- **로깅**: verbose 로그 지양, 대용량 데이터 요약, 로그 레벨 적절히 설정

---

# ⚠️ Section 5: CRITICAL

## plan 및 브레인스토밍 문서 작성

문서 작성 규칙은 [MD_WRITE_GUIDE.md](MD_WRITE_GUIDE.md)를 참고하세요.

**핵심 원칙**:
- 실용적 균형: 구현에 필요한 핵심 정보만 간결하게
- 필수: 작업 리스트 + 예시 코드 링크
- 선택: 배경, 핵심 결정, 주의사항 (필요시에만)
- 금지: 과도한 형식, 불필요한 섹션, 중복 내용

**디렉토리 구조** ([docs/ai/gates/planning-gate.md](docs/ai/gates/planning-gate.md)):
- 브레인스토밍: `docs/brainstorms/{YYYY-MM-DD}-{topic}-brainstorm.md`
- plan: `docs/plans/{YYYY-MM-DD}-{NNN}-{type}-{descriptive-name}-plan.md`
- plan 예시 코드: `docs/plans/examples/{component}-example.md`
- 기존 `.claude/analyzes/`, `.claude/designs/` 문서는 legacy reference 전용(신규 작성 금지)

## 구현 규칙

- **승인된 `docs/plans/` plan 문서 필수 준수**
- plan과 다른 구현 발견 시 **즉시 중단 후 재승인 요청**
- 구현 중 plan 변경 필요 시 **plan 문서 수정 후 재승인**