# 애플리케이션 서비스 예시

## EcosIndicatorMetadataService

```java
package com.thlee.stock.market.stockmarket.economics.application;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.domain.repository.EcosIndicatorMetadataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ECOS 경제지표 메타데이터 조회 서비스
 * 앱 시작 시 DB에서 로딩하여 메모리 캐싱
 */
@Service
@RequiredArgsConstructor
public class EcosIndicatorMetadataService {

    private final EcosIndicatorMetadataRepository metadataRepository;
    private final Map<String, EcosIndicatorMetadata> metadataCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadCache() {
        List<EcosIndicatorMetadata> all = metadataRepository.findAll();
        Map<String, EcosIndicatorMetadata> map = all.stream()
            .collect(Collectors.toMap(
                EcosIndicatorMetadata::toCompareKey,
                meta -> meta,
                (a, b) -> b
            ));
        metadataCache.clear();
        metadataCache.putAll(map);
    }

    /**
     * compareKey 기반 메타데이터 Map 반환
     */
    public Map<String, EcosIndicatorMetadata> getMetadataMap() {
        return metadataCache;
    }

    /**
     * 캐시 갱신 (데이터 변경 시 호출)
     */
    public void refreshCache() {
        loadCache();
    }
}
```