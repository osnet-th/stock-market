# 기반 설정 및 Spring Security 설계

## 1. 개요

본 설계는 Phase 1의 초기 작업 3가지를 다룹니다:
- 기본 의존성 추가 (Spring Web, JPA, Security, JWT)
- 데이터베이스 설정 (PostgreSQL 개발용)
- Spring Security 작성 (profile 기반, dev는 모든 API 허용)

---

## 2. 기본 의존성 추가

### 2.1 필요한 의존성 목록

**Spring Core**
- spring-boot-starter-web: REST API 개발
- spring-boot-starter-data-jpa: JPA 지원
- spring-boot-starter-security: 보안 및 인증
- spring-boot-starter-validation: 입력 검증

**Database**
- postgresql: PostgreSQL JDBC 드라이버
- HikariCP: 커넥션 풀 (Spring Boot 기본 포함)

**JWT**
- jjwt-api: JWT API
- jjwt-impl: JWT 구현체
- jjwt-jackson: JWT JSON 처리

**Utility**
- lombok: 보일러플레이트 코드 제거

**Test**
- spring-boot-starter-test: 테스트 프레임워크
- spring-security-test: Security 테스트 지원

### 2.2 버전 선택 기준

- Spring Boot: 3.2.x 이상 (안정화된 최신 버전)
- Java: 17 이상
- jjwt: 0.12.x (최신 안정 버전)
- PostgreSQL: 42.x (최신 JDBC 드라이버)

### 2.3 의존성 배치

- `build.gradle` (Gradle 사용 시) 또는 `pom.xml` (Maven 사용 시)에 추가
- 테스트 의존성은 `testImplementation` 스코프로 분리
- compileOnly와 annotationProcessor로 Lombok 설정

---

## 3. 데이터베이스 설정

### 3.1 application.yml 구조

**프로파일 분리**
- `application.yml`: 공통 설정
- `application-dev.yml`: 개발 환경 설정
- `application-prod.yml`: 운영 환경 설정 (향후 추가)

### 3.2 데이터베이스 연결 설정

**공통 설정 항목**
- spring.datasource.driver-class-name: PostgreSQL 드라이버
- spring.jpa.database-platform: PostgreSQL Dialect

**개발 환경 (application-dev.yml)**
- url: jdbc:postgresql://localhost:5432/stock_market_dev
- username: 개발용 계정
- password: 개발용 비밀번호
- show-sql: true (쿼리 로깅)
- ddl-auto: validate (스키마 검증만)

### 3.3 HikariCP 설정

- maximum-pool-size: 10
- minimum-idle: 5
- connection-timeout: 3000ms
- idle-timeout: 600000ms (10분)
- max-lifetime: 1800000ms (30분)

### 3.4 JPA 설정

- hibernate.naming.physical-strategy: snake_case 변환
- hibernate.format_sql: true (개발 환경)
- open-in-view: false (OSIV 비활성화)

---

## 4. Spring Security 설계

### 4.1 설계 목표

- **dev 프로파일**: 모든 API 요청 허용 (인증/인가 비활성화)
- **prod 프로파일**: JWT 기반 인증/인가 활성화
- profile별 SecurityConfig 분리 또는 단일 Config에서 조건부 분기

### 4.2 SecurityConfig 구조

**파일 위치**
- `infrastructure/security/config/SecurityConfig.java`

**설정 방식**
- `@Profile("dev")`와 `@Profile("prod")`로 Config 분리
- 또는 단일 Config에서 `@Value("${spring.profiles.active}")`로 조건 분기

### 4.3 dev 프로파일 설정

**SecurityFilterChain 구성**
- 모든 요청에 대해 `.permitAll()` 적용
- CSRF 비활성화
- Session 정책: STATELESS (향후 JWT 대비)
- CORS 설정: 모든 origin 허용 (개발용)

**비활성화 항목**
- httpBasic 비활성화
- formLogin 비활성화
- 커스텀 필터 추가 없음

### 4.4 prod 프로파일 설정 (향후 구현)

**SecurityFilterChain 구성**
- `/api/auth/**`: 인증 엔드포인트, permitAll
- 나머지 요청: authenticated 필요
- JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞에 배치)
- 예외 처리: AuthenticationEntryPoint, AccessDeniedHandler

### 4.5 CORS 설정

**CorsConfigurationSource 정의**
- dev: 모든 origin 허용
- prod: 특정 도메인만 허용 (설정 파일로 관리)
- 허용 메서드: GET, POST, PUT, DELETE, PATCH
- 허용 헤더: Authorization, Content-Type
- Credentials: true

### 4.6 패키지 구조

```
infrastructure/
└── security/
    ├── config/
    │   ├── SecurityConfig.java (또는 DevSecurityConfig.java, ProdSecurityConfig.java)
    │   └── CorsConfig.java
    └── jwt/
        └── (향후 JWT 필터 및 Provider 배치)
```

### 4.7 설정 적용 순서

1. SecurityConfig 작성 (dev 프로파일만 우선 구현)
2. CORS 설정 추가
3. application-dev.yml에 security 관련 설정 추가
4. 로컬 실행하여 모든 API 접근 가능 여부 확인

---

## 5. 제약 사항 및 주의 사항

### 5.1 아키텍처 준수

- SecurityConfig는 infrastructure 계층에 배치
- domain 계층에 Spring Security 의존성 절대 금지
- 설정 파일은 application 또는 infrastructure에서만 참조

### 5.2 TDD 적용 범위

- SecurityConfig 자체는 설정 코드이므로 단위 테스트 불필요
- 향후 JWT 필터, AuthenticationProvider 등은 TDD로 작성
- Security 통합 테스트는 Phase 1 완료 후 별도 작성 고려

### 5.3 환경 분리

- dev 프로파일에서는 절대 운영 데이터베이스 연결 금지
- 설정 파일에 민감 정보(비밀번호, 키) 하드코딩 금지
- 향후 환경 변수 또는 외부 설정 관리 도구 사용 검토

---

## 6. 작업 순서

1. **의존성 추가**: build.gradle 또는 pom.xml 수정
2. **application.yml 작성**: 공통 설정 및 dev 프로파일 분리
3. **SecurityConfig 작성**: dev 프로파일용 설정 구현
4. **CorsConfig 작성**: CORS 설정 추가
5. **검증**: 로컬 실행하여 설정 적용 확인

---

**작성일**: 2026-01-20