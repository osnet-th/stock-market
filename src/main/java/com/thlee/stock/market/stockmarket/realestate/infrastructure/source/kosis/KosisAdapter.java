package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.kosis;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateRestClientConfig;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.BaseUri;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.SecretMasker;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.kosis.exception.KosisApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KOSIS 시군구별 미분양현황 어댑터.
 * <p>
 * 호출 API: KOSIS Open API "통계자료 — 매개변수 입력 방식(통계표선택)".
 * 2026-05-17 운영 dry-run에서 endpoint deprecated 확인되어 신규 path로 정정:
 * <ul>
 *   <li>기존: {@code /openapi/statisticsParameterData.do}</li>
 *   <li>신규: {@code /openapi/Param/statisticsParameterData.do}</li>
 * </ul>
 * <p>
 * <b>미완료 — 필수 파라미터 매핑 필요:</b>
 * 신규 가이드에 {@code itmId}(항목 ID)가 필수로 명시되어 있으나 plan 작성 시 추정 매핑된
 * {@code orgId=116} / {@code tblId=DT_MLTM_2082} 모두 실제 KOSIS 통계DB에서 정확한 값이
 * 검증되지 않음. 신규 endpoint로 호출은 통과하나 {@code itmId} 누락 또는 {@code tblId} 오류로
 * 실패할 가능성 — KOSIS 통계DB 검색으로 미분양 통계표의 {@code orgId/tblId/itmId} 매핑이
 * 별도 필요.
 */
@Component
@Slf4j
public class KosisAdapter implements RealEstateMarketSourceAdapter {

    /** KOSIS 신규 매개변수 방식 endpoint (Param path 추가). */
    private static final String STAT_PATH = "/Param/statisticsParameterData.do";
    private static final String DEFAULT_ORG_ID = "116";
    private static final String DEFAULT_TBL_ID = "DT_MLTM_2082";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    public KosisAdapter(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                        RealEstateMarketProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public RealEstateMarketSource supportedSource() { return RealEstateMarketSource.KOSIS; }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.UNSOLD);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.UNSOLD) {
            return FetchResult.failure("unsupported category: " + category);
        }
        RealEstateMarketProperties.SourceProps kosis = properties.getKosis();
        if (kosis.getApiKey() == null || kosis.getApiKey().isBlank()) {
            throw new IllegalStateException("KOSIS api-key not configured");
        }
        BaseUri base = BaseUri.parse(kosis.getBaseUrl());
        try {
            List<Map<String, Object>> rows = restClient.get()
                    .uri(builder -> builder
                            .scheme(base.scheme())
                            .host(base.host())
                            .port(base.port())
                            .path(base.contextPath() + STAT_PATH)
                            .queryParam("method", "getList")
                            .queryParam("apiKey", kosis.getApiKey())
                            .queryParam("orgId", DEFAULT_ORG_ID)
                            .queryParam("tblId", DEFAULT_TBL_ID)
                            .queryParam("objL1", region.value())
                            .queryParam("prdSe", "M")
                            .queryParam("startPrdDe", window.from().toString().replace("-", "").substring(0, 6))
                            .queryParam("endPrdDe", window.to().toString().replace("-", "").substring(0, 6))
                            .queryParam("format", "json")
                            // jsonVD는 deprecated. 신규 가이드에서 미정의.
                            // itmId(필수)는 통계표별 매핑이 필요해 plan에서 미설정 — 후속 작업.
                            .build())
                    .retrieve()
                    .body(List.class);
            if (rows == null || rows.isEmpty()) {
                return FetchResult.success(List.of());
            }
            return FetchResult.success(buildIndicators(region, rows, window));
        } catch (RestClientException e) {
            throw new KosisApiException("KOSIS API call failed", SecretMasker.sanitize(e));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.kosis] fetch failed: region={}", region, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    private List<RealEstateMarketIndicator> buildIndicators(RegionCode region,
                                                            List<Map<String, Object>> rows,
                                                            FetchWindow window) {
        BigDecimal unsold = BigDecimal.ZERO;
        BigDecimal postCompletion = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            String itmId = String.valueOf(row.get("ITM_ID"));
            BigDecimal value = parseValue(row.get("DT"));
            if (value == null) continue;
            if (itmId.contains("UNSOLD") || itmId.contains("미분양")) {
                unsold = unsold.add(value);
            } else if (itmId.contains("POST") || itmId.contains("준공")) {
                postCompletion = postCompletion.add(value);
            }
        }
        String reference = window.to().toString().substring(0, 7);
        LocalDateTime snapshot = LocalDateTime.now();
        return List.of(
                indicator(region, "UNSOLD_HOUSEHOLDS", unsold, reference, snapshot),
                indicator(region, "POST_COMPLETION_UNSOLD", postCompletion, reference, snapshot)
        );
    }

    private BigDecimal parseValue(Object raw) {
        if (raw == null) return null;
        try {
            return new BigDecimal(raw.toString().replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                String code,
                                                BigDecimal value,
                                                String reference,
                                                LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(RealEstateMarketCategory.UNSOLD)
                .source(RealEstateMarketSource.KOSIS)
                .indicatorCode(code)
                .referenceText(reference)
                .value(value)
                .sourceUrl("https://kosis.kr/")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }

}