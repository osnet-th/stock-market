package com.thlee.stock.market.stockmarket.portfolio.infrastructure.goldprice;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gold.api")
public class GoldPriceProperties {
    private String baseUrl;
    private String serviceKey;
    private int cacheTtlMinutes = 60;
    private int failureBackoffMinutes = 5;
    private int lookbackDays = 14;
}
