package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.ggdatadream;

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
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.ggdatadream.exception.GgDataDreamApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 경기도 데이터드림 어댑터 — 경기 시군(region prefix 41)만 지원.
 * <p>
 * 2026-05-18 dry-run으로 정확한 서비스명 4종 확정 (사용자 제공 가이드 기반):
 * <ul>
 *   <li>{@code /Apttradedelng} — 아파트 매매 실거래 자료 현황</li>
 *   <li>{@code /Apttradedelngdetail} — 아파트 매매 실거래 자료 상세 현황</li>
 *   <li>{@code /Coaltnmlpxhoutrade} — 연립 다세대 매매 실거래 자료 현황</li>
 *   <li>{@code /Snglnsmulthstrade} — 단독 다가구 매매 실거래 자료 현황</li>
 * </ul>
 * <p>
 * 공통 파라미터: {@code KEY, Type, pIndex, pSize, SIGUN_CD}.
 * 응답 필드: {@code SIGUN_CD, SIGUN_NM, LEGALDONG_NM, DELNG_AMT(만원), PRVTUSE_AR}.
 * <p>
 * region별로 4개 endpoint 순차 호출 → 각각 거래수/평균가 2개 indicator → region당 8개 indicator.
 */
@Component
@Slf4j
public class GgDataDreamAdapter implements RealEstateMarketSourceAdapter {

    /** 데이터드림 부동산 거래 dataset — path + indicator prefix. */
    private enum GgDataset {
        APT_TRADE("/Apttradedelng", "APT"),
        APT_TRADE_DETAIL("/Apttradedelngdetail", "APT_DETAIL"),
        COALTN_TRADE("/Coaltnmlpxhoutrade", "COALTN"),
        SNGLNSMULT_TRADE("/Snglnsmulthstrade", "SNGL");

        final String path;
        final String prefix;

        GgDataset(String path, String prefix) {
            this.path = path;
            this.prefix = prefix;
        }
    }

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;
    private final ObjectMapper objectMapper;

    public GgDataDreamAdapter(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                              RealEstateMarketProperties properties,
                              ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public RealEstateMarketSource supportedSource() {
        return RealEstateMarketSource.GG_DATA_DREAM;
    }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.GYEONGGI_LOCAL);
    }

    @Override
    public boolean supportsRegion(RegionCode region) {
        // 경기 시군구만 (SIDO "41"은 광역 row이므로 차단 — REB가 처리)
        return region != null && region.isSigungu() && region.isGyeonggi();
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.GYEONGGI_LOCAL) {
            return FetchResult.failure("unsupported category: " + category);
        }
        RealEstateMarketProperties.SourceProps props = properties.getGgDataDream();
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("GG_DATA_DREAM api-key not configured");
        }
        BaseUri base = BaseUri.parse(props.getBaseUrl());
        String reference = window.to().toString().substring(0, 7);
        LocalDateTime snapshot = LocalDateTime.now();

        // 부분 실패 허용: 한 endpoint 실패가 다른 endpoint 호출을 막지 않도록 catch + log + continue.
        List<RealEstateMarketIndicator> indicators = new ArrayList<>();
        for (GgDataset dataset : GgDataset.values()) {
            try {
                List<Map<String, Object>> rows = callDataset(base, props.getApiKey(), dataset, region);
                indicators.addAll(buildIndicators(region, dataset, rows, reference, snapshot));
            } catch (RestClientException e) {
                Throwable sanitized = SecretMasker.sanitize(e);
                log.warn("[realestate.gg] dataset fetch failed: dataset={}, region={}, cause={}",
                        dataset.path, region,
                        sanitized == null ? null : sanitized.getMessage());
            } catch (Exception e) {
                log.warn("[realestate.gg] dataset fetch failed: dataset={}, region={}",
                        dataset.path, region, e);
            }
        }
        return FetchResult.success(indicators);
    }

    private List<Map<String, Object>> callDataset(BaseUri base,
                                                  String apiKey,
                                                  GgDataset dataset,
                                                  RegionCode region) {
        // 데이터드림은 응답 본문이 JSON이지만 Content-Type을 text/html로 보내는 경우가 있어
        // String으로 받아 ObjectMapper로 직접 파싱한다 (KOSIS와 동일 패턴).
        String body = restClient.get()
                .uri(builder -> builder
                        .scheme(base.scheme())
                        .host(base.host())
                        .port(base.port())
                        .path(base.contextPath() + dataset.path)
                        .queryParam("KEY", apiKey)
                        .queryParam("Type", "json")
                        .queryParam("SIGUN_CD", region.value())
                        .queryParam("pSize", 200)
                        .build())
                .retrieve()
                .body(String.class);
        return parseRows(body);
    }

    /**
     * 데이터드림 envelope 분기 — {@code {DatasetName: [{head:[...]}, {row:[...]}]}}.
     * JSON 파싱은 ObjectMapper로 직접 처리 (text/html Content-Type 회피).
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseRows(String body) {
        if (body == null || body.isBlank()) {
            return Collections.emptyList();
        }
        Map<String, Object> response;
        try {
            response = objectMapper.readValue(body, new TypeReference<>() {});
        } catch (Exception e) {
            log.debug("[realestate.gg] response parse failed (likely empty/error envelope): {}",
                    e.getMessage());
            return Collections.emptyList();
        }
        if (response == null) return Collections.emptyList();
        for (Object value : response.values()) {
            if (value instanceof List<?> list && !list.isEmpty()
                    && list.get(0) instanceof Map<?, ?>) {
                for (Object inner : (List<?>) value) {
                    if (inner instanceof Map<?, ?> wrapper && wrapper.containsKey("row")) {
                        Object rowValue = wrapper.get("row");
                        if (rowValue instanceof List<?> rowList) {
                            return (List<Map<String, Object>>) rowList;
                        }
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private List<RealEstateMarketIndicator> buildIndicators(RegionCode region,
                                                            GgDataset dataset,
                                                            List<Map<String, Object>> rows,
                                                            String reference,
                                                            LocalDateTime snapshot) {
        BigDecimal count = BigDecimal.valueOf(rows.size());
        BigDecimal avgPrice = averageDealAmount(rows);
        return List.of(
                indicator(region, dataset.prefix + "_TRADE_COUNT", count, reference, snapshot),
                indicator(region, dataset.prefix + "_TRADE_AVG_PRICE", avgPrice, reference, snapshot)
        );
    }

    /** {@code DELNG_AMT} (거래금액, 만원) 평균. 콤마/공백/통화기호 제거 후 BigDecimal 누적. */
    private BigDecimal averageDealAmount(List<Map<String, Object>> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Map<String, Object> row : rows) {
            Object raw = row.get("DELNG_AMT");
            if (raw == null) continue;
            try {
                sum = sum.add(new BigDecimal(raw.toString().replace(",", "").trim()));
                count++;
            } catch (NumberFormatException ignored) {
            }
        }
        if (count == 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                String code,
                                                BigDecimal value,
                                                String reference,
                                                LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(RealEstateMarketCategory.GYEONGGI_LOCAL)
                .source(RealEstateMarketSource.GG_DATA_DREAM)
                .indicatorCode(code)
                .referenceText(reference)
                .value(value)
                .sourceUrl("https://data.gg.go.kr/")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }
}
