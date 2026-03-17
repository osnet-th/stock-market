# 인프라 설정 예시

## EcosIndicatorMetadataProperties

위치: `economics/infrastructure/korea/ecos/config/EcosIndicatorMetadataProperties.java`

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.ecos.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ecos.indicator.metadata")
public class EcosIndicatorMetadataProperties {

    private Map<String, IndicatorMeta> indicators = new HashMap<>();

    @Getter
    @Setter
    public static class IndicatorMeta {
        private String description;
        private PositiveDirection positiveDirection = PositiveDirection.NEUTRAL;
        private boolean keyIndicator;
    }

    public enum PositiveDirection {
        UP, DOWN, NEUTRAL
    }
}
```

## ecos-indicator-metadata.yml

위치: `src/main/resources/ecos-indicator-metadata.yml`

```yaml
ecos:
  indicator:
    metadata:
      indicators:
        # === 금리 (INTEREST_RATE) ===
        "시장금리::한국은행 기준금리":
          description: "한국은행 기준금리 — 금융기관 간 RP 매매 기준"
          positive-direction: DOWN
          key-indicator: true
        "시장금리::CD(91일)":
          description: "91일 만기 양도성예금증서 유통수익률"
          positive-direction: DOWN
          key-indicator: true

        # === 환율 (EXCHANGE_RATE) ===
        "환율::원/달러":
          description: "원/달러 환율 — 원화 강세 시 하락"
          positive-direction: DOWN
          key-indicator: true

        # === 물가 (PRICE) ===
        "소비자/생산자 물가::소비자물가지수":
          description: "소비자물가 변동을 측정하는 종합 지수"
          positive-direction: DOWN
          key-indicator: true

        # === 성장/소득 (GROWTH_INCOME) ===
        "성장률::GDP성장률":
          description: "국내총생산 성장률 — 경제 성장 속도"
          positive-direction: UP
          key-indicator: true

        # === 고용/노동 (EMPLOYMENT_LABOR) ===
        "고용::실업률":
          description: "경제활동인구 중 실업자 비율"
          positive-direction: DOWN
          key-indicator: true

        # === 주식/채권 (STOCK_BOND) ===
        "주식::KOSPI":
          description: "한국 종합주가지수"
          positive-direction: UP
          key-indicator: true
        "주식::KOSDAQ":
          description: "코스닥 종합주가지수"
          positive-direction: UP
          key-indicator: true

        # === 경기심리 (SENTIMENT) ===
        "심리지표::CSI":
          description: "소비자심리지수 — 100 이상이면 낙관적"
          positive-direction: UP
          key-indicator: true

        # 참고: 실제 keystatName 값은 ECOS API 응답에서 확인 후 조정 필요
```

## application.yml 변경

```yaml
spring:
  config:
    import:
      - optional:classpath:ecos-indicator-metadata.yml
```