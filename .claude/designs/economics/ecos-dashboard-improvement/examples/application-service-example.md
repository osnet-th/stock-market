# 애플리케이션 서비스 예시

## EcosIndicatorSaveService 배치 로직 변경

### saveChangedIndicators 변경

```java
private int saveChangedIndicators(List<KeyStatIndicator> validIndicators, LocalDate today) {
    // latest 전체 조회 → Map<compareKey, EcosIndicatorLatest> (dataValue 포함)
    Map<String, EcosIndicatorLatest> latestMap = new HashMap<>();
    for (EcosIndicatorLatest latest : ecosIndicatorLatestRepository.findAll()) {
        latestMap.put(latest.toCompareKey(), latest);
    }

    // 변경분 추출 + 히스토리 저장
    List<EcosIndicator> changedIndicators = validIndicators.stream()
            .filter(indicator -> isCycleChanged(indicator, latestMap))
            .map(indicator -> EcosIndicator.fromKeyStatIndicator(indicator, today))
            .toList();

    if (!changedIndicators.isEmpty()) {
        ecosIndicatorRepository.saveAll(changedIndicators);
        log.info("ECOS 히스토리 저장 완료: date={}, count={}", today, changedIndicators.size());
    }

    // latest 전체 갱신 — dataValue + previousDataValue 보존
    List<EcosIndicatorLatest> latestList = validIndicators.stream()
            .map(indicator -> {
                EcosIndicatorLatest existing = latestMap.get(indicator.toCompareKey());
                boolean cycleChanged = existing != null
                    && indicator.cycle() != null
                    && !indicator.cycle().equals(existing.getCycle());

                String previousDataValue = existing != null
                    ? (cycleChanged ? existing.getDataValue() : existing.getPreviousDataValue())
                    : null;

                return new EcosIndicatorLatest(
                    indicator.className(),
                    indicator.keystatName(),
                    indicator.dataValue(),
                    previousDataValue,
                    indicator.cycle(),
                    LocalDateTime.now()
                );
            })
            .toList();
    ecosIndicatorLatestRepository.saveAll(latestList);

    return changedIndicators.size();
}
```

### isCycleChanged 시그니처 변경

```java
private boolean isCycleChanged(KeyStatIndicator indicator, Map<String, EcosIndicatorLatest> latestMap) {
    String apiCycle = indicator.cycle();
    EcosIndicatorLatest latest = latestMap.get(indicator.toCompareKey());

    if (apiCycle == null) return false;
    if (latest == null) return true;
    return !latest.getCycle().equals(apiCycle);
}
```

### putCacheByCategory 변경 — previousDataValue pre-merge

```java
private void putCacheByCategory(List<KeyStatIndicator> indicators,
                                 Map<String, EcosIndicatorLatest> latestMap) {
    Cache cache = ecosCacheManager.getCache(EcosCacheConfig.ECOS_INDICATOR_CACHE);
    if (cache == null) return;

    // previousDataValue 병합
    List<KeyStatIndicator> enriched = indicators.stream()
        .map(ind -> {
            EcosIndicatorLatest latest = latestMap.get(ind.toCompareKey());
            String prevValue = latest != null ? latest.getPreviousDataValue() : null;
            return ind.withPreviousDataValue(prevValue);
        })
        .toList();

    Map<EcosIndicatorCategory, List<KeyStatIndicator>> grouped = enriched.stream()
        .filter(ind -> EcosIndicatorCategory.fromClassName(ind.className()) != null)
        .collect(Collectors.groupingBy(
            ind -> EcosIndicatorCategory.fromClassName(ind.className())
        ));

    for (EcosIndicatorCategory category : EcosIndicatorCategory.values()) {
        cache.put(category.name(), grouped.getOrDefault(category, List.of()));
    }
}
```

## EcosIndicatorService 메타데이터 머지

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class EcosIndicatorService {

    private final EcosIndicatorPort ecosIndicatorPort;
    private final CacheManager ecosCacheManager;
    private final EcosIndicatorMetadataProperties metadataProperties;

    @SuppressWarnings("unchecked")
    public List<KeyStatIndicator> getIndicatorsByCategory(EcosIndicatorCategory category) {
        Cache cache = ecosCacheManager.getCache(EcosCacheConfig.ECOS_INDICATOR_CACHE);

        List<KeyStatIndicator> indicators;
        if (cache != null) {
            List<KeyStatIndicator> cached = cache.get(category.name(), List.class);
            if (cached != null) {
                indicators = cached;
            } else {
                log.warn("ECOS 캐시 miss, API fallback: category={}", category);
                EcosKeyStatResult result = ecosIndicatorPort.fetchKeyStatistics();
                indicators = result.indicators().stream()
                    .filter(ind -> category.contains(ind.className()))
                    .toList();
            }
        } else {
            indicators = List.of();
        }

        return indicators;
    }

    /**
     * 메타데이터를 포함한 지표 조회 (presentation 레이어에서 사용)
     */
    public IndicatorMetadata getMetadata(String compareKey) {
        return metadataProperties.getIndicators().get(compareKey);
    }
}
```