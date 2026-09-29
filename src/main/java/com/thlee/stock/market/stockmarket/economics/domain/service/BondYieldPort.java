package com.thlee.stock.market.stockmarket.economics.domain.service;

import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldFetchException;
import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldParseException;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldSnapshot;

import java.time.LocalDate;

/**
 * 채권 기준수익률 조회 포트.
 * 외부 요청과 응답 해석은 infrastructure 어댑터가 담당하고, 서비스는 이 계약에만 의존한다.
 * 출처를 바꿀 때는 이 포트를 구현한 어댑터만 교체한다.
 */
public interface BondYieldPort {

    /**
     * 기준일 하루치 기준수익률을 조회한다. 데이터가 없는 날짜는 빈 스냅샷을 돌려준다.
     *
     * @throws BondYieldFetchException 출처와 통신하지 못함
     * @throws BondYieldParseException 출처 응답을 해석하지 못함
     */
    BondYieldSnapshot fetch(LocalDate baseDate);

    /** 화면에 표시할 출처 이름 */
    String sourceName();
}
