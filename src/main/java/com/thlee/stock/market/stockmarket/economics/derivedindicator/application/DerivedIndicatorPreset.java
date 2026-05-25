package com.thlee.stock.market.stockmarket.economics.derivedindicator.application;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;

/**
 * 시스템 제공 파생지표 프리셋 정의(코드 상수). 사용자가 1클릭 복제 시 본인 소유 레코드로 INSERT된다.
 * 프리셋은 R3 예외(교차 카테고리 허용). 등급/해석 텍스트는 비포함.
 *
 * @param key     프리셋 식별 화이트리스트 키
 * @param name    표시 이름
 * @param unit    단위 표기
 * @param formula 구조형 수식
 */
public record DerivedIndicatorPreset(String key, String name, String unit, DerivedFormula formula) {
}
