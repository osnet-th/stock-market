package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * data.go.kr 표준 응답 형태. 매매/전월세 등 모든 MOLIT 실거래 API가 같은 envelope을 사용한다.
 *
 * <pre>
 * {
 *   "response": {
 *     "header": { "resultCode": "00", "resultMsg": "OK" },
 *     "body": {
 *       "items": { "item": [...] | {...} },
 *       "totalCount": "...", ...
 *     }
 *   }
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MolitApiResponse {

    @JsonProperty("response")
    private Response response;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        /**
         * data.go.kr 응답 분기:
         * <ul>
         *   <li>다수 결과: {@code {"item": [...]}}</li>
         *   <li>단일 결과: {@code {"item": {...}}}</li>
         *   <li>빈 결과: {@code ""} (빈 문자열) — totalCount=0인 경우 일부 endpoint가 객체 대신 문자열로 응답</li>
         * </ul>
         * 세 케이스를 모두 흡수하기 위해 Object로 받고 {@link #rows()}에서 분기한다.
         */
        private Object items;
        private String totalCount;
        private String numOfRows;
        private String pageNo;
    }

    /** "00" 또는 "000" 모두 정상으로 처리 — endpoint별 코드 형식이 다름. */
    public boolean isSuccess() {
        if (response == null || response.header == null) {
            return false;
        }
        String code = response.header.resultCode;
        return "00".equals(code) || "000".equals(code);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> rows() {
        if (response == null || response.body == null) {
            return Collections.emptyList();
        }
        Object items = response.body.items;
        // 빈 응답: items가 "" 빈 문자열 또는 null (totalCount=0)
        if (items == null || items instanceof String) {
            return Collections.emptyList();
        }
        if (items instanceof Map<?, ?> itemsMap) {
            Object item = itemsMap.get("item");
            if (item == null) {
                return Collections.emptyList();
            }
            if (item instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
            if (item instanceof Map<?, ?> single) {
                return List.of((Map<String, Object>) single);
            }
        }
        return Collections.emptyList();
    }
}
