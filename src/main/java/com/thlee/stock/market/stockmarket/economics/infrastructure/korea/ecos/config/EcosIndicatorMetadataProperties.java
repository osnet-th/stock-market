package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ECOS 경제지표 메타데이터 설정
 * ecos-indicator-metadata.yml에서 로딩
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ecos.indicator.metadata")
public class EcosIndicatorMetadataProperties {

    private List<IndicatorMeta> indicators = new ArrayList<>();

    /**
     * compareKey(className::keystatName) 기반 Map 변환
     */
    public Map<String, IndicatorMeta> toMap() {
        return indicators.stream()
            .collect(Collectors.toMap(
                meta -> meta.getClassName() + "::" + meta.getKeystatName(),
                meta -> meta,
                (a, b) -> b
            ));
    }

    @Getter
    @Setter
    public static class IndicatorMeta {
        private String className;
        private String keystatName;
        private String description;
        private PositiveDirection positiveDirection = PositiveDirection.NEUTRAL;
        private boolean keyIndicator;
    }

    public enum PositiveDirection {
        UP, DOWN, NEUTRAL
    }
}