# News 도메인 모델 변경 예시

## 변경 전

```java
public class News {
    private Long id;
    private String originalUrl;
    private Long userId;           // 제거
    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private NewsPurpose purpose;   // 제거
    private Long sourceId;         // 제거
    private Region region;
}
```

## 변경 후

```java
public class News {
    private Long id;
    private String originalUrl;
    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private Long keywordId;        // 추가
    private Region region;

    public static News create(String originalUrl, String title, String content,
                               LocalDateTime publishedAt, Long keywordId, Region region) {
        validateOriginalUrl(originalUrl);
        validateTitle(title);
        validateKeywordId(keywordId);
        return new News(null, originalUrl, title, content, publishedAt,
                        LocalDateTime.now(), keywordId, region);
    }

    public static News reconstruct(Long id, String originalUrl, String title,
                                    String content, LocalDateTime publishedAt,
                                    LocalDateTime createdAt, Long keywordId, Region region) {
        return new News(id, originalUrl, title, content, publishedAt,
                        createdAt, keywordId, region);
    }
}
```