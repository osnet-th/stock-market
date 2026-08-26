package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.dto;

/**
 * R-ONE {@code SttsApiTblItm.do} 응답 row.
 * <p>
 * 통계표 안의 항목(지역/주택유형 등 분류) 메타. region 매핑은 ITM_NM/ITM_FULLNM 기반.
 * 명세: {@code .claude/analyzes/realestate/reb-r-one-openapi-spec.md} (2번 엔드포인트).
 */
public record SttsApiTblItmRow(
        String statblId,
        String itmTag,
        String itmId,
        String parItmId,
        String itmNm,
        String itmFullnm,
        String uiNm,
        Integer vOrder
) {
}