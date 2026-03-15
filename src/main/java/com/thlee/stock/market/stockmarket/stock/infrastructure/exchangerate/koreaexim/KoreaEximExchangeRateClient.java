package com.thlee.stock.market.stockmarket.stock.infrastructure.exchangerate.koreaexim;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 한국수출입은행 환율 조회 클라이언트.
 * 당일 매매기준율을 조회한다.
 */
@Component
@RequiredArgsConstructor
public class KoreaEximExchangeRateClient {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;
    private final KoreaEximProperties properties;

    private static final int MAX_RETRY_DAYS = 5;

    /**
     * 환율을 조회한다.
     * 비영업일이나 영업일 11시 이전에는 데이터가 없으므로,
     * 당일부터 최대 5일 전까지 순차적으로 조회하여 가장 최근 영업일 환율을 반환한다.
     *
     * @return 통화별 환율 목록
     */
    public List<ExchangeRateItem> getExchangeRates() {
        LocalDate date = LocalDate.now();

        for (int i = 0; i < MAX_RETRY_DAYS; i++) {
            List<ExchangeRateItem> result = fetchByDate(date.minusDays(i));
            if (!result.isEmpty()) {
                return result;
            }
        }

        return Collections.emptyList();
    }

    private List<ExchangeRateItem> fetchByDate(LocalDate date) {
        try {
            List<ExchangeRateItem> result = restClient.get()
                .uri(properties.getUrl() + "?authkey={authkey}&searchdate={searchdate}&data=AP01",
                    properties.getAuthkey(), date.format(DATE_FORMAT))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

            return (result != null && !result.isEmpty()) ? result : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ExchangeRateItem {

        @JsonProperty("cur_unit")
        private String currencyUnit;     // 통화코드 (USD, JPY(100) 등)

        @JsonProperty("deal_bas_r")
        private String baseRate;         // 매매기준율 (쉼표 포함, 예: "1,380.5")

        @JsonProperty("cur_nm")
        private String currencyName;     // 통화명 (예: "미국 달러")
    }
}