package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "sec.api")
public class SecProperties {

    private String baseUrl;
    private String userAgent;
}