package com.thlee.stock.market.stockmarket.portfolio.infrastructure.goldprice;

import com.thlee.stock.market.stockmarket.portfolio.domain.service.GoldPriceProvider;
import com.thlee.stock.market.stockmarket.portfolio.infrastructure.goldprice.dto.GoldPriceApiResponse;
import com.thlee.stock.market.stockmarket.portfolio.infrastructure.goldprice.dto.GoldPriceApiResponse.GoldPriceItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;

/**
 * KRX 금시장 시세 어댑터 (data.go.kr 금융위원회_일반상품시세정보)
 * - 최근 영업일의 "금 99.99" 종목 종가(1g당 KRW)를 반환
 * - 성공 결과는 TTL 캐시, 실패는 짧은 backoff 후 재시도 (호출 폭주 방지)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxGoldPriceAdapter implements GoldPriceProvider {

    private static final DateTimeFormatter BAS_DT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String GOLD_ITEM_NAME_PREFIX = "금 99.99";

    private final RestClient restClient;
    private final GoldPriceProperties properties;

    private volatile CachedPrice cache;
    private volatile Instant lastFailureAt;

    @Override
    public Optional<BigDecimal> getPricePerGram() {
        CachedPrice cached = this.cache;
        if (cached != null && cached.isFresh(properties.getCacheTtlMinutes())) {
            return Optional.of(cached.price());
        }
        Instant failedAt = this.lastFailureAt;
        if (failedAt != null && withinMinutes(failedAt, properties.getFailureBackoffMinutes())) {
            return staleOrEmpty(cached);
        }

        synchronized (this) {
            cached = this.cache;
            if (cached != null && cached.isFresh(properties.getCacheTtlMinutes())) {
                return Optional.of(cached.price());
            }
            Optional<BigDecimal> fetched = fetchLatestPrice();
            if (fetched.isPresent()) {
                this.cache = new CachedPrice(fetched.get(), Instant.now());
                this.lastFailureAt = null;
                return fetched;
            }
            this.lastFailureAt = Instant.now();
            return staleOrEmpty(this.cache);
        }
    }

    /**
     * TTL이 지난 캐시라도 새 조회 실패 시에는 재사용 (완전 실패보다 전일 시세가 낫다)
     */
    private Optional<BigDecimal> staleOrEmpty(CachedPrice cached) {
        return cached != null ? Optional.of(cached.price()) : Optional.empty();
    }

    private Optional<BigDecimal> fetchLatestPrice() {
        try {
            LocalDate today = LocalDate.now();
            String uriString = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                    .queryParam("resultType", "json")
                    .queryParam("numOfRows", 50)
                    .queryParam("pageNo", 1)
                    .queryParam("beginBasDt", today.minusDays(properties.getLookbackDays()).format(BAS_DT_FORMATTER))
                    .queryParam("endBasDt", today.plusDays(1).format(BAS_DT_FORMATTER))
                    .encode()
                    .build()
                    .toUriString()
                    + "&serviceKey=" + properties.getServiceKey();

            GoldPriceApiResponse response = restClient.get()
                    .uri(URI.create(uriString))
                    .retrieve()
                    .body(GoldPriceApiResponse.class);

            if (response == null || !response.isSuccess()) {
                log.warn("금시세 API 응답 실패: {}",
                        response != null && response.getResponse() != null && response.getResponse().getHeader() != null
                                ? response.getResponse().getHeader().getResultMsg() : "empty response");
                return Optional.empty();
            }

            // 날짜 필터가 무시된 응답(과거 데이터 페이지) 방어: lookback 범위 밖 시세는 실패로 처리
            String minBasDt = today.minusDays(properties.getLookbackDays()).format(BAS_DT_FORMATTER);
            return response.getItemList().stream()
                    .filter(item -> item.getItmsNm() != null && item.getItmsNm().startsWith(GOLD_ITEM_NAME_PREFIX))
                    .max(Comparator.comparing(GoldPriceItem::getBasDt))
                    .filter(item -> item.getBasDt() != null && item.getBasDt().compareTo(minBasDt) >= 0)
                    .map(item -> new BigDecimal(item.getClpr()));
        } catch (Exception e) {
            log.warn("금시세 API 호출 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean withinMinutes(Instant instant, int minutes) {
        return Instant.now().isBefore(instant.plus(Duration.ofMinutes(minutes)));
    }

    private record CachedPrice(BigDecimal price, Instant fetchedAt) {
        boolean isFresh(int ttlMinutes) {
            return Instant.now().isBefore(fetchedAt.plus(Duration.ofMinutes(ttlMinutes)));
        }
    }
}
