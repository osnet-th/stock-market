# fetchAndSave() 수정 예시

## EcosIndicatorRepository

```java
public interface EcosIndicatorRepository {

    List<EcosIndicator> saveAll(List<EcosIndicator> indicators);

    /**
     * 히스토리 데이터 존재 여부 확인
     */
    boolean existsAny();
}
```

## EcosIndicatorJpaRepository

```java
public interface EcosIndicatorJpaRepository extends JpaRepository<EcosIndicatorEntity, Long> {

    boolean existsFirstBy();
}
```

## EcosIndicatorRepositoryImpl

```java
@Override
public boolean existsAny() {
    return jpaRepository.existsFirstBy();
}
```

## EcosIndicatorSaveService.fetchAndSave()

```java
@Transactional
public int fetchAndSave() {
    try {
        LocalDate today = LocalDate.now();

        // 1. API 조회 (1회)
        EcosKeyStatResult result = ecosIndicatorPort.fetchKeyStatistics();

        // 2. 카테고리별 캐시 적재
        putCacheByCategory(result.indicators());

        // 3. 유효 지표 필터링
        List<KeyStatIndicator> validIndicators = result.validIndicators();

        // 4. 히스토리 존재 여부 확인
        boolean historyExists = ecosIndicatorRepository.existsAny();

        if (!historyExists) {
            // 초기 시딩: 전체 저장
            return initialSeed(validIndicators, today);
        }

        // 기존 로직: cycle 비교 → 변경분만 저장
        return saveChangedIndicators(validIndicators, today);
    } catch (Exception e) {
        log.error(e.getMessage(), e);
        return 0;
    }
}

/**
 * 초기 시딩: 히스토리가 비어있을 때 전체 지표 저장
 */
private int initialSeed(List<KeyStatIndicator> validIndicators, LocalDate today) {
    List<EcosIndicator> allIndicators = validIndicators.stream()
            .filter(indicator -> indicator.cycle() != null)
            .map(indicator -> EcosIndicator.fromKeyStatIndicator(indicator, today))
            .toList();

    ecosIndicatorRepository.saveAll(allIndicators);

    List<EcosIndicatorLatest> latestList = validIndicators.stream()
            .map(EcosIndicatorLatest::fromKeyStatIndicator)
            .toList();
    ecosIndicatorLatestRepository.saveAll(latestList);

    log.info("ECOS 초기 시딩 완료: date={}, count={}", today, allIndicators.size());
    return allIndicators.size();
}

/**
 * 기존 로직: latest cycle 비교 → 변경분만 히스토리 저장
 */
private int saveChangedIndicators(List<KeyStatIndicator> validIndicators, LocalDate today) {
    // latest 전체 조회 (1회) → Map 변환
    Map<String, String> latestCycleMap = new HashMap<>();
    for (EcosIndicatorLatest latest : ecosIndicatorLatestRepository.findAll()) {
        latestCycleMap.put(latest.toCompareKey(), latest.getCycle());
    }

    // 변경분 추출
    List<EcosIndicator> changedIndicators = validIndicators.stream()
            .filter(indicator -> isCycleChanged(indicator, latestCycleMap))
            .map(indicator -> EcosIndicator.fromKeyStatIndicator(indicator, today))
            .toList();

    if (!changedIndicators.isEmpty()) {
        ecosIndicatorRepository.saveAll(changedIndicators);
        log.info("ECOS 히스토리 저장 완료: date={}, count={}", today, changedIndicators.size());
    } else {
        log.info("ECOS 지표 변경 없음, 히스토리 저장 스킵");
    }

    // latest 전체 갱신
    List<EcosIndicatorLatest> latestList = validIndicators.stream()
            .map(EcosIndicatorLatest::fromKeyStatIndicator)
            .toList();
    ecosIndicatorLatestRepository.saveAll(latestList);

    return changedIndicators.size();
}
```