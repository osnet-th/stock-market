# Caffeine 캐시 설정 예시

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("stockPrice");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(500));
        return cacheManager;
    }
}
```

- 캐시 이름: `stockPrice`
- TTL: 30분 (쓰기 후 30분 경과 시 만료)
- 최대 500개 종목 캐싱
