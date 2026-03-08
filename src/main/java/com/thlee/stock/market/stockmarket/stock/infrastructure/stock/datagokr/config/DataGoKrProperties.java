package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "stock.api.datagokr")
public class DataGoKrProperties {
    private String baseUrl;
    private String serviceKey;
    private int numOfRows = 100;
}