package com.thlee.stock.market.stockmarket.realestate.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * R-ONE(한국부동산원) STATBL_ID 11개 + DTACYCLE_CD 매핑.
 * <p>
 * 공유 {@link RealEstateMarketProperties.SourceProps} 구조를 흩뜨리지 않기 위해 별도 prefix
 * ({@code realestate.reb.statbl})로 분리. {@code RebClient}/{@code RebAdapter}만 본 빈을 주입받는다.
 * <p>
 * 각 entry는 (statbl-id, dtacycle-cd) 쌍. {@code DTACYCLE_CD}는 발표 주기 메타이므로 통계표가
 * deprecated되거나 주기 변경되지 않는 한 고정값 (예: "MM" = 월간). 시점 인자({@code WRTTIME_IDTFR_ID}
 * 등)는 본 properties와 무관 — adapter가 {@code FetchWindow}에서 자동 계산.
 * <p>
 * 강타입 필드로 typo 방지 — 신규 STATBL 추가 시 본 클래스 필드와 yml 양쪽 동시 갱신.
 * 모든 필드는 부팅 시 non-null 필수 (validation은 RebAdapter 초기화 단계에서).
 */
@Getter
@Setter
@Slf4j
@Component
@ConfigurationProperties(prefix = "realestate.reb.statbl")
public class RebStatblProperties {

    // 매매가격지수
    /** 매매가격지수 — 주택종합 */
    private Entry salePriceTotal;
    /** 매매가격지수 — 아파트 */
    private Entry salePriceApt;
    /** 매매가격지수 — 연립/다세대 */
    private Entry salePriceRowhouse;
    /** 매매가격지수 — 단독주택 */
    private Entry salePriceSinglehouse;

    // 전세가격지수
    /** 전세가격지수 — 주택종합 */
    private Entry rentPriceTotal;
    /** 전세가격지수 — 아파트 */
    private Entry rentPriceApt;
    /** 전세가격지수 — 연립/다세대 */
    private Entry rentPriceRowhouse;
    /** 전세가격지수 — 단독주택 */
    private Entry rentPriceSinglehouse;

    // 거래량
    /** 거래량 — 주택종합 */
    private Entry tradeVolumeTotal;
    /** 거래량 — 아파트 */
    private Entry tradeVolumeApt;

    // 지가변동률
    /** 지가변동률 */
    private Entry landPriceChange;

    /**
     * 부팅 시 11개 entry + 각 entry의 statblId/dtacycleCd 모두 non-null 검증.
     * 누락 시 부팅 fail-fast — yml 오타로 인한 silent 부분 작동 차단.
     */
    @PostConstruct
    void validateEntries() {
        List<String> missing = new ArrayList<>();
        check("salePriceTotal", salePriceTotal, missing);
        check("salePriceApt", salePriceApt, missing);
        check("salePriceRowhouse", salePriceRowhouse, missing);
        check("salePriceSinglehouse", salePriceSinglehouse, missing);
        check("rentPriceTotal", rentPriceTotal, missing);
        check("rentPriceApt", rentPriceApt, missing);
        check("rentPriceRowhouse", rentPriceRowhouse, missing);
        check("rentPriceSinglehouse", rentPriceSinglehouse, missing);
        check("tradeVolumeTotal", tradeVolumeTotal, missing);
        check("tradeVolumeApt", tradeVolumeApt, missing);
        check("landPriceChange", landPriceChange, missing);
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "RebStatblProperties: 누락된 entry/field — " + missing
                            + ". application.yml의 realestate.reb.statbl 블록을 확인하세요.");
        }
        log.info("[realestate.reb.statbl] 11개 STATBL entry 검증 완료");
    }

    private static void check(String name, Entry entry, List<String> missing) {
        if (entry == null) {
            missing.add(name + "(missing)");
            return;
        }
        if (entry.getStatblId() == null || entry.getStatblId().isBlank()) {
            missing.add(name + ".statblId");
        }
        if (entry.getDtacycleCd() == null || entry.getDtacycleCd().isBlank()) {
            missing.add(name + ".dtacycleCd");
        }
    }

    /**
     * R-ONE STATBL entry — (통계표 ID, 발표 주기 코드) 쌍.
     */
    @Getter
    @Setter
    public static class Entry {
        /** R-ONE 통계표 ID (예: A_2024_00017) */
        private String statblId;
        /** 발표 주기 코드 (예: "MM" = 월간). R-ONE 명세상 CHAR(11). */
        private String dtacycleCd;
    }
}
