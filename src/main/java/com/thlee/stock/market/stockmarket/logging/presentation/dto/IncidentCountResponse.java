package com.thlee.stock.market.stockmarket.logging.presentation.dto;

import com.thlee.stock.market.stockmarket.logging.application.dto.IncidentCountResult;

import java.time.Instant;

/**
 * 메인 대시보드 운영자 카드용 응답.
 *
 * <p>{@code available=false} 인 경우 프론트는 "데이터 불러올 수 없음" placeholder 로 분기한다
 * (ES degrade 와 진짜 0건의 의미 분리).
 */
public record IncidentCountResponse(long count, Instant asOf, boolean available) {

    public static IncidentCountResponse from(IncidentCountResult r) {
        return new IncidentCountResponse(r.count(), r.asOf(), r.available());
    }
}