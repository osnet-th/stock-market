package com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RegionCode;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchResult;
import com.thlee.stock.market.stockmarket.realestate.domain.service.FetchWindow;
import com.thlee.stock.market.stockmarket.realestate.domain.service.RealEstateMarketSourceAdapter;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.config.RebStatblProperties;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.dto.SttsApiTblDataRow;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.source.reb.dto.SttsApiTblItmRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * R-ONE 광역시도(SIDO) 단위 어댑터.
 * <p>
 * Q1=B/Q4=D1 결정에 따라 SIDO region만 처리. 시군구(SIGUNGU) region은 supportsRegion=false로 차단.
 * 한 fetch 호출은 (region, category) 단위로 4개 주택유형(주택종합/아파트/연립/단독) 또는 카테고리별
 * 단일 STATBL을 모두 적재한다.
 * <p>
 * 명세: {@code .claude/analyzes/realestate/reb-r-one-openapi-spec.md}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RebAdapter implements RealEstateMarketSourceAdapter {

    /**
     * 시도 한글명 → 행정안전부 2자리 sido_code 매핑.
     * 단축형/공식형 모두 등록 (Q2=A 양보적 매핑). R-ONE ITM_NM이 어느 표기를 쓰든 매치 가능.
     * KOSIS 함정 #14 동일 패턴.
     */
    private static final Map<String, String> SIDO_NAME_TO_CODE = Map.ofEntries(
            Map.entry("서울", "11"), Map.entry("서울특별시", "11"), Map.entry("서울시", "11"),
            Map.entry("부산", "26"), Map.entry("부산광역시", "26"), Map.entry("부산시", "26"),
            Map.entry("대구", "27"), Map.entry("대구광역시", "27"), Map.entry("대구시", "27"),
            Map.entry("인천", "28"), Map.entry("인천광역시", "28"), Map.entry("인천시", "28"),
            Map.entry("광주", "29"), Map.entry("광주광역시", "29"), Map.entry("광주시", "29"),
            Map.entry("대전", "30"), Map.entry("대전광역시", "30"), Map.entry("대전시", "30"),
            Map.entry("울산", "31"), Map.entry("울산광역시", "31"), Map.entry("울산시", "31"),
            Map.entry("세종", "36"), Map.entry("세종특별자치시", "36"), Map.entry("세종시", "36"),
            Map.entry("경기", "41"), Map.entry("경기도", "41"),
            Map.entry("충북", "43"), Map.entry("충청북도", "43"),
            Map.entry("충남", "44"), Map.entry("충청남도", "44"),
            Map.entry("전남", "46"), Map.entry("전라남도", "46"),
            Map.entry("경북", "47"), Map.entry("경상북도", "47"),
            Map.entry("경남", "48"), Map.entry("경상남도", "48"),
            Map.entry("제주", "50"), Map.entry("제주특별자치도", "50"), Map.entry("제주도", "50"),
            Map.entry("강원", "51"), Map.entry("강원특별자치도", "51"), Map.entry("강원도", "51"),
            Map.entry("전북", "52"), Map.entry("전북특별자치도", "52"), Map.entry("전라북도", "52")
    );

    private final RebMetadataCache metadataCache;
    private final RebStatblProperties statblProperties;

    @Override
    public RealEstateMarketSource supportedSource() {
        return RealEstateMarketSource.REB;
    }

    @Override
    public Set<RealEstateMarketCategory> supportedCategories() {
        return EnumSet.of(
                RealEstateMarketCategory.PRICE_INDEX,
                RealEstateMarketCategory.RENT,
                RealEstateMarketCategory.OFFICIAL_STATS);
    }

    /**
     * R-ONE은 SIDO 단위만 처리. SIGUNGU region은 차단 (Scheduler 헛 submit 방지).
     */
    @Override
    public boolean supportsRegion(RegionCode region) {
        return region != null && region.isSido();
    }

    @Override
    public FetchResult fetch(RegionCode region,
                             RealEstateMarketCategory category,
                             FetchWindow window) {
        if (!supportedCategories().contains(category)) {
            return FetchResult.failure("unsupported category: " + category);
        }
        if (!supportsRegion(region)) {
            return FetchResult.failure("REB supports SIDO only: " + region);
        }
        // api-key 미설정 시 IllegalStateException — Scheduler가 항목 단위 skip (다른 어댑터와 동일 패턴)
        // RebClient.fetchItm/fetchData가 키 검사를 수행한다.
        List<StatblTarget> targets = resolveTargets(category);
        if (targets.isEmpty()) {
            return FetchResult.failure("REB STATBL not mapped for category=" + category);
        }
        List<RealEstateMarketIndicator> indicators = new java.util.ArrayList<>();
        for (StatblTarget target : targets) {
            try {
                indicators.addAll(fetchOne(region, category, target, window));
            } catch (IllegalStateException e) {
                throw e; // api-key 미설정은 상위로 — Scheduler가 항목 skip
            } catch (Exception e) {
                // 부분 실패 허용 (#16): 한 STATBL 실패가 다른 STATBL을 막지 않음
                log.warn("[realestate.reb] partial-failure region={}, category={}, statbl={} ({})",
                        region, category, target.statblId(), e.getClass().getSimpleName());
            }
        }
        return FetchResult.success(indicators);
    }

    private List<RealEstateMarketIndicator> fetchOne(RegionCode region,
                                                     RealEstateMarketCategory category,
                                                     StatblTarget target,
                                                     FetchWindow window) {
        SttsApiTblItmRow itm = resolveItm(target.statblId(), region.value());
        if (itm == null) {
            log.warn("[realestate.reb] no ITM match: statbl={}, sidoCode={}",
                    target.statblId(), region.value());
            return List.of();
        }
        String startWrttime = window.from().toString().replace("-", "").substring(0, 6);
        String endWrttime = window.to().toString().replace("-", "").substring(0, 6);

        // 동일 (STATBL, window)의 17 SIDO 호출은 캐시 hit으로 처리 (P0 #5 — redundant 호출 폭증 해소).
        // 첫 region 호출이 STATBL 전체 데이터를 받아 캐시, 나머지 16 region은 메모리에서 region 필터만.
        List<SttsApiTblDataRow> rows = metadataCache.dataFor(
                target.statblId(), target.dtacycleCd(), startWrttime, endWrttime);

        // 메모리 region 필터 (Q4=D1).
        // R-ONE 통계표는 (GRP_ID, CLS_ID, ITM_ID) 다차원 — region이 어느 차원에 있는지는 STATBL마다 다르다.
        // 예: A_2024_00017(매매가격지수) → region이 CLS_ID, ITM_ID는 단일 "지수".
        // ITM API의 ITM_TAG=분류의 ITM_ID는 DATA의 GRP_ID/CLS_ID/ITM_ID 중 어느 하나와 매치된다.
        String regionId = itm.itmId();
        List<SttsApiTblDataRow> filtered = regionId == null ? List.of() : rows.stream()
                .filter(row -> regionId.equals(row.clsId())
                        || regionId.equals(row.grpId())
                        || regionId.equals(row.itmId()))
                .toList();
        if (filtered.isEmpty()) {
            return List.of();
        }
        SttsApiTblDataRow latest = filtered.stream()
                .max(Comparator.comparing(r -> r.wrttimeIdtfrId() == null ? "" : r.wrttimeIdtfrId()))
                .orElse(null);
        if (latest == null || latest.dtaVal() == null) {
            return List.of();
        }
        return List.of(buildIndicator(region, category, target, latest));
    }

    private SttsApiTblItmRow resolveItm(String statblId, String sidoCode) {
        // 캐시된 ITM 목록에서 매칭되는 시도 row 찾기
        List<SttsApiTblItmRow> itms = metadataCache.itmsFor(statblId);
        for (SttsApiTblItmRow row : itms) {
            String code = SIDO_NAME_TO_CODE.get(row.itmNm());
            if (code != null && code.equals(sidoCode)) {
                return row;
            }
            // ITM_FULLNM에 시도명이 포함된 경우(예: "전국>수도권>서울") 보조 매치
            if (row.itmFullnm() != null) {
                for (Map.Entry<String, String> entry : SIDO_NAME_TO_CODE.entrySet()) {
                    if (row.itmFullnm().contains(entry.getKey()) && entry.getValue().equals(sidoCode)) {
                        return row;
                    }
                }
            }
        }
        // 매핑 누락 진단: ITM_NM 목록을 ERROR 로그로 노출 → 운영자가 새 시도명 변형을
        // SIDO_NAME_TO_CODE에 추가하거나 R-ONE 응답 schema 변경 audit 가능
        String unmatchedSample = itms.stream()
                .map(r -> r == null ? "null" : r.itmNm())
                .filter(Objects::nonNull)
                .limit(20)
                .collect(Collectors.joining(", "));
        log.error("[realestate.reb] SIDO mapping miss: statbl={}, sidoCode={}, itmCount={}, itmNm sample=[{}]"
                        + " — SIDO_NAME_TO_CODE에 변형 추가 또는 R-ONE 응답 audit 필요",
                statblId, sidoCode, itms.size(), unmatchedSample);
        return null;
    }

    private List<StatblTarget> resolveTargets(RealEstateMarketCategory category) {
        // 4개 주택유형 묶음 적재. 카테고리 → entries.
        return switch (category) {
            case PRICE_INDEX -> nonNull(
                    of(statblProperties.getSalePriceTotal()),
                    of(statblProperties.getSalePriceApt()),
                    of(statblProperties.getSalePriceRowhouse()),
                    of(statblProperties.getSalePriceSinglehouse())
            );
            case RENT -> nonNull(
                    of(statblProperties.getRentPriceTotal()),
                    of(statblProperties.getRentPriceApt()),
                    of(statblProperties.getRentPriceRowhouse()),
                    of(statblProperties.getRentPriceSinglehouse())
            );
            case OFFICIAL_STATS -> nonNull(
                    of(statblProperties.getTradeVolumeTotal()),
                    of(statblProperties.getTradeVolumeApt()),
                    of(statblProperties.getLandPriceChange())
            );
            default -> List.of();
        };
    }

    private StatblTarget of(RebStatblProperties.Entry entry) {
        if (entry == null || entry.getStatblId() == null || entry.getDtacycleCd() == null) {
            return null;
        }
        // Q1=A 정책: indicator-code 매핑이 있는 STATBL만 적재 (4종 묶음 중 대표 1종만).
        if (entry.getIndicatorCode() == null || entry.getIndicatorCode().isBlank()) {
            return null;
        }
        return new StatblTarget(entry.getStatblId(), entry.getDtacycleCd(), entry.getIndicatorCode());
    }

    private List<StatblTarget> nonNull(StatblTarget... entries) {
        List<StatblTarget> result = new java.util.ArrayList<>(entries.length);
        for (StatblTarget e : entries) {
            if (e != null) result.add(e);
        }
        return result;
    }

    private RealEstateMarketIndicator buildIndicator(RegionCode region,
                                                     RealEstateMarketCategory category,
                                                     StatblTarget target,
                                                     SttsApiTblDataRow row) {
        BigDecimal value = row.dtaVal();
        return RealEstateMarketIndicator.builder()
                .regionCode(region.value())
                .category(category)
                .source(RealEstateMarketSource.REB)
                .indicatorCode(target.indicatorCode())
                .referenceText(row.wrttimeIdtfrId() == null ? "" : row.wrttimeIdtfrId())
                .value(value)
                .sourceUrl("https://www.reb.or.kr/r-one/")
                .cycle(target.dtacycleCd())
                .snapshotDate(LocalDateTime.now())
                .build();
    }

    private record StatblTarget(String statblId, String dtacycleCd, String indicatorCode) {
    }
}