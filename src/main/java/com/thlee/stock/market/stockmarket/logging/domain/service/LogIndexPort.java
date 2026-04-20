package com.thlee.stock.market.stockmarket.logging.domain.service;

import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;

/**
 * 애플리케이션 로그 인덱싱 포트.
 *
 * ES 장애 시 예외는 구현체 내부에서 삼켜야 하며, 로그 적재 실패가 비즈니스 흐름을 막아서는 안 된다
 * (best-effort at-most-once 정책).
 */
public interface LogIndexPort {

    /**
     * 단일 로그 적재. 구현체는 내부 버퍼링 여부와 무관하게 동일 계약을 따른다.
     */
    void save(ApplicationLog log);
}