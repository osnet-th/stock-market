package com.thlee.stock.market.stockmarket.realestate.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 부동산 도메인 외부 출처 설정. application.yml의 {@code realestate.*} 블록에서 주입된다.
 * <p>
 * 인증키 미설정 출처는 부팅을 막지 않으며, 일배치 fetch 시점에 어댑터가 IllegalStateException을
 * 던져 스케줄러가 해당 항목만 skip하고 로그를 남긴다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "realestate")
public class RealEstateMarketProperties {

    private SourceProps molit = new SourceProps();
    private SourceProps reb = new SourceProps();
    private SourceProps hub = new SourceProps();
    private SourceProps kosis = new SourceProps();
    private SourceProps subscriptionHome = new SourceProps();
    private SourceProps hug = new SourceProps();
    private SourceProps seoulOpenData = new SourceProps();
    private SourceProps ggDataDream = new SourceProps();

    @Getter
    @Setter
    public static class SourceProps {
        private String baseUrl;
        private String apiKey;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 30000;
        /** 일일 호출 한도 (정보 표시용, 실제 throttle은 어댑터 책임). */
        private Integer dailyQuota;
    }
}