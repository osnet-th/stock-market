# EcosCacheConfig 구현 예시

## build.gradle 의존성 추가

```groovy
implementation 'org.springframework.boot:spring-boot-starter-cache'
implementation 'com.github.ben-manes.caffeine:caffeine'
```

## EcosCacheConfig

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class EcosCacheConfig {

    public static final String ECOS_INDICATOR_CACHE = "ecosIndicators";

    @Bean
    public CacheManager ecosCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(ECOS_INDICATOR_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(15));
        return cacheManager;
    }
}
```

## Port 인터페이스 (domain 계층)

위치: `economics/domain/service/EcosIndicatorPort.java`

```java
package com.thlee.stock.market.stockmarket.economics.domain.service;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosKeyStatResult;

/**
 * ECOS 경제지표 조회 포트
 */
public interface EcosIndicatorPort {
    EcosKeyStatResult fetchKeyStatistics();
}
```

## Domain 모델 (domain 계층)

위치: `economics/domain/model/KeyStatIndicator.java`

```java
package com.thlee.stock.market.stockmarket.economics.domain.model;

/**
 * 경제지표 단건 도메인 모델
 */
public record KeyStatIndicator(
    String className,
    String keystatName,
    String dataValue,
    String cycle,
    String unitName
) {
}
```

위치: `economics/domain/model/EcosKeyStatResult.java`

```java
package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.util.List;

/**
 * ECOS 100대 경제지표 조회 결과
 */
public record EcosKeyStatResult(
    int totalCount,
    List<KeyStatIndicator> indicators
) {
}
```

## Port 구현체 (infrastructure 계층)

위치: `economics/infrastructure/korea/ecos/EcosIndicatorAdapter.java`

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosKeyStatResult;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.service.EcosIndicatorPort;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.dto.EcosKeyStatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EcosIndicatorAdapter implements EcosIndicatorPort {

    private final EcosApiClient ecosApiClient;

    @Override
    public EcosKeyStatResult fetchKeyStatistics() {
        EcosKeyStatResponse response = ecosApiClient.fetchKeyStatistics();

        if (response.getKeyStatisticList() == null || response.getKeyStatisticList().getRow() == null) {
            return new EcosKeyStatResult(0, Collections.emptyList());
        }

        List<KeyStatIndicator> indicators = response.getKeyStatisticList().getRow().stream()
            .map(row -> new KeyStatIndicator(
                row.getClassName(),
                row.getKeystatName(),
                row.getDataValue(),
                row.getCycle(),
                row.getUnitName()))
            .toList();

        return new EcosKeyStatResult(
            response.getKeyStatisticList().getListTotalCount(),
            indicators);
    }
}
```

## Service 적용 예시 (application 계층)

위치: `economics/application/EcosIndicatorService.java`

```java
package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosKeyStatResult;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;
import com.thlee.stock.market.stockmarket.economics.domain.service.EcosIndicatorPort;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config.EcosCacheConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EcosIndicatorService {

    private final EcosIndicatorPort ecosIndicatorPort;

    /**
     * 카테고리별 경제지표 조회 (24시간 캐싱)
     *
     * @param category 조회할 카테고리 (예: INTEREST_RATE → 시장금리, 여수신금리)
     * @return 해당 카테고리에 속하는 경제지표 목록
     */
    @Cacheable(
        cacheManager = "ecosCacheManager",
        cacheNames = EcosCacheConfig.ECOS_INDICATOR_CACHE,
        key = "#category.name()"
    )
    public List<KeyStatIndicator> getIndicatorsByCategory(EcosIndicatorCategory category) {
        EcosKeyStatResult result = ecosIndicatorPort.fetchKeyStatistics();

        return result.indicators().stream()
            .filter(indicator -> category.contains(indicator.className()))
            .toList();
    }
}
```