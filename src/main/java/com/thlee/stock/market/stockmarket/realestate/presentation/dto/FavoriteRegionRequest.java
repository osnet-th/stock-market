package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

/**
 * 관심지역 등록 요청. emdCode가 null/빈 문자열이면 시군구 단위 즐겨찾기.
 */
public record FavoriteRegionRequest(String regionCode, String emdCode) {
}