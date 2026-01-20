# Phase 4: 커뮤니티 기능 (Week 7-8)

## 4.1 게시글 도메인 모델 (TDD)

### 테스트 → 구현 순서

1. [ ] Post 도메인 모델 테스트 작성 (생성, 수정, 삭제 규칙)
2. [ ] Post 도메인 모델 최소 구현
3. [ ] 게시글 권한 검증 도메인 서비스 테스트 작성
4. [ ] 게시글 권한 검증 도메인 서비스 최소 구현

### 대상 파일
- `post/domain/model/Post.java` (테스트 먼저)
- `post/domain/service/PostDomainService.java` (테스트 먼저)
- `post/domain/repository/PostRepository.java` (인터페이스)

---

## 4.2 게시글 저장소 구현 (TDD)

### 테스트 → 구현 순서

1. [ ] Post 리포지토리 구현 테스트 작성
2. [ ] Post 리포지토리 최소 구현
3. [ ] Post Entity 작성 (ID 기반 참조만 사용)

### 대상 파일
- `post/infrastructure/entity/PostEntity.java`
- `post/infrastructure/repository/PostRepositoryImpl.java` (테스트 먼저)

### Entity 작성 규칙

**Entity 작성 전 사전 승인 필요**

```java
// ❌ 금지: Entity 연관관계
@Entity
public class PostEntity {
    @ManyToOne
    private UserEntity user;
}

// ✅ 허용: ID 기반 참조
@Entity
public class PostEntity {
    private Long userId;       // ID 기반 참조
    private String stockCode;  // ID 기반 참조
}
```

---

## 4.3 게시글 관리 유스케이스 (TDD)

### 테스트 → 구현 순서

1. [ ] 게시글 작성 유스케이스 테스트 작성
2. [ ] 게시글 작성 유스케이스 최소 구현
3. [ ] 게시글 수정 유스케이스 테스트 작성
4. [ ] 게시글 수정 유스케이스 최소 구현
5. [ ] 게시글 삭제 유스케이스 테스트 작성
6. [ ] 게시글 삭제 유스케이스 최소 구현
7. [ ] 게시글 조회 유스케이스 테스트 작성
8. [ ] 게시글 조회 유스케이스 최소 구현

### 대상 파일
- `post/application/PostCommandService.java` (테스트 먼저)
- `post/application/PostQueryService.java` (테스트 먼저)
- `post/presentation/PostController.java` (테스트 먼저)

---

## 도메인 간 참조 규칙

### Post 도메인 모델
```java
// post/domain/model/Post.java
public class Post {
    private Long userId;          // ✅ User 도메인의 ID만 참조
    private String stockCode;     // ✅ Stock 도메인의 식별자만 참조
}
```

### Application 계층에서 조합
```java
// post/application/PostQueryService.java
public PostWithUserResponse findPostWithUser(Long postId) {
    Post post = postRepository.findById(postId);
    // 별도로 사용자 정보 조회
    User user = userQueryService.findById(post.getUserId());
    return PostWithUserResponse.of(post, user);
}
```

---

## 데이터베이스 스키마

```sql
CREATE TABLE posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,      -- ID 기반 참조
    stock_code VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
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

---

## 참고 문서

- [ARCHITECTURE.md](../../ARCHITECTURE.md)
- [STOCK-MARKET-PROJECT.md](../../STOCK-MARKET-PROJECT.md)
- [CLAUDE.md](../../CLAUDE.md)

---

**작성일**: 2026-01-19
**작성자**: Claude Code