# 프로젝트 의존성

## 빌드 도구 및 언어

- **Gradle**: Spring Boot Gradle Plugin 사용
- **Java**: 21
- **Spring Boot**: 4.0.1

---

## 핵심 의존성

### Spring Framework

| 의존성 | 버전 | 용도 |
|--------|------|------|
| spring-boot-starter | Boot 관리 | Spring Boot 기본 |
| spring-boot-starter-web | Boot 관리 | REST API 개발 |
| spring-boot-starter-webflux | Boot 관리 | 비동기 웹 클라이언트 |
| spring-boot-starter-data-jpa | Boot 관리 | JPA 지원 |
| spring-boot-starter-security | Boot 관리 | 보안 및 인증 |
| spring-boot-starter-validation | Boot 관리 | 입력 검증 |
| spring-boot-starter-actuator | Boot 관리 | 애플리케이션 모니터링 |

### Database

| 의존성 | 버전 | 용도 |
|--------|------|------|
| postgresql | Boot 관리 | PostgreSQL JDBC 드라이버 |

### Query DSL

| 의존성 | 버전 | 용도 |
|--------|------|------|
| querydsl-jpa | 5.0.0 | 타입 안전 쿼리 API |
| querydsl-apt | 5.0.0 | QueryDSL 코드 생성 |

### JWT

| 의존성 | 버전 | 용도 |
|--------|------|------|
| jjwt-api | 0.12.3 | JWT API |
| jjwt-impl | 0.12.3 | JWT 구현체 (runtime) |
| jjwt-jackson | 0.12.3 | JWT JSON 처리 (runtime) |

### Utility

| 의존성 | 버전 | 용도 |
|--------|------|------|
| lombok | Boot 관리 | 보일러플레이트 코드 제거 |
| rome | 2.1.0 | RSS/Atom 피드 파싱 |

---

## 테스트 의존성

| 의존성 | 버전 | 용도 |
|--------|------|------|
| spring-boot-starter-test | Boot 관리 | Spring Boot 테스트 |
| spring-security-test | Boot 관리 | Spring Security 테스트 |
| junit-platform-launcher | Boot 관리 | JUnit 플랫폼 |

---

## 주석 처리기

| 의존성 | 용도 |
|--------|------|
| querydsl-apt:jakarta | QueryDSL Q클래스 생성 |
| jakarta.annotation-api | Jakarta 어노테이션 처리 |
| jakarta.persistence-api | JPA 어노테이션 처리 |
| lombok | Lombok 어노테이션 처리 |

---

## 버전 관리 정책

- Spring Boot 관리 의존성: Spring Boot의 Dependency Management 플러그인에 의해 자동 버전 관리
- 명시적 버전: QueryDSL, JWT, Rome 등 특정 버전이 필요한 라이브러리만 명시
- Java Toolchain: Java 21 고정

---

**작성일**: 2026-01-20