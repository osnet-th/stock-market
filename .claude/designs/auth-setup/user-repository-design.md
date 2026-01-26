# UserRepository 구현 설계

## 1. 설계 개요

UserRepository는 사용자 도메인 모델의 영속성을 담당하는 리포지토리입니다.
도메인 계층의 인터페이스(포트)와 인프라 계층의 JPA 구현체(어댑터)로 분리하여 구현합니다.

### 핵심 설계 원칙

- **도메인 모델과 JPA 엔티티 분리**: User(도메인) ↔ UserEntity(인프라)
- **ID 기반 참조**: Entity 연관관계 사용 금지, Long 타입 ID로 참조
- **의존성 방향**: 인프라 계층이 도메인 계층에 의존 (역방향 금지)
- **TDD 적용**: domain/application 계층만 (infrastructure는 제외)
- **UserMapper 사용**: Entity ↔ Domain Model 변환 전용 클래스

---

## 2. 구성 요소

### 2.1 도메인 계층 (domain/repository)

#### UserRepository 인터페이스 ✅
- 도메인 모델(User)을 다루는 포트 인터페이스
- 영속성 기술에 무관한 순수 도메인 메서드 정의

**주요 메서드:**
- `save(User user): User`: 사용자 저장/수정
- `findById(Long id): Optional<User>`: ID로 사용자 조회
- `existsByNickname(Nickname nickname): boolean`: 닉네임 중복 확인
- `findByNicknameAndPhoneNumber(Nickname nickname, PhoneNumber phoneNumber): Optional<User>`: 계정 연동용 조회

### 2.2 인프라 계층 (infrastructure/persistence)

#### UserEntity
- JPA 엔티티 (@Entity, @Table 등 JPA 애노테이션 사용)
- 도메인 User와 독립적으로 존재
- 데이터베이스 users 테이블 매핑

**주요 필드:**
- id (Long, @Id @GeneratedValue)
- name (String)
- nickname (String)
- phoneNumber (String, nullable)
- status (String - enum 저장: ACTIVE, INACTIVE, SUSPENDED, DELETED)
- role (String - enum 저장: USER, ADMIN, SIGNING_USER)
- createdAt, deletedAt (LocalDateTime)

**연관관계:**
- **연관관계 사용 금지** - ID 기반 참조만 허용
- OAuthAccount와의 관계는 oauthAccountIds(List<Long>)로 도메인에서 관리

#### UserJpaRepository (Spring Data JPA)
- `JpaRepository<UserEntity, Long>` 확장
- Spring Data JPA 쿼리 메서드 정의

**쿼리 메서드:**
- `Optional<UserEntity> findByEmail(String email)`
- 추가 쿼리 메서드는 필요시 정의

#### UserRepositoryImpl (어댑터)
- UserRepository 인터페이스 구현
- UserJpaRepository 조합하여 사용
- UserMapper를 사용하여 도메인 모델 ↔ JPA 엔티티 변환

**의존성:**
- UserJpaRepository (Spring Data JPA)
- UserMapper (변환 담당)

#### UserMapper (mapper 패키지)
- Entity ↔ Domain Model 변환 전용 클래스
- 정적 메서드로 구성

**주요 메서드:**
- `toEntity(User domain): UserEntity`: 도메인 → JPA 엔티티 변환
- `toDomain(UserEntity entity): User`: JPA 엔티티 → 도메인 변환

---

## 3. 매핑 전략

### 3.1 Value Object 매핑

도메인 VO는 JPA 엔티티에서 원시 타입으로 저장:

**UserMapper.toEntity() 변환:**
- `Long id` → `Long id`
- `String name` → `String name`
- `Nickname.getValue()` → `String nickname`
- `PhoneNumber.getValue()` → `String phoneNumber`
- `UserStatus` → `String status` (enum.name())
- `UserRole` → `String role` (enum.name())
- `List<Long> oauthAccountIds` → 저장 안함 (별도 OAuthAccount 테이블에서 관리)

**UserMapper.toDomain() 변환:**
- `Long id` → `Long id`
- `String name` → `String name`
- `String nickname` → `new Nickname(nickname)`
- `String phoneNumber` → `new PhoneNumber(phoneNumber)`
- `String status` → `UserStatus.valueOf(status)`
- `String role` → `UserRole.valueOf(role)`
- OAuthAccount는 OAuthAccountRepository를 통해 별도 조회

### 3.2 변환 예시

```java
// toEntity 예시
public static UserEntity toEntity(User user) {
    return UserEntity.builder()
        .id(user.getId())
        .name(user.getName())
        .nickname(user.getNickname().getValue())
        .phoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber().getValue() : null)
        .status(user.getStatus().name())
        .role(user.getRole().name())
        .createdAt(user.getCreatedAt())
        .deletedAt(user.getDeletedAt())
        .build();
}

// toDomain 예시
public static User toDomain(UserEntity entity, List<Long> oauthAccountIds) {
    return new User(
        entity.getId(),
        entity.getName(),
        entity.getNickname() != null ? new Nickname(entity.getNickname()) : null,
        entity.getPhoneNumber() != null ? new PhoneNumber(entity.getPhoneNumber()) : null,
        oauthAccountIds,
        UserStatus.valueOf(entity.getStatus()),
        UserRole.valueOf(entity.getRole()),
        entity.getCreatedAt(),
        entity.getDeletedAt()
    );
}

---

## 4. TDD 구현 순서

### 4.1 도메인 계층 (테스트 우선)
1. **UserId VO 테스트 작성** → 구현
   - null 검증
   - from() 팩토리 메서드
2. **Email VO 테스트 작성** → 구현
   - 이메일 형식 검증
   - null/빈 값 검증
3. **User 도메인 모델 테스트 작성** → 구현
   - 생성자/빌더 검증
   - 도메인 행위 메서드
4. **UserRepository 인터페이스 정의** (구현 없음)

### 4.2 Application 계층 (테스트 우선)
- Mock UserRepository를 사용한 서비스 테스트 작성
- 실제 Repository 구현은 인프라 계층에서

### 4.3 인프라 계층 (TDD 제외)
1. UserEntity 작성
2. UserMapper 작성
3. UserJpaRepository 인터페이스 작성
4. UserRepositoryImpl 구현
5. 수동 통합 테스트로 검증 (선택)

### 4.4 주의 사항
- **인프라 계층은 TDD 적용 대상이 아님**
- JPA 설정, 쿼리 동작은 실제 DB 연동 후 확인
- 도메인/어플리케이션 계층은 Mock Repository로 테스트

---

## 5. 예외 처리

### 5.1 도메인 예외
- **조회 실패**: `Optional<User>` 반환 (예외 던지지 않음)
- **Email 형식 오류**: `InvalidEmailFormatException` (도메인 예외)
- **Email 필수 입력**: `EmailRequiredException` (도메인 예외)
- **비즈니스 규칙 위반**: 도메인 서비스/엔티티에서 UserDomainException 계열 발생

### 5.2 인프라 예외
- **JPA 예외**: 인프라 계층에서 catch 후 적절히 처리
- **필요시 도메인 예외로 변환**: 외부 기술 예외를 도메인 의미로 변환

---

## 6. 트랜잭션 경계

- **UserRepository는 트랜잭션을 관리하지 않음**
- 트랜잭션은 application 계층 (@Service)에서 @Transactional로 관리
- save/delete 메서드는 호출자의 트랜잭션 컨텍스트 내에서 실행

---

## 7. 구현 체크리스트

### 7.1 도메인 계층
- [x] Nickname VO 작성 (TDD)
- [x] PhoneNumber VO 작성 (TDD)
- [x] User 도메인 모델 작성 (TDD)
- [x] UserRepository 인터페이스 정의

### 7.2 인프라 계층
- [x] UserEntity 작성 (JPA Entity)
- [x] UserMapper 작성
  - [x] toEntity() 변환 메서드
  - [x] toDomain() 변환 메서드
- [x] UserJpaRepository 인터페이스 작성
  - [x] existsByNickname() 쿼리 메서드
  - [x] findByNicknameAndPhoneNumber() 쿼리 메서드
- [x] UserRepositoryImpl 구현
  - [x] save() 구현
  - [x] findById() 구현
  - [x] existsByNickname() 구현
  - [x] findByNicknameAndPhoneNumber() 구현

### 7.3 검증
- [x] application 계층에서 Mock UserRepository 사용 테스트 (완료)
- [ ] 실제 DB 연동 후 수동 검증

---

## 8. 참고 사항

### 8.1 현재 구현된 패키지 구조
```
user/
├── domain/
│   ├── model/
│   │   ├── User.java ✅
│   │   ├── Nickname.java ✅
│   │   ├── PhoneNumber.java ✅
│   │   ├── OAuthAccount.java ✅
│   │   ├── OAuthIdentifier.java ✅
│   │   ├── OAuthProvider.java ✅
│   │   ├── UserRole.java ✅
│   │   └── UserStatus.java ✅
│   └── repository/
│       ├── UserRepository.java ✅
│       └── OAuthAccountRepository.java ✅
└── infrastructure/
    └── persistence/
        ├── UserEntity.java ✅
        ├── UserJpaRepository.java ✅
        ├── UserRepositoryImpl.java ✅
        └── mapper/
            └── UserMapper.java ✅
```

### 8.2 관련 문서
- [auth-package-structure.md](./auth-package-structure.md)
- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [phase-auth-setup.md](../plans/phase-auth-setup.md)

---

**작성일**: 2026-01-26
**최종 수정일**: 2026-01-26 (인프라 계층 구현 완료)