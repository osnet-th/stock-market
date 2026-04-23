package com.thlee.stock.market.stockmarket.favorite.application.exception;

/**
 * 본인이 관심 등록한 지표가 아닌 것을 재조회 시도한 경우.
 */
public class FavoriteRefreshForbiddenException extends RuntimeException {
    public FavoriteRefreshForbiddenException(String message) {
        super(message);
    }
}