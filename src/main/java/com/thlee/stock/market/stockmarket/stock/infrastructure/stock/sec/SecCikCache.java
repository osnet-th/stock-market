package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.config.SecProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecCompanyTicker;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SecCikCache {

    private static final String CACHE_KEY = "tickerCikMap";

    private final RestClient restClient;
    private final SecProperties secProperties;

    private final Cache<String, Map<String, Long>> cache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    public SecCikCache(@Qualifier("secRestClient") RestClient restClient, SecProperties secProperties) {
        this.restClient = restClient;
        this.secProperties = secProperties;
    }

    @Scheduled(fixedRate = 23, timeUnit = TimeUnit.HOURS)
    public void refreshCache() {
        try {
            String json = restClient.get()
                    .uri("https://www.sec.gov/files/company_tickers.json")
                    .retrieve()
                    .body(String.class);

            if (json == null || json.isBlank()) {
                throw new SecApiException(SecErrorType.API_ERROR, "SEC company_tickers.json 응답이 비어있습니다");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, SecCompanyTicker> rawMap = objectMapper.readValue(
                    json, new TypeReference<>() {});

            Map<String, Long> mapping = rawMap.values().stream()
                    .filter(t -> t.getTicker() != null && t.getCikStr() != null)
                    .collect(Collectors.toMap(
                            t -> t.getTicker().toUpperCase(),
                            SecCompanyTicker::getCikStr,
                            (existing, replacement) -> existing
                    ));

            cache.put(CACHE_KEY, mapping);
            log.info("SEC CIK 캐시 갱신 완료: {}건", mapping.size());
        } catch (SecApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("SEC CIK 캐시 갱신 실패: {}", e.getMessage());
        }
    }

    /**
     * 티커 → CIK 조회
     */
    public Optional<Long> getCik(String ticker) {
        Map<String, Long> mapping = cache.getIfPresent(CACHE_KEY);
        if (mapping == null) {
            throw new SecApiException(SecErrorType.API_ERROR, "SEC CIK 캐시가 초기화되지 않았습니다");
        }
        return Optional.ofNullable(mapping.get(ticker.toUpperCase()));
    }

    /**
     * CIK → 10자리 패딩 문자열 (예: 320193 → "CIK0000320193")
     */
    public static String formatCik(long cik) {
        return String.format("CIK%010d", cik);
    }
}