package com.thlee.stock.market.stockmarket.glossary.application.dto;

/**
 * 카테고리 생성 명령. controller request DTO → service 변환 시 사용.
 */
public record CreateGlossaryCategoryCommand(String name) {
}
