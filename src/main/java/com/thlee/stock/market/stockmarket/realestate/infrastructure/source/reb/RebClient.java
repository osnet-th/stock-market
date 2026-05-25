package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb;

import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateRestClientConfig;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.BaseUri;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.SecretMasker;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.dto.SttsApiTblDataRow;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.dto.SttsApiTblItmRow;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.exception.RebApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 한국부동산원 R-ONE Open API 클라이언트 — 3-step 모델.
 * <p>
 * 엔드포인트:
 * <ul>
 *   <li>{@code SttsApiTbl} — 통계표 목록 (현재 미사용. STATBL_ID/DTACYCLE_CD는 yml 고정)</li>
 *   <li>{@code SttsApiTblItm} — 통계표 항목 (region 매핑용)</li>
 *   <li>{@code SttsApiTblData} — 시계열 데이터</li>
 * </ul>
 * <p>
 * 적용 함정 (학습 docs/solutions/architecture-patterns/realestate-public-open-api-pitfalls-2026-05-07.md):
 * <ul>
 *   <li>#12 Content-Type 무시 — String body + ObjectMapper로 파싱</li>
 *   <li>#15 contextPath — BaseUri.contextPath() 합성</li>
 *   <li>#11 resultCode "00"/"000" — INFO 000/INFO 200 모두 success</li>
 * </ul>
 */
@Component
@Slf4j
public class RebClient {

    private static final String PATH_ITM = "/SttsApiTblItm";
    private static final String PATH_DATA = "/SttsApiTblData";
    private static final int DEFAULT_PAGE_SIZE = 100;
    /** R-ONE ERROR 336 — 한 요청에 1,000건 초과 차단. */
    private static final int MAX_PAGE_SIZE = 1000;
    /** 한 호출당 최대 페이지 수 — 무한 루프 방지. */
    private static final int MAX_PAGES = 20;

    private static final String INFO_OK = "000";
    private static final String INFO_OK_LEGACY = "00";
    private static final String INFO_EMPTY = "200";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;
    private final ObjectMapper objectMapper;

    public RebClient(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                     RealEstateMarketProperties properties,
                     ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 통계표 항목(ITM) 메타 조회. 페이징 누적.
     *
     * @throws RebApiException 호출 실패 또는 API 에러 코드(ERROR 290/300/336/337/500 등)
     */
    public List<SttsApiTblItmRow> fetchItm(String statblId) {
        Map<String, String> base = Map.of("STATBL_ID", statblId);
        List<Map<String, Object>> raw = fetchAllPages(PATH_ITM, base);
        List<SttsApiTblItmRow> result = new ArrayList<>(raw.size());
        for (Map<String, Object> row : raw) {
            result.add(new SttsApiTblItmRow(
                    str(row, "STATBL_ID"),
                    str(row, "ITM_TAG"),
                    str(row, "ITM_ID"),
                    str(row, "PAR_ITM_ID"),
                    str(row, "ITM_NM"),
                    str(row, "ITM_FULLNM"),
                    str(row, "UI_NM"),
                    intOrNull(row, "V_ORDER")
            ));
        }
        return result;
    }

    /**
     * 시계열 데이터 조회 (Q4=D1 전략: ITM_ID 미지정 — STATBL 전체 응답을 메모리에서 region 필터).
     * 페이징 누적.
     *
     * @param startWrttime "YYYYMM" 또는 "YYYY" (인자 형식은 통계표 주기에 따름). null이면 제외.
     * @param endWrttime   동일.
     */
    public List<SttsApiTblDataRow> fetchData(String statblId,
                                             String dtacycleCd,
                                             String startWrttime,
                                             String endWrttime) {
        Map<String, String> base = new HashMap<>();
        base.put("STATBL_ID", statblId);
        base.put("DTACYCLE_CD", dtacycleCd);
        if (startWrttime != null) base.put("START_WRTTIME", startWrttime);
        if (endWrttime != null) base.put("END_WRTTIME", endWrttime);

        List<Map<String, Object>> raw = fetchAllPages(PATH_DATA, base);
        List<SttsApiTblDataRow> result = new ArrayList<>(raw.size());
        for (Map<String, Object> row : raw) {
            result.add(new SttsApiTblDataRow(
                    str(row, "STATBL_ID"),
                    str(row, "DTACYCLE_CD"),
                    str(row, "WRTTIME_IDTFR_ID"),
                    str(row, "GRP_ID"),
                    str(row, "GRP_NM"),
                    str(row, "CLS_ID"),
                    str(row, "CLS_NM"),
                    str(row, "ITM_ID"),
                    str(row, "ITM_NM"),
                    decimalOrNull(row, "DTA_VAL"),
                    str(row, "UI_NM"),
                    str(row, "WRTTIME_DESC")
            ));
        }
        return result;
    }

    /**
     * 페이지를 끝까지 누적해서 row 리스트 반환. 빈 페이지(INFO 200) 또는 pSize 미만 응답에서 종료.
     * MAX_PAGES에 도달했는데 마지막 페이지가 가득 차 있으면 truncation 가능성을 경고 로깅한다.
     */
    private List<Map<String, Object>> fetchAllPages(String path, Map<String, String> base) {
        List<Map<String, Object>> accumulator = new ArrayList<>();
        boolean truncated = false;
        for (int pIndex = 1; pIndex <= MAX_PAGES; pIndex++) {
            Map<String, String> params = new HashMap<>(base);
            params.put("pIndex", String.valueOf(pIndex));
            params.put("pSize", String.valueOf(DEFAULT_PAGE_SIZE));
            PageResponse page = fetchPage(path, params);
            if (page.rows.isEmpty()) {
                break;
            }
            accumulator.addAll(page.rows);
            if (page.rows.size() < DEFAULT_PAGE_SIZE) {
                break;
            }
            if (pIndex == MAX_PAGES) {
                truncated = true;
            }
        }
        if (truncated) {
            log.warn("[realestate.reb] fetchAllPages truncated at MAX_PAGES={} for path={} params={} — possible data loss, raise MAX_PAGES if STATBL row count exceeds {}",
                    MAX_PAGES, path, base, MAX_PAGES * DEFAULT_PAGE_SIZE);
        }
        return accumulator;
    }

    private PageResponse fetchPage(String path, Map<String, String> queryParams) {
        RealEstateMarketProperties.SourceProps reb = properties.getReb();
        if (reb.getApiKey() == null || reb.getApiKey().isBlank()) {
            throw new IllegalStateException("REB api-key not configured");
        }
        BaseUri base = BaseUri.parse(reb.getBaseUrl());
        String body;
        try {
            body = restClient.get()
                    .uri(builder -> {
                        var uri = builder.scheme(base.scheme())
                                .host(base.host())
                                .port(base.port())
                                .path(base.contextPath() + path)
                                .queryParam("Key", reb.getApiKey())
                                .queryParam("Type", "json");
                        queryParams.forEach(uri::queryParam);
                        return uri.build();
                    })
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new RebApiException("REB API call failed: " + path, SecretMasker.sanitize(e));
        }
        return parseResponse(path, body);
    }

    @SuppressWarnings("unchecked")
    private PageResponse parseResponse(String path, String body) {
        if (body == null || body.isBlank()) {
            return PageResponse.empty();
        }
        Map<String, Object> root;
        try {
            root = objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RebApiException("REB response parse failed: " + path, e);
        }
        // 응답 envelope은 통계표/엔드포인트에 따라 key가 다를 수 있어 result 키를 우선 탐색
        ResultCode result = extractResultCode(root);
        if (result.isEmpty()) {
            return PageResponse.empty();
        }
        if (!result.isSuccess()) {
            throw new RebApiException("REB API returned error code=" + result.code + " msg=" + result.message);
        }
        List<Map<String, Object>> rows = extractRows(root);
        return new PageResponse(rows);
    }

    @SuppressWarnings("unchecked")
    private ResultCode extractResultCode(Map<String, Object> root) {
        // R-ONE 응답에서 RESULT 객체 추출 (다양한 위치 가능)
        Object resultObj = root.get("RESULT");
        if (resultObj == null) {
            resultObj = root.get("result");
        }
        if (resultObj instanceof Map<?, ?> map) {
            Object codeObj = map.get("CODE");
            if (codeObj == null) codeObj = map.get("code");
            Object msgObj = map.get("MESSAGE");
            if (msgObj == null) msgObj = map.get("message");
            String code = codeObj == null ? "" : codeObj.toString();
            String message = msgObj == null ? "" : msgObj.toString();
            return new ResultCode(code, message);
        }
        // RESULT envelope이 없으면 자료 row 존재 여부로 판단
        return new ResultCode("", "");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(Map<String, Object> root) {
        // 가장 흔한 형태: root.values()를 순회하며 List<Map>을 발견
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if ("RESULT".equalsIgnoreCase(entry.getKey())) continue;
            Object value = entry.getValue();
            if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
                return (List<Map<String, Object>>) value;
            }
            if (value instanceof Map<?, ?> wrapper) {
                for (Object inner : wrapper.values()) {
                    if (inner instanceof List<?> innerList && !innerList.isEmpty()
                            && innerList.get(0) instanceof Map<?, ?>) {
                        return (List<Map<String, Object>>) inner;
                    }
                }
            }
        }
        return List.of();
    }

    private String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v == null ? null : v.toString().trim();
    }

    private Integer intOrNull(Map<String, Object> row, String key) {
        String s = str(row, key);
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal decimalOrNull(Map<String, Object> row, String key) {
        String s = str(row, key);
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record PageResponse(List<Map<String, Object>> rows) {
        static PageResponse empty() {
            return new PageResponse(List.of());
        }
    }

    private record ResultCode(String code, String message) {
        boolean isEmpty() {
            return INFO_EMPTY.equals(code);
        }

        boolean isSuccess() {
            return code == null || code.isBlank()
                    || INFO_OK.equals(code) || INFO_OK_LEGACY.equals(code);
        }
    }
}
