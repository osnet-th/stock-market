package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;

/**
 * 시장영향별 사건 건수 그룹 결과 value (화면 통계용 projection).
 */
public record NewsEventImpactCount(EventImpact impact, long count) {
}
