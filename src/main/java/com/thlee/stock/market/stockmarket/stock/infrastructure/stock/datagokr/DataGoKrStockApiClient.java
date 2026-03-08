package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.config.DataGoKrProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.dto.DataGoKrStockResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception.DataGoKrApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class DataGoKrStockApiClient {

    private static final DateTimeFormatter BAS_DT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestClient restClient;
    private final DataGoKrProperties properties;

    public DataGoKrStockResponse searchByName(String stockName) {
        try {
            String yesterday = LocalDate.now().minusDays(1).format(BAS_DT_FORMATTER);

            String uriString = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .queryParam("resultType", "json")
                .queryParam("numOfRows", properties.getNumOfRows())
                .queryParam("pageNo", 1)
                .queryParam("likeItmsNm", stockName)
                .encode()
                .build()
                .toUriString()
                + "&serviceKey=" + properties.getServiceKey();

            return restClient.get()
                .uri(URI.create(uriString))
                .retrieve()
                .body(DataGoKrStockResponse.class);
        } catch (RestClientException e) {
            throw new DataGoKrApiException("공공데이터포털 상장종목 API 호출 실패: " + e.getMessage(), e);
        }
    }
}