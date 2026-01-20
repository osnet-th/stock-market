# Phase 5: 관심/즐겨찾기 (Week 9)

## 5.1 관심/즐겨찾기 도메인 모델 (TDD)

### 테스트 → 구현 순서

1. [ ] Favorite 도메인 모델 테스트 작성
2. [ ] Favorite 도메인 모델 최소 구현
3. [ ] 중복 등록 방지 도메인 서비스 테스트 작성
4. [ ] 중복 등록 방지 도메인 서비스 최소 구현

### 대상 파일
- `favorite/domain/model/Favorite.java` (테스트 먼저)
- `favorite/domain/model/FavoriteType.java` (enum: STOCK, NEWS)
- `favorite/domain/service/FavoriteDomainService.java` (테스트 먼저)
- `favorite/domain/repository/FavoriteRepository.java` (인터페이스)

---

## 5.2 관심/즐겨찾기 저장소 구현 (TDD)

### 테스트 → 구현 순서

1. [ ] Favorite 리포지토리 구현 테스트 작성
2. [ ] Favorite 리포지토리 최소 구현
3. [ ] Favorite Entity 작성

### 대상 파일
- `favorite/infrastructure/entity/FavoriteEntity.java`
- `favorite/infrastructure/repository/FavoriteRepositoryImpl.java` (테스트 먼저)

### Entity 작성 규칙

**Entity 작성 전 사전 승인 필요**

```java
// ✅ 허용: ID 기반 참조
@Entity
public class FavoriteEntity {
    private Long userId;           // User 도메인 ID 참조
    private String targetType;     // STOCK, NEWS
    private String targetId;       // 대상의 식별자 (stockCode 또는 newsId)
}
```

---

## 5.3 관심/즐겨찾기 유스케이스 (TDD)

### 테스트 → 구현 순서

1. [ ] 관심 등록 유스케이스 테스트 작성
2. [ ] 관심 등록 유스케이스 최소 구현
3. [ ] 관심 해제 유스케이스 테스트 작성
4. [ ] 관심 해제 유스케이스 최소 구현
5. [ ] 관심 목록 조회 유스케이스 테스트 작성
6. [ ] 관심 목록 조회 유스케이스 최소 구현

### 대상 파일
- `favorite/application/FavoriteCommandService.java` (테스트 먼저)
- `favorite/application/FavoriteQueryService.java` (테스트 먼저)
- `favorite/presentation/FavoriteController.java` (테스트 먼저)

---

## 도메인 설계

### Favorite 도메인 모델
```java
// favorite/domain/model/Favorite.java
public class Favorite {
    private Long id;
    private Long userId;           // ✅ User 도메인의 ID만 참조
    private FavoriteType type;     // STOCK, NEWS
    private String targetId;       // 대상의 식별자
    private LocalDateTime createdAt;
}

// favorite/domain/model/FavoriteType.java
public enum FavoriteType {
    STOCK,
    NEWS
}
```

### 중복 등록 방지
```java
// favorite/domain/service/FavoriteDomainService.java
public class FavoriteDomainService {
    public void validateDuplicateFavorite(Long userId, FavoriteType type, String targetId) {
        // 중복 등록 검증 로직
    }
}
```

---

## 데이터베이스 스키마

```sql
CREATE TABLE favorites (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,           -- ID 기반 참조
    target_type VARCHAR(20) NOT NULL,  -- STOCK, NEWS
    target_id VARCHAR(255) NOT NULL,   -- stockCode 또는 newsId
    created_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
);
```

---

## 제약 사항

- **DDD 계층형 구조 준수 필수**
- **Entity 연관관계 사용 금지** (ID 기반 참조만)
- **Entity 작성 전 사전 승인 필요**
- **domain 계층에 Spring/JPA 의존성 금지**
- **@Transactional은 application 계층에서만 사용**
- **테스트 실패 → 최소 구현 → 테스트 성공 순서 준수**
- **Mock은 테스트 대상의 경계에서만 사용**
- **중복 등록 방지를 위한 유니크 제약 조건 필수**

---

## 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [STOCK-MARKET-PROJECT.md](../../STOCK-MARKET-PROJECT.md)
- [CLAUDE.md](../../CLAUDE.md)

---

**작성일**: 2026-01-19
**작성자**: Claude Code