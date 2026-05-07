package com.thlee.stock.market.stockmarket.realestate.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부동산 시장 데이터 지원 지역.
 * <p>
 * 시군구 단위(emdCode 빈 문자열) 또는 읍면동 단위(emdCode 8자리).
 * 인프라 영역에서만 빈 문자열 sentinel 사용, 도메인 노출 시 Optional로 wrapping.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RealEstateRegion {

    private String regionCode;
    private String sidoCode;
    private String sidoName;
    private String sigunguName;
    private String emdCode;
    private String emdName;
    private int displayOrder;

    public boolean hasEmd() {
        return emdCode != null && !emdCode.isEmpty();
    }
}
