package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.hug;

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
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.hug.exception.HugApiException;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.molit.dto.MolitApiResponse;
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
 * HUG 분양보증 사고현황/사고금액/사고세대수 어댑터.
 * <p>
 * <b>현재 비활성화 상태</b> — {@code HUG_API_KEY} 빈값 시 BatchScheduler가 자동 skip.
 * <p>
 * <b>비활성화 결정 근거 (2026-05-10):</b>
 * <ol>
 *   <li>HUG 분양보증사고는 data.go.kr 오픈API가 아님 (링크데이터셋 = khug.or.kr 안내).
 *       data.go.kr 표준 envelope({@code response/header/body/items}) 가정한 현재 코드와 호환 안 됨.</li>
 *   <li>HUG 자체 OPEN API({@code https://www.khug.or.kr/houstar/...}) 존재하지만,
 *       실제 endpoint는 {@code /accidentDistributionGuaranteeStatus.do},
 *       파라미터는 {@code API_KEY/STDD_YYD/AREA_DCD} 형태 — 본 어댑터의 현재 path/파라미터와 전혀 다름.</li>
 *   <li>호출 테스트 결과 데이터 응답 빈도가 매우 낮음({@code [{"ERROR_CODE":"03","ERROR_MSG":"NO_DATA"}]}).
 *       분양보증사고는 본질적으로 발생 빈도 낮은 이벤트라 일배치 시계열로 부적합.</li>
 * </ol>
 * <p>
 * <b>후속 sprint에서 결정 필요:</b>
 * <ul>
 *   <li>옵션 1: 어댑터 폐기 + T8 분양 리스크 카테고리 재설계</li>
 *   <li>옵션 2: HUG OPEN API 호환으로 본 어댑터 전면 재작성 (응답 envelope/파라미터 schema 별도)</li>
 *   <li>옵션 3: KOSIS 분양보증 통계표로 우회 (KosisAdapter에 tblId 추가)</li>
 * </ul>
 * <p>
 * 아래 {@code INCIDENT_PATH}는 plan 작성 시 추정값으로 잡은 것이며 실제 작동하지 않습니다.
 * 어댑터 재작성 시 통째로 교체 필요.
 */
@Component
@Slf4j
public class HugAdapter implements RealEstateMarketSourceAdapter {

    /**
     * @deprecated 추정 endpoint — 실제 HUG API와 불일치. 어댑터 재작성 시 교체 필요.
     */
    @Deprecated
    private static final String INCIDENT_PATH = "/MhmDamageMnyDamageHshldCo/getMhmDamageMnyDamageHshldCo";

    private final RestClient restClient;
    private final RealEstateMarketProperties properties;

    public HugAdapter(@Qualifier(RealEstateRestClientConfig.CLIENT_BEAN) RestClient restClient,
                      RealEstateMarketProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public RealEstateMarketSource supportedSource() { return RealEstateMarketSource.HUG; }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(RealEstateMarketCategory.GUARANTEE_RISK);
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (category != RealEstateMarketCategory.GUARANTEE_RISK) {
            return FetchResult.failure("unsupported category: " + category);
        }
        RealEstateMarketProperties.SourceProps hug = properties.getHug();
        if (hug.getApiKey() == null || hug.getApiKey().isBlank()) {
            throw new IllegalStateException("HUG api-key not configured");
        }
        BaseUri base = BaseUri.parse(hug.getBaseUrl());
        try {
            MolitApiResponse response = restClient.get()
                    .uri(builder -> builder
                            .scheme(base.scheme())
                            .host(base.host())
                            .port(base.port())
                            .path(base.contextPath() + INCIDENT_PATH)
                            .queryParam("serviceKey", hug.getApiKey())
                            .queryParam("sigunguCd", region.value())
                            .queryParam("strtYm", window.from().toString().replace("-", "").substring(0, 6))
                            .queryParam("endYm", window.to().toString().replace("-", "").substring(0, 6))
                            .queryParam("_type", "json")
                            .queryParam("numOfRows", 200)
                            .build())
                    .retrieve()
                    .body(MolitApiResponse.class);
            if (response == null || !response.isSuccess()) {
                return FetchResult.success(List.of());
            }
            return FetchResult.success(buildIndicators(region, response.rows(), window));
        } catch (RestClientException e) {
            throw new HugApiException("HUG API call failed", SecretMasker.sanitize(e));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[realestate.hug] fetch failed: region={}", region, e);
            return FetchResult.failure(e.getMessage());
        }
    }

    private List<RealEstateMarketIndicator> buildIndicators(RegionCode region,
                                                            List<Map<String, Object>> rows,
                                                            FetchWindow window) {
        long count = rows.size();
        BigDecimal totalAmount = sumField(rows, "damageAmt", "DamageAmt");
        BigDecimal totalHouseholds = sumField(rows, "damageHshldCo", "DamageHshldCo");
        String reference = window.to().toString().substring(0, 7);
        LocalDateTime snapshot = LocalDateTime.now();
        return List.of(
                indicator(region, "GUARANTEE_INCIDENT_COUNT", BigDecimal.valueOf(count), reference, snapshot),
                indicator(region, "GUARANTEE_INCIDENT_AMOUNT", totalAmount, reference, snapshot),
                indicator(region, "GUARANTEE_INCIDENT_HOUSEHOLDS", totalHouseholds, reference, snapshot)
        );
    }

    private BigDecimal sumField(List<Map<String, Object>> rows, String... keys) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            for (String key : keys) {
                Object raw = row.get(key);
                if (raw == null) continue;
                try {
                    total = total.add(new BigDecimal(raw.toString().replace(",", "").trim()));
                } catch (NumberFormatException ignored) {
                }
                break;
            }
        }
        return total;
    }

    private RealEstateMarketIndicator indicator(RegionCode region,
                                                String code,
                                                BigDecimal value,
                                                String reference,
                                                LocalDateTime snapshot) {
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(RealEstateMarketCategory.GUARANTEE_RISK)
                .source(RealEstateMarketSource.HUG)
                .indicatorCode(code)
                .referenceText(reference)
                .value(value)
                .sourceUrl("https://www.khug.or.kr/")
                .cycle("M")
                .snapshotDate(snapshot)
                .build();
    }

}