# EcosCacheConfig 수정 예시

```java
@Configuration
@EnableCaching
public class EcosCacheConfig {

    public static final String ECOS_INDICATOR_CACHE = "ecosIndicators";

    @Bean
    public CacheManager ecosCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(ECOS_INDICATOR_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(25, TimeUnit.HOURS) // 배치 주기(24h)보다 약간 길게
            .maximumSize(15));
        return cacheManager;
    }
}
```
