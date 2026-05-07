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
         * data.go.kr 응답은 단일 결과일 때 객체, 다수일 때 배열로 오므로
         * Object로 받아 어댑터에서 정규화한다.
         */
        private Items items;
        private String totalCount;
        private String numOfRows;
        private String pageNo;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        private Object item;

        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> asList() {
            if (item == null) {
                return Collections.emptyList();
            }
            if (item instanceof List<?> list) {
                return (List<Map<String, Object>>) list;
            }
            if (item instanceof Map<?, ?> map) {
                return List.of((Map<String, Object>) map);
            }
            return Collections.emptyList();
        }
    }

    public boolean isSuccess() {
        return response != null
                && response.header != null
                && "00".equals(response.header.resultCode);
    }

    public List<Map<String, Object>> rows() {
        if (response == null || response.body == null || response.body.items == null) {
            return Collections.emptyList();
        }
        return response.body.items.asList();
    }
}
