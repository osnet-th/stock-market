# UserKeyword Repository 예시

## 도메인 인터페이스

```java
package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.UserKeyword;

import java.util.List;
import java.util.Optional;

public interface UserKeywordRepository {
    UserKeyword save(UserKeyword userKeyword);
    Optional<UserKeyword> findByUserIdAndKeywordId(Long userId, Long keywordId);
    List<UserKeyword> findByUserId(Long userId);
    List<UserKeyword> findByUserIdAndActive(Long userId, boolean active);
    List<UserKeyword> findByKeywordId(Long keywordId);
    boolean existsByKeywordId(Long keywordId);
    void delete(UserKeyword userKeyword);
    void deleteByUserIdAndKeywordId(Long userId, Long keywordId);
}
```

## JPA Repository

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserKeywordJpaRepository extends JpaRepository<UserKeywordEntity, Long> {
    Optional<UserKeywordEntity> findByUserIdAndKeywordId(Long userId, Long keywordId);
    List<UserKeywordEntity> findByUserId(Long userId);
    List<UserKeywordEntity> findByUserIdAndActive(Long userId, boolean active);
    List<UserKeywordEntity> findByKeywordId(Long keywordId);
    boolean existsByKeywordId(Long keywordId);
    void deleteByUserIdAndKeywordId(Long userId, Long keywordId);
}
```

## Mapper

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.UserKeyword;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.UserKeywordEntity;

public class UserKeywordMapper {

    public static UserKeyword toDomain(UserKeywordEntity entity) {
        return UserKeyword.reconstruct(
                entity.getId(),
                entity.getUserId(),
                entity.getKeywordId(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static UserKeywordEntity toEntity(UserKeyword domain) {
        return new UserKeywordEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getKeywordId(),
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
```