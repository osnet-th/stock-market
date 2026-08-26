package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.dto;

import java.math.BigDecimal;

/**
 * R-ONE {@code SttsApiTblData.do} 응답 row.
 * <p>
 * 실제 시계열 데이터. {@link #wrttimeIdtfrId}가 자료작성 시점(예: "202604"),
 * {@link #dtaVal}가 통계 자료값. region 식별은 {@link #itmId} → {@code SttsApiTblItm} 응답의
 * {@code ITM_NM}/{@code ITM_FULLNM} 매핑으로 추출.
 * <p>
 * 명세: {@code .claude/analyzes/realestate/reb-r-one-openapi-spec.md} (3번 엔드포인트).
 */
public record SttsApiTblDataRow(
        String statblId,
        String dtacycleCd,
        String wrttimeIdtfrId,
        String grpId,
        String grpNm,
        String clsId,
        String clsNm,
        String itmId,
        String itmNm,
        BigDecimal dtaVal,
        String uiNm,
        String wrttimeDesc
) {
}