package com.thlee.stock.market.stockmarket.glossary.application.dto;

/**
 * 카테고리 이름 수정 명령. controller request DTO → service 변환 시 사용.
 */
public record UpdateGlossaryCategoryCommand(String name) {
}
