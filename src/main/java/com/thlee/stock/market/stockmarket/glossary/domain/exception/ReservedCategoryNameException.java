package com.thlee.stock.market.stockmarket.glossary.domain.exception;

/**
 * 카테고리명에 예약어("미분류")가 입력됐을 때 발생.
 * HTTP 400 으로 매핑.
 */
public class ReservedCategoryNameException extends RuntimeException {

    public ReservedCategoryNameException(String name) {
        super("예약된 카테고리명입니다: " + name);
    }
}
