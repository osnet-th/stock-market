package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.koreaap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 한국자산평가 공개 화면 조회 설정. 캐시·폴백 정책은 출처와 무관한 economics.bond-yield.* 에 둔다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "economics.api.korea.koreaap")
public class KoreaApProperties {
    private String baseUrl = "https://www.koreaap.com";
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 7000;
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
}
