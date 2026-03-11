package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartCorpCode;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.exception.DartApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DartCorpCodeCache {

    private static final String CACHE_KEY = "corpCodeMap";

    private final DartApiClient dartApiClient;

    private final Cache<String, Map<String, String>> cache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    @Scheduled(fixedRate = 23, timeUnit = TimeUnit.HOURS)
    public void refreshCache() {
        try {
            List<DartCorpCode> corpCodes = dartApiClient.downloadCorpCodes();

            Map<String, String> mapping = corpCodes.stream()
                    .filter(c -> c.getStockCode() != null && !c.getStockCode().isBlank())
                    .collect(Collectors.toMap(
                            DartCorpCode::getStockCode,
                            DartCorpCode::getCorpCode,
                            (existing, replacement) -> existing
                    ));

            cache.put(CACHE_KEY, mapping);
            log.info("DART 고유번호 캐시 갱신 완료: {}건", mapping.size());
        } catch (Exception e) {
            log.error("DART 고유번호 캐시 갱신 실패: {}", e.getMessage());
        }
    }

    /**
     * 종목코드 → 고유번호 변환
     */
    public String getCorpCode(String stockCode) {
        Map<String, String> mapping = cache.getIfPresent(CACHE_KEY);
        if (mapping == null) {
            throw new DartApiException("DART 고유번호 캐시가 초기화되지 않았습니다");
        }

        String corpCode = mapping.get(stockCode);
        if (corpCode == null) {
            throw new DartApiException("종목코드에 해당하는 DART 고유번호를 찾을 수 없습니다: " + stockCode);
        }

        return corpCode;
    }

    /**
     * 다중 종목코드 → 쉼표 구분 고유번호 변환 (최대 100건)
     */
    public String getCorpCodes(List<String> stockCodes) {
        if (stockCodes.size() > 100) {
            throw new DartApiException("다중회사 조회는 최대 100건까지 가능합니다");
        }

        return stockCodes.stream()
                .map(this::getCorpCode)
                .collect(Collectors.joining(","));
    }
}