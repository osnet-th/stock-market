# Keyword 도메인 모델 변경 예시

## 변경 전

```java
public class Keyword {
    private Long id;
    private String keyword;
    private Long userId;       // 제거
    private boolean active;    // 제거 (user_keyword.active로 이관)
    private Region region;
    private LocalDateTime createdAt;

    public static Keyword create(String keyword, Long userId, Region region) { ... }
}
```

## 변경 후

```java
public class Keyword {
    private Long id;
    private String keyword;
    private Region region;
    private LocalDateTime createdAt;

    public static Keyword create(String keyword, Region region) {
        validateKeyword(keyword);
        validateRegion(region);
        return new Keyword(null, keyword, region, LocalDateTime.now());
    }

    public static Keyword reconstruct(Long id, String keyword,
                                       Region region, LocalDateTime createdAt) {
        return new Keyword(id, keyword, region, createdAt);
    }
}
```

## KeywordEntity 변경

```java
@Entity
@Table(name = "keyword",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"keyword", "region"})
       })
@Getter
public class KeywordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    // user_id 컬럼 제거
    // active 컬럼 제거 (user_keyword.active로 이관)

    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false, length = 20)
    private Region region;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```