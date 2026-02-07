# KeywordRepository 구현체 설계

작성일: 2026-02-04

---

## 대상 파일

| 파일 | 경로 | 역할 |
|------|------|------|
| KeywordEntity | `keyword/infrastructure/persistence/KeywordEntity.java` | JPA Entity |
| KeywordJpaRepository | `keyword/infrastructure/persistence/KeywordJpaRepository.java` | Spring Data JPA 인터페이스 |
| KeywordRepositoryImpl | `keyword/infrastructure/persistence/KeywordRepositoryImpl.java` | Repository 어댑터 |
| KeywordMapper | `keyword/infrastructure/persistence/mapper/KeywordMapper.java` | Entity ↔ Domain 변환 |

---

## 변환 매핑 (Keyword Domain ↔ KeywordEntity)

| Domain (Keyword) | Entity (KeywordEntity) |
|------------------|------------------------|
| id (Long) | id (Long, @Id, IDENTITY) |
| keyword (String) | keyword (String, nullable=false, length=100) |
| userId (Long) | userId (Long, nullable=false) |
| active (boolean) | active (boolean, nullable=false, default=true) |
| createdAt (LocalDateTime) | createdAt (LocalDateTime, nullable=false, updatable=false, @PrePersist) |

---

## KeywordJpaRepository 메서드 구성

기존 `KeywordRepository` 인터페이스의 메서드를 매핑하여 구성합니다.

| KeywordRepository 메서드 | KeywordJpaRepository 대응 |
|--------------------------|---------------------------|
| save(Keyword) | save(KeywordEntity) — JpaRepository 기본 제공 |
| findById(Long) | findById(Long) — JpaRepository 기본 제공 |
| findByUserId(Long) | findByUserId(Long userId) — 쿼리 메서드 |
| findByUserIdAndActive(Long, boolean) | findByUserIdAndActive(Long userId, boolean active) — 쿼리 메서드 |
| findByActive(boolean) | findByActive(boolean active) — 쿼리 메서드 |
| delete(Keyword) | delete(KeywordEntity) — JpaRepository 기본 제공 |

---

## KeywordRepositoryImpl 구현 패턴

`NewsRepositoryImpl`과 동일한 패턴 적용:
- `@Repository`, `@RequiredArgsConstructor`
- `KeywordJpaRepository` 주입
- 모든 메서드에서 `KeywordMapper` 를 통해 변환 후 위임

---

## 제약 사항

- Entity 연관관계 사용 금지 (ID 기반 참조만)
- `@Transactional` 은 infrastructure 계층에서 사용하지 않음
- `createdAt` 은 `@PrePersist`로 자동 설정, Entity 생성자에서는 받아도 JPA에서는 override