# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 저장소의 코드를 작업할 때 참고할 가이드를 제공합니다.

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
6. **작업 계획 필수**: 코드 작성 전 `.claude/plans`에 md 파일로 작성 → 승인 후 구현
7. **Entity 작성 시 사전 승인 필수**
8. **테스트 가능성** : 코드는 테스트 가능성을 고려하여 테스트 작성이 가능한 코드로 구현합니다.
9. **커뮤니케이션**: 응답 시 항상 "태형님" 호칭 사용
10. **YAGNI 원칙**: 현재 설계 문서의 작업 범위에 포함되지 않은 메서드, 클래스, 인터페이스를 미리 만들지 않습니다.
11. **Lombok 사용**: getter/setter는 Lombok 애노테이션(`@Getter`, `@Setter`, `@RequiredArgsConstructor` 등)을 사용하며, 수동으로 getter/setter 메서드를 작성하지 않습니다.

## 설계 및 구현 프로세스

**CRITICAL: 코드 구현이나 수정은 최소한 하나의 설계 문서 또는 계획서가 없는 경우 절대 진행하지 않습니다.**

1. **설계 문서 작성**: `.claude/designs/모듈명/` 하위에 설계 문서 작성
2. **설계 승인 대기**: 사용자 승인 후 구현 시작
3. **설계 준수 구현**: 승인된 설계 문서의 내용을 **반드시 준수**하여 구현
4. **설계 이탈 금지**: 설계와 다른 구현 절대 금지, 변경 필요 시 재승인 필요

## 버그 및 문제 발견 시 프로세스

### 문제 발견 시 원칙

**CRITICAL: 문제 발견 시 즉시 수정 절대 금지**. 아래 프로세스를 반드시 따름:

1. **문제 분석 및 문서화**
   - `.claude/analyzes/{모듈명}/{문제명}/{문제명}.md` 작성
   - 현재 상태, 문제점, 근본 원인, 영향 범위 분석
   - 코드 위치 명시 (파일:라인)

2. **해결 방안 설계**
   - `.claude/designs/{모듈명}/{해결방안명}/{해결방안명}.md` 작성
   - 또는 기존 설계 문서 업데이트 (변경 사항 명시)
   - 해결 방안, 구현 계획, 테스트 계획 포함

3. **승인 대기**
   - 사용자 승인 후에만 수정 진행
   - 승인 없이 코드 수정 절대 금지

### 구현 중 문제 발견 시

- **즉시 구현 중단**
- **설계 단계로 복귀**
- 설계 문서 업데이트 (발견된 문제 및 해결 방안 추가)
- 재승인 후 구현 재개

### 예외 사항

**다음 경우에만** 즉시 수정 가능:
- 린터/포맷터 자동 수정 (코드 로직 변경 없음)
- 명백한 컴파일 에러 (오타, import 누락, 변수명 오류 등)
- **단, 로직 변경이 필요한 버그는 반드시 위 프로세스 준수**

### 원칙 요약

```
문제 발견 → 분석 문서 작성 → 설계 문서 작성/업데이트 → 승인 대기 → 수정
```

**즉시 수정 시도는 규칙 위반**입니다.

## 작업 리스트 작성 규칙

TaskCreate 사용 시 다음 규칙을 준수:
- **subject**: `[모듈명] 구체적인 작업 내용` 형식 (예: `[ground] ConnectionValidator 테스트 코드 작성`)
- **activeForm**: 현재 진행형으로 명확하게 (예: `ConnectionValidator 테스트 코드 작성 중`)
- 애매한 표현 금지 (예: "작업 중", "처리 중" 등)

## 작업 진행 규칙

- **한 번에 하나의 작업만 진행**: 작업 리스트의 작업은 순서대로 한 단계씩만 수행
- **완료 후 대기**: 현재 작업이 완료되면 즉시 다음 작업으로 넘어가지 않고, 사용자에게 다음 작업 진행 여부를 반드시 확인
- **동시 진행 금지**: 여러 작업을 동시에 진행하지 않음
- **작업 완료 시 체크 표시**: 설계 문서의 작업 리스트에서 작업을 완료하면 `- [ ]`를 `- [x]`로 변경하여 완료 처리

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

## 설계 및 분석 문서 작성

**분석 문서** 및 **설계 문서** 작성 규칙은 [MD_WRITE_GUIDE.md](MD_WRITE_GUIDE.md)를 참고하세요.

**핵심 원칙**:
- 실용적 균형: 구현에 필요한 핵심 정보만 간결하게
- 필수: 작업 리스트 + 예시 코드 링크
- 선택: 배경, 핵심 결정, 주의사항 (필요시에만)
- 금지: 과도한 형식, 불필요한 섹션, 중복 내용

**디렉토리 구조**:
- 분석 문서: `.claude/analyzes/{모듈명}/{기능명}/{기능명}.md`
- 분석 코드 예시: `.claude/analyzes/{모듈명}/{기능명}/examples/{컴포넌트명}-example.md`
- 설계 문서: `.claude/designs/{모듈명}/{기능명}/{기능명}.md`
- 설계 코드 예시: `.claude/designs/{모듈명}/{기능명}/examples/{컴포넌트명}-example.md`

## 구현 규칙

- **승인된 설계 문서 필수 준수**
- 설계와 다른 구현 발견 시 **즉시 중단 후 재승인 요청**
- 구현 중 설계 변경 필요 시 **설계 문서 수정 후 재승인**