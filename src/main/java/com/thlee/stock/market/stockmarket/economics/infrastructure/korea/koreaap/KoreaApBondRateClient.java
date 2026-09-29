package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.koreaap;

import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldFetchException;
import com.thlee.stock.market.stockmarket.economics.infrastructure.korea.koreaap.config.KoreaApProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 한국자산평가 "채권금리 기준수익률" 화면이 쓰는 날짜별 조회를 호출한다.
 * 휴일·미발표 날짜는 빈 배열([])이 온다.
 */
@Component
public class KoreaApBondRateClient {

    private static final String BOND_RATES_PATH = "/vl/valuation01_01/bondRates?ymd={ymd}&type=Y&searchType=0&lang=kor";

    private final RestClient restClient;
    private final KoreaApProperties properties;

    public KoreaApBondRateClient(@Qualifier("koreaApRestClient") RestClient restClient, KoreaApProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /** 기준일 하루치 응답 본문(JSON 배열 문자열). 연결·타임아웃·HTTP 오류는 통신 실패로 바꾼다. */
    public String fetch(LocalDate baseDate) {
        try {
            return restClient.get()
                    .uri(properties.getBaseUrl() + BOND_RATES_PATH, baseDate.format(DateTimeFormatter.BASIC_ISO_DATE))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new BondYieldFetchException("한국자산평가 금리 조회에 실패했습니다. 잠시 후 다시 시도해 주세요.", e);
        }
    }
}
