package com.thlee.stock.market.stockmarket.economics.infrastructure.korea.koreaap;

import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.service.BondYieldPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 한국자산평가를 출처로 하는 금리 조회 포트 구현.
 * 출처를 바꿀 때는 BondYieldPort를 구현한 다른 어댑터로 이 빈을 교체한다.
 */
@Component
@RequiredArgsConstructor
public class KoreaApBondYieldAdapter implements BondYieldPort {

    private static final String SOURCE_NAME = "한국자산평가";

    private final KoreaApBondRateClient client;
    private final KoreaApBondRateParser parser;

    @Override
    public BondYieldSnapshot fetch(LocalDate baseDate) {
        return parser.parse(baseDate, client.fetch(baseDate));
    }

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }
}
