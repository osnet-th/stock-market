package com.thlee.stock.market.stockmarket.stockevaluation.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * KIS 표 형태 응답 DTO.
 * columns(한글 라벨) + rows(레코드별 값, columns 순서)로 구성된 범용 테이블.
 */
@Getter
@Builder
public class KisTableResponse {

    private final List<String> columns;
    private final List<List<String>> rows;

    /**
     * 라벨 맵(kis_key → 한글) 기준으로 KIS 레코드 목록을 표로 변환한다.
     * columns는 라벨 맵의 값(순서 유지), rows는 각 레코드의 kis_key 값.
     */
    public static KisTableResponse from(List<Map<String, String>> records, LinkedHashMap<String, String> labelMap) {
        List<String> columns = new ArrayList<>(labelMap.values());
        List<List<String>> rows = new ArrayList<>();
        if (records != null) {
            for (Map<String, String> rec : records) {
                List<String> row = new ArrayList<>();
                for (String key : labelMap.keySet()) {
                    row.add(rec.getOrDefault(key, ""));
                }
                rows.add(row);
            }
        }
        return KisTableResponse.builder().columns(columns).rows(rows).build();
    }
}
