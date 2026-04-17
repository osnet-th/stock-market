package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisMasterStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class KisStockMasterCache {

    private static final String CACHE_KEY = "all_stocks";

    private final KisMasterFileClient masterFileClient;
    private final Cache<String, List<KisMasterStock>> cache;

    public KisStockMasterCache(KisMasterFileClient masterFileClient) {
        this.masterFileClient = masterFileClient;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(1)
            .build();
    }

    /**
     * 서버 시작 시 즉시 실행 + 23시간 주기 갱신
     */
    @Scheduled(fixedRate = 23, timeUnit = TimeUnit.HOURS)
    public void refreshCache() {
        loadAndCacheStocks();
    }

    /**
     * 종목명에 keyword가 포함된 종목 검색 (한글명 + 영문명 부분일치)
     */
    public List<KisMasterStock> searchByName(String keyword) {
        List<KisMasterStock> allStocks = getAllStocks();
        String lowerKeyword = keyword.toLowerCase();

        return allStocks.stream()
            .filter(stock -> matchesKeyword(stock, lowerKeyword))
            .toList();
    }

    /**
     * 종목코드 정확일치 조회
     */
    public Optional<KisMasterStock> findByCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            return Optional.empty();
        }
        return getAllStocks().stream()
            .filter(stock -> stockCode.equals(stock.getStockCode()))
            .findFirst();
    }

    private boolean matchesKeyword(KisMasterStock stock, String lowerKeyword) {
        if (stock.getKoreanName() != null && stock.getKoreanName().toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        if (stock.getEnglishName() != null && stock.getEnglishName().toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        return false;
    }

    private List<KisMasterStock> getAllStocks() {
        List<KisMasterStock> stocks = cache.getIfPresent(CACHE_KEY);
        if (stocks != null) {
            return stocks;
        }
        return loadAndCacheStocks();
    }

    private List<KisMasterStock> loadAndCacheStocks() {
        List<KisMasterStock> stocks = masterFileClient.downloadAllStocks();
        if (stocks.isEmpty()) {
            log.warn("KIS 마스터파일에서 종목 데이터를 로드하지 못했습니다");
            return Collections.emptyList();
        }
        List<KisMasterStock> immutableStocks = Collections.unmodifiableList(stocks);
        cache.put(CACHE_KEY, immutableStocks);
        return immutableStocks;
    }
}