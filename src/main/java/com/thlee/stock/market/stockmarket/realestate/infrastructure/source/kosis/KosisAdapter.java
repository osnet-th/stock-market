package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.kosis;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.repository.RealEstateRegionRepository;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateMarketProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RealEstateRestClientConfig;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.BaseUri;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.common.SecretMasker;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.kosis.exception.KosisApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * KOSIS 시·군·구별 미분양현황 어댑터.
 * <p>
 * 호출 API: KOSIS Open API "통계자료 — 매개변수 입력 방식(통계표선택)".
 * 2026-05-17 dry-run 결과 신규 path + itmId 매핑 확정:
 * <ul>
 *   <li>endpoint: {@code /openapi/Param/statisticsParameterData.do}</li>
 *   <li>orgId=116 (국토교통부), tblId=DT_MLTM_2082 (시·군·구별 미분양현황)</li>
 *   <li>itmId={@code 13103871087T1} (미분양현황 항목)</li>
 *   <li>objL1=ALL, objL2=ALL — 한 번 호출로 전국 시도+시군구 데이터 수신</li>
 * </ul>
 * <p>
 * 응답의 {@code C1_NM}(시도명) + {@code C2_NM}(시군구명)을 region entity의
 * sidoName/sigunguName과 매칭해 해당 region 데이터만 추출한다.
 * BatchScheduler가 region별로 fetch()를 호출하지만 매번 같은 ALL 응답을 받게 되므로
 * 부하 최적화는 후속 — 현재 일일 트래픽 한도(10,000회) 대비 56 region × 1일 = 56회로 충분 여유.
 */
@Component
@Slf4j
public class KosisAdapter implements RealEstateMarketSourceAdapter {

    private static final String STAT_PATH = "/Param/statisticsParameterData.do";
    private static final String DEFAULT_ORG_ID = "116";
    private static final String DEFAULT_TBL_ID = "DT_MLTM_2082";
    /** 미분양현황 항목 ID — dry-run에서 확정. */
    private static final String DEFAULT_ITM_ID = "13103871087T1";

    /** 최근 N개월 수신 — KOSIS는 미분양 발표 lag이 1~2개월이라 시점기준 호출은 빈 데이터 위험. */
    private static final int RECENT_PRD_CNT = 3;

    /**
     * KOSIS 응답의 시도명(C1_NM)은 약식("서울"/"경기")인 반면 region entity의 sidoName은
     * 정식 행정구역명("서울특별시"/"경기도"). 매핑 테이블로 정규화한다.
     */
    private static final Map<String, String> KOSIS_SIDO_TO_OFFICIAL = Map.ofEntries(
            Map.entry("서울", "서울특별시"),
            Map.entry("부산", "부산광역시"),
            Map.entry("대구", "대구광역시"),
            Map.entry("인천", "인천광역시"),
            Map.entry("광주", "광주광역시"),
            Map.entry("대전", "대전광역시"),
            Map.entry("울산", "울산광역시"),
            Map.entry("세종", "세종특별자치시"),
            Map.entry("경기", "경기도"),
            Map.entry("강원", "강원특별자치도"),
            Map.entry("충북", "충청북도"),
            Map.entry("충남", "충청남도"),
            Map.entry("전북", "전북특별자치도"),
            Map.entry("전남", "전라남도"),
            Map.entry("경북", "경상북도"),
            Map.entry("경남", "경상남도"),
            Map.entry("제주", "제주특별자치도")
    );

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;
    private final RealEstateRegionRepository regionRepository;
    private final ObjectMapper objectMapper;

    public KosisAdapter(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                        RealEstateMarketProperties properties,
                        RealEstateRegionRepository regionRepository,
                        ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.regionRepository = regionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public RealEstateMarketSource supportedSource() { return RealEstateMarketSource.KOSIS; }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.UNSOLD);
    }

    @Override
    public boolean supportsRegion(RegionCode region) {
        // KOSIS는 시군구 단위만 — SIDO row 차단
        return region != null && region.isSigungu();
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
        // region.value() → RealEstateRegion entity 조회로 한글 시도/시군구명 확보
        RealEstateRegion regionEntity = regionRepository
                .findByRegionCodeAndEmdCode(region.value(), "")
                .orElse(null);
        if (regionEntity == null) {
            return FetchResult.failure("region entity not found: " + region.value());
        }

        BaseUri base = BaseUri.parse(kosis.getBaseUrl());
        try {
            // KOSIS는 응답 본문이 JSON이지만 Content-Type을 text/html로 보내는 경우가 있어
            // String으로 받아 ObjectMapper로 직접 파싱한다.
            String body = restClient.get()
                    .uri(builder -> builder
                            .scheme(base.scheme())
                            .host(base.host())
                            .port(base.port())
                            .path(base.contextPath() + STAT_PATH)
                            .queryParam("method", "getList")
                            .queryParam("apiKey", kosis.getApiKey())
                            .queryParam("orgId", DEFAULT_ORG_ID)
                            .queryParam("tblId", DEFAULT_TBL_ID)
                            .queryParam("itmId", DEFAULT_ITM_ID)
                            // objL1=ALL, objL2=ALL — 전국 시도+시군구 일괄 수신
                            .queryParam("objL1", "ALL")
                            .queryParam("objL2", "ALL")
                            .queryParam("prdSe", "M")
                            // 시점기준(startPrdDe/endPrdDe) 대신 최신 N개월 기준 — 발표 lag 회피
                            .queryParam("newEstPrdCnt", RECENT_PRD_CNT)
                            .queryParam("format", "json")
                            .queryParam("jsonVD", "Y")
                            .build())
                    .retrieve()
                    .body(String.class);
            List<Map<String, Object>> rows = parseRows(body);
            if (rows.isEmpty()) {
                return FetchResult.success(List.of());
            }
            return FetchResult.success(buildIndicators(region, regionEntity, rows, window));
        } catch (RestClientException e) {
            throw new KosisApiException("KOSIS API call failed", SecretMasker.sanitize(e));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.kosis] fetch failed: region={}", region, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    /**
     * KOSIS 응답 body 파싱.
     * <p>
     * 정상: JSON 배열 {@code [{...}, {...}]}.
     * 에러: JSON 객체 {@code {"err":"30","errMsg":"데이터가 존재하지 않습니다."}}.
     * 두 케이스를 모두 흡수해 정상은 row 리스트, 에러는 빈 리스트로 반환.
     */
    private List<Map<String, Object>> parseRows(String body) {
        if (body == null || body.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.contains("\"err\"")) {
            log.debug("[realestate.kosis] empty/error response: {}", trimmed);
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(trimmed, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[realestate.kosis] response parse failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 응답 row 중 {@code C1_NM}(시도명) + {@code C2_NM}(시군구명)이 region과 매칭되는 것만 추출.
     * 가장 최근 {@code PRD_DE} row의 값을 사용.
     */
    private List<RealEstateMarketIndicator> buildIndicators(RegionCode region,
                                                            RealEstateRegion regionEntity,
                                                            List<Map<String, Object>> rows,
                                                            FetchWindow window) {
        String sidoName = regionEntity.getSidoName();
        String sigunguName = regionEntity.getSigunguName();

        BigDecimal latestValue = null;
        String latestPrdDe = null;
        for (Map<String, Object> row : rows) {
            String rowSido = String.valueOf(row.get("C1_NM"));
            String officialSido = KOSIS_SIDO_TO_OFFICIAL.getOrDefault(rowSido, rowSido);
            if (!sidoName.equals(officialSido)) continue;
            if (!sigunguName.equals(String.valueOf(row.get("C2_NM")))) continue;
            BigDecimal value = parseValue(row.get("DT"));
            if (value == null) continue;
            String prdDe = String.valueOf(row.get("PRD_DE"));
            if (latestPrdDe == null || prdDe.compareTo(latestPrdDe) > 0) {
                latestPrdDe = prdDe;
                latestValue = value;
            }
        }
        if (latestValue == null) {
            return List.of();
        }
        String reference = latestPrdDe.length() >= 6
                ? latestPrdDe.substring(0, 4) + "-" + latestPrdDe.substring(4, 6)
                : window.to().toString().substring(0, 7);
        LocalDateTime snapshot = LocalDateTime.now();
        return List.of(
                indicator(region, "UNSOLD_HOUSEHOLDS", latestValue, reference, snapshot)
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