package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kis.api")
public class KisProperties {

    private String url;
    private String key;
    private String secret;
    private String account;
    private Master master = new Master();

    /** RestClient connect timeout (ms). 기본 3000. */
    private long connectTimeoutMs = 3000L;

    /** RestClient read timeout (ms). 기본 10000. */
    private long readTimeoutMs = 10000L;

    @Getter
    @Setter
    public static class Master {
        private String baseUrl;
    }
}