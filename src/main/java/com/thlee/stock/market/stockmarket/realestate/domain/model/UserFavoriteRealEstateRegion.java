package com.thlee.stock.market.stockmarket.realestate.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자별 부동산 관심지역. 단위는 시군구 또는 읍면동(선택).
 * <p>
 * 주택유형/면적 등 필터 상태는 본 즐겨찾기에 저장하지 않는다 — 칩은 "지역"이지 "지역+필터 상태"가 아님.
 * favorite 도메인은 "지표 즐겨찾기" 의미를 유지하므로 enum 확장 대신 본 entity가 신설되었다.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFavoriteRealEstateRegion {

    private Long id;
    private Long userId;
    private String sidoCode;
    private String regionCode;
    private String emdCode;
    private int displayOrder;
    private LocalDateTime createdAt;
}
