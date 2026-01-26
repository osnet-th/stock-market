# OAuthAccountRepository 구현 설계

## 1. 설계 개요

OAuthAccountRepository는 OAuth 계정 도메인 모델의 영속성을 담당하는 리포지토리입니다.
도메인 계층의 인터페이스(포트)와 인프라 계층의 JPA 구현체(어댑터)로 분리하여 구현합니다.

### 핵심 설계 원칙

- **도메인 모델과 JPA 엔티티 분리**: OAuthAccount(도메인) ↔ OAuthAccountEntity(인프라)
- **ID 기반 참조**: Entity 연관관계 사용 금지, Long 타입 userId로 User 참조
- **의존성 방향**: 인프라 계층이 도메인 계층에 의존 (역방향 금지)
- **TDD 적용**: domain/application 계층만 (infrastructure는 제외)
- **OAuthAccountMapper 사용**: Entity ↔ Domain Model 변환 전용 클래스

---

## 2. 구성 요소

### 2.1 도메인 계층 (domain/repository)

#### OAuthAccountRepository 인터페이스 ✅
- 도메인 모델(OAuthAccount)을 다루는 포트 인터페이스
- 영속성 기술에 무관한 순수 도메인 메서드 정의

**주요 메서드:**
- `save(OAuthAccount oauthAccount): OAuthAccount`: OAuth 계정 저장/수정
- `findByProviderAndIssuerAndSubject(OAuthProvider provider, String issuer, String subject): Optional<OAuthAccount>`: OAuth 식별자로 조회
- `findById(Long id): Optional<OAuthAccount>`: ID로 OAuth 계정 조회
- `findByUserId(Long userId): List<OAuthAccount>`: 사용자 ID로 OAuth 계정 목록 조회
- `findByIdIn(List<Long> ids): List<OAuthAccount>`: 여러 ID로 OAuth 계정 목록 조회

### 2.2 인프라 계층 (infrastructure/persistence)

#### OAuthAccountEntity
- JPA 엔티티 (@Entity, @Table 등 JPA 애노테이션 사용)
- 도메인 OAuthAccount와 독립적으로 존재
- 데이터베이스 oauth_accounts 테이블 매핑

**주요 필드:**
- id (Long, @Id @GeneratedValue)
- userId (Long, nullable) - User 참조 (연관관계 아닌 ID 참조)
- provider (String - enum 저장: KAKAO, GOOGLE, NAVER 등)
- issuer (String)
- subject (String)
- email (String)
- connectedAt (LocalDateTime)

**연관관계:**
- **연관관계 사용 금지** - userId(Long)로 User 참조만 허용
- User와의 관계는 ID 기반으로 관리

#### OAuthAccountJpaRepository (Spring Data JPA)
- `JpaRepository<OAuthAccountEntity, Long>` 확장
- Spring Data JPA 쿼리 메서드 정의

**쿼리 메서드:**
- `Optional<OAuthAccountEntity> findByProviderAndIssuerAndSubject(String provider, String issuer, String subject)`
- `List<OAuthAccountEntity> findByUserId(Long userId)`
- `List<OAuthAccountEntity> findByIdIn(List<Long> ids)`

#### OAuthAccountRepositoryImpl (어댑터)
- OAuthAccountRepository 인터페이스 구현
- OAuthAccountJpaRepository 조합하여 사용
- OAuthAccountMapper를 사용하여 도메인 모델 ↔ JPA 엔티티 변환

**의존성:**
- OAuthAccountJpaRepository (Spring Data JPA)
- OAuthAccountMapper (변환 담당)

#### OAuthAccountMapper (mapper 패키지)
- Entity ↔ Domain Model 변환 전용 클래스
- 정적 메서드로 구성

**주요 메서드:**
- `toEntity(OAuthAccount domain): OAuthAccountEntity`: 도메인 → JPA 엔티티 변환
- `toDomain(OAuthAccountEntity entity): OAuthAccount`: JPA 엔티티 → 도메인 변환

---

## 3. 매핑 전략

### 3.1 Value Object 매핑

도메인 VO는 JPA 엔티티에서 원시 타입으로 저장:

**OAuthAccountMapper.toEntity() 변환:**
- `Long id` → `Long id`
- `Long userId` → `Long userId`
- `OAuthProvider` → `String provider` (enum.name())
- `String issuer` → `String issuer`
- `String subject` → `String subject`
- `String email` → `String email`
- `LocalDateTime connectedAt` → `LocalDateTime connectedAt`

**OAuthAccountMapper.toDomain() 변환:**
- `Long id` → `Long id`
- `Long userId` → `Long userId`
- `String provider` → `OAuthProvider.valueOf(provider)`
- `String issuer` → `String issuer`
- `String subject` → `String subject`
- `String email` → `String email`
- `LocalDateTime connectedAt` → `LocalDateTime connectedAt`

### 3.2 변환 주의사항

- OAuthAccount 도메인 모델의 재구성용 생성자 사용
- userId는 nullable 허용 (최초 생성 시 null일 수 있음)
- provider는 enum String으로 저장/조회

---

## 4. TDD 구현 순서

### 4.1 도메인 계층 (테스트 우선)
1. **OAuthProvider enum 테스트 작성** → 구현 ✅
2. **OAuthAccount 도메인 모델 테스트 작성** → 구현 ✅
   - 생성자 검증
   - matches() 메서드 검증
   - isSameProvider() 메서드 검증
   - connectToUser() 메서드 검증
3. **OAuthAccountRepository 인터페이스 정의** ✅

### 4.2 Application 계층 (테스트 우선)
- Mock OAuthAccountRepository를 사용한 서비스 테스트 작성
- 실제 Repository 구현은 인프라 계층에서

### 4.3 인프라 계층 (TDD 제외)
1. OAuthAccountEntity 작성
2. OAuthAccountMapper 작성
3. OAuthAccountJpaRepository 인터페이스 작성
4. OAuthAccountRepositoryImpl 구현
5. 수동 통합 테스트로 검증 (선택)

### 4.4 주의 사항
- **인프라 계층은 TDD 적용 대상이 아님**
- JPA 설정, 쿼리 동작은 실제 DB 연동 후 확인
- 도메인/어플리케이션 계층은 Mock Repository로 테스트

---

## 5. 예외 처리

### 5.1 도메인 예외
- **조회 실패**: `Optional<OAuthAccount>` 반환 (예외 던지지 않음)
- **필수 파라미터 누락**: `InvalidUserArgumentException` (도메인 예외)
- **비즈니스 규칙 위반**: `InvalidUserStateException` (이미 연결된 계정)

### 5.2 인프라 예외
- **JPA 예외**: 인프라 계층에서 catch 후 적절히 처리
- **필요시 도메인 예외로 변환**: 외부 기술 예외를 도메인 의미로 변환

---

## 6. 트랜잭션 경계

- **OAuthAccountRepository는 트랜잭션을 관리하지 않음**
- 트랜잭션은 application 계층 (@Service)에서 @Transactional로 관리
- save 메서드는 호출자의 트랜잭션 컨텍스트 내에서 실행

---

## 7. 구현 체크리스트

### 7.1 도메인 계층
- [x] OAuthProvider enum 작성 (TDD)
- [x] OAuthAccount 도메인 모델 작성 (TDD)
- [x] OAuthAccountRepository 인터페이스 정의

### 7.2 인프라 계층
- [x] OAuthAccountEntity 작성 (JPA Entity)
- [x] OAuthAccountMapper 작성
  - [x] toEntity() 변환 메서드
  - [x] toDomain() 변환 메서드
- [x] OAuthAccountJpaRepository 인터페이스 작성
  - [x] findByProviderAndIssuerAndSubject() 쿼리 메서드
  - [x] findByUserId() 쿼리 메서드
  - [x] findByIdIn() 쿼리 메서드
- [x] OAuthAccountRepositoryImpl 구현
  - [x] save() 구현
  - [x] findByProviderAndIssuerAndSubject() 구현
  - [x] findById() 구현
  - [x] findByUserId() 구현
  - [x] findByIdIn() 구현

### 7.3 검증
- [x] application 계층에서 Mock OAuthAccountRepository 사용 테스트
- [ ] 실제 DB 연동 후 수동 검증

---

## 8. 참고 사항

### 8.1 패키지 구조

```
user/
├── domain/
│   ├── model/
│   │   ├── OAuthAccount.java ✅
│   │   ├── OAuthProvider.java ✅
│   │   └── OAuthIdentifier.java ✅
│   └── repository/
│       └── OAuthAccountRepository.java ✅
└── infrastructure/
    └── persistence/
        ├── OAuthAccountEntity.java ✅
        ├── OAuthAccountJpaRepository.java ✅
        ├── OAuthAccountRepositoryImpl.java ✅
        └── mapper/
            └── OAuthAccountMapper.java ✅
```

### 8.2 User와의 관계

- User 도메인 모델은 `List<Long> oauthAccountIds`로 OAuthAccount ID 목록 보유
- OAuthAccount 도메인 모델은 `Long userId`로 User 참조
- 양방향 참조이지만 모두 ID 기반으로 관리
- 실제 객체 참조는 Application 계층에서 조합

### 8.3 관련 문서

- [user-repository-design.md](./user-repository-design.md)
- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [phase-auth-setup.md](../plans/phase-auth-setup.md)

---

**작성일**: 2026-01-26
**최종 수정일**: 2026-01-26 (인프라 계층 구현 완료)