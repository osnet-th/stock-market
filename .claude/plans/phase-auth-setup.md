# Phase 1: 기반 설정 및 인증 (Week 1-2)

## 구현된 패키지 구조

```
user/
├── domain/
│   ├── model/
│   │   ├── User.java                    # 사용자 엔티티
│   │   ├── OAuthAccount.java            # OAuth 연동 계정 엔티티
│   │   ├── OAuthProvider.java           # OAuth 제공자 enum (KAKAO, GOOGLE)
│   │   ├── OAuthIdentifier.java         # OAuth 식별자 VO
│   │   ├── Nickname.java                # 닉네임 VO
│   │   ├── PhoneNumber.java             # 전화번호 VO
│   │   ├── UserRole.java                # 사용자 역할 enum (USER, ADMIN)
│   │   └── UserStatus.java              # 사용자 상태 enum (ACTIVE, INACTIVE, SUSPENDED)
│   ├── repository/
│   │   ├── UserRepository.java          # 사용자 리포지토리 인터페이스
│   │   └── OAuthAccountRepository.java  # OAuth 계정 리포지토리 인터페이스
│   └── service/
│       └── OAuthConnectionService.java  # OAuth 연동 도메인 서비스
│
infrastructure/
└── security/
    └── config/
        ├── DevSecurityConfig.java       # 개발 환경 Security 설정
        └── ProdSecurityConfig.java      # 운영 환경 Security 설정
```

## 1.1 프로젝트 초기 설정

### 작업 리스트(순서대로 작업 필수)
- [완료] Spring Boot 프로젝트 생성
- [완료] 기본 의존성 추가 (Spring Web, JPA, Security, jwt)
- [완료] 데이터베이스 설정 (PostgreSQL 개발용)
- [완료] profiles 구조로 yml 분리
- [완료] Spring Security 작성(profiles 기반으로 dev일때는 모든 API 허용)
- [ ] 사용자 인증 기능 구현
  - [완료] 사용자 인증 도메인 계층 구현
  - [완료] 사용자 인증 유스케이스(서비스) 구현
  - [완료] Kakao Oauth 인증 구조 설정
  - [완료] Kakao Oauth 인증 흐름 구현
  - [ ] JWT Provider 구현
  - [ ] Google Oauth 인증 구조 설정


---

## 1.2 JWT 인증 구현 (TDD)

### 도메인 서비스 배치 주의
- JWT는 보통 외부 라이브러리/키/시간 소스에 의존하므로 **domain에 직접 구현하지 않는 것을 권장**.
- 권장: domain에는 `JwtTokenProvider` 같은 **인터페이스(포트)**만 두고, 구현체는 `infrastructure/security/jwt` 등에 위치.

---

## 제약 사항

- **DDD 계층형 구조 준수 필수**
- **Entity 연관관계 사용 금지** (ID 기반 참조만)
- **domain 계층에 Spring/JPA 의존성 금지**
- **@Transactional은 application 계층에서만 사용**
- **테스트 실패 → 최소 구현 → 테스트 성공 순서 준수**
- **Mock은 테스트 대상의 경계에서만 사용**
- **domain 계층은 Mock 없이 순수 테스트**

---

## 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [STOCK-MARKET-PROJECT.md](../../STOCK-MARKET-PROJECT.md)
- [CLAUDE.md](../../CLAUDE.md)

---

**작성일**: 2026-01-19
**수정일**: 2026-01-22
