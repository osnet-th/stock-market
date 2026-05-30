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

    // R-ONE endpoint는 ".do" suffix 필수 — 미부착 시 HTML 오류 페이지 반환 (학습 #15 contextPath와 별개)
    private static final String PATH_ITM = "/SttsApiTblItm.do";
    private static final String PATH_DATA = "/SttsApiTblData.do";
    private static final int DEFAULT_PAGE_SIZE = 100;
    /** R-ONE ERROR 336 — 한 요청에 1,000건 초과 차단. */
    private static final int MAX_PAGE_SIZE = 1000;
    /** 한 호출당 최대 페이지 수 — 무한 루프 방지. */
    private static final int MAX_PAGES = 20;

    // R-ONE resultCode 실제 형식은 "INFO-000" (정상) / "INFO-200" (빈 결과) / "ERROR-NNN" (오류).
    // 명세서엔 "000" 형식이라 적혀 있으나 실제 응답은 prefix 포함.
    private static final String INFO_OK = "INFO-000";
    private static final String INFO_OK_LEGACY_3 = "000";
    private static final String INFO_OK_LEGACY_2 = "00";
    private static final String INFO_EMPTY = "INFO-200";
    private static final String INFO_EMPTY_LEGACY = "200";

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

    /**
     * 응답 어디든 RESULT/result 키의 Map 발견 시 그 안의 CODE/MESSAGE 추출.
     * R-ONE 형식 예: {SttsApiTblItm: [{head: [{list_total_count:N}, {RESULT: {CODE:"INFO-000", ...}}]}, ...]}
     */
    private ResultCode extractResultCode(Map<String, Object> root) {
        Map<String, Object> resultMap = findResultMap(root);
        if (resultMap == null) {
            return new ResultCode("", "");
        }
        Object codeObj = resultMap.get("CODE");
        if (codeObj == null) codeObj = resultMap.get("code");
        Object msgObj = resultMap.get("MESSAGE");
        if (msgObj == null) msgObj = resultMap.get("message");
        String code = codeObj == null ? "" : codeObj.toString();
        String message = msgObj == null ? "" : msgObj.toString();
        return new ResultCode(code, message);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> findResultMap(Object node) {
        if (node instanceof Map<?, ?> map) {
            Object direct = map.get("RESULT");
            if (direct == null) direct = map.get("result");
            if (direct instanceof Map<?, ?> direct2) {
                return (Map<String, Object>) direct2;
            }
            for (Object v : map.values()) {
                Map<String, Object> found = findResultMap(v);
                if (found != null) return found;
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> found = findResultMap(item);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * 응답에서 실제 데이터 row 리스트 추출. R-ONE은 wrapper 형식 사용:
     * {SttsApiTblItm: [{head: [...]}, {row: [{STATBL_ID, ITM_ID, ...}, ...]}]}
     * → "row" 키를 가진 nested wrapper의 값을 우선 탐색.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRows(Map<String, Object> root) {
        // 우선순위 1: 모든 위치에서 "row" 또는 "ROW" 키 가진 List<Map> 탐색 (R-ONE 표준)
        List<Map<String, Object>> rowsByKey = findRowsByKey(root);
        if (rowsByKey != null) {
            return rowsByKey;
        }
        // 우선순위 2: legacy fallback — root values의 첫 List<Map> (다른 출처용)
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if ("RESULT".equalsIgnoreCase(entry.getKey())) continue;
            Object value = entry.getValue();
            if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first
                    && !first.containsKey("head") && !first.containsKey("row")
                    && !first.containsKey("RESULT")) {
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

    /**
     * 재귀 탐색으로 "row" / "ROW" 키 발견 시 그 값(List<Map>)을 반환.
     * R-ONE wrapper 형식의 핵심 추출기.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> findRowsByKey(Object node) {
        if (node instanceof Map<?, ?> map) {
            Object direct = map.get("row");
            if (direct == null) direct = map.get("ROW");
            if (direct instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?>) {
                return (List<Map<String, Object>>) direct;
            }
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if ("RESULT".equalsIgnoreCase(String.valueOf(entry.getKey()))) continue;
                if ("head".equalsIgnoreCase(String.valueOf(entry.getKey()))) continue;
                List<Map<String, Object>> found = findRowsByKey(entry.getValue());
                if (found != null) return found;
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                List<Map<String, Object>> found = findRowsByKey(item);
                if (found != null) return found;
            }
        }
        return null;
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
            return INFO_EMPTY.equals(code) || INFO_EMPTY_LEGACY.equals(code);
        }

        boolean isSuccess() {
            return code == null || code.isBlank()
                    || INFO_OK.equals(code)
                    || INFO_OK_LEGACY_3.equals(code)
                    || INFO_OK_LEGACY_2.equals(code);
        }
    }
}
