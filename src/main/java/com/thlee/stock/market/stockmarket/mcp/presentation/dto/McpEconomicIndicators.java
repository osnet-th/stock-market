package com.thlee.stock.market.stockmarket.mcp.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorValue;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 경제지표 (Claude 응답용).
 *
 * @param scope    ECOS(한국은행 100대 지표 카테고리) / GLOBAL(국가별 지표 카테고리) / RECENT(최근 7일 갱신 지표)
 * @param category 조회한 카테고리 코드 (RECENT면 null)
 */
public record McpEconomicIndicators(String scope, String category, List<Item> items) {

    /**
     * 지표 1건. 값은 출처 문자열 그대로다.
     *
     * @param source  ECOS(한국은행) / GLOBAL(Trading Economics)
     * @param country 국가 (ECOS는 대한민국)
     * @param group   ECOS 분류명 또는 글로벌 지표 카테고리
     * @param unit    단위 (출처가 주지 않으면 null)
     * @param period  기준 시점(ECOS 주기 값, 글로벌은 참고 문구)
     */
    public record Item(String source, String country, String group, String name, String value,
                       String previousValue, String unit, String period, LocalDateTime updatedAt) {}

    public static McpEconomicIndicators ecos(String category, List<KeyStatIndicator> indicators) {
        return new McpEconomicIndicators("ECOS", category, indicators.stream()
                .map(i -> new Item("ECOS", "대한민국", i.className(), i.keystatName(), i.dataValue(),
                        i.previousDataValue(), i.unitName(), i.cycle(), null))
                .toList());
    }

    public static McpEconomicIndicators global(String category,
                                               Map<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>> data) {
        List<Item> items = new ArrayList<>();
        data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> e.getValue().forEach(s -> items.add(new Item("GLOBAL", s.getCountryName(),
                        e.getKey().getCategory().getDisplayName(), e.getKey().getDisplayName(),
                        text(s.getLastValue()), text(s.getPreviousValue()), unit(s.getLastValue()),
                        s.getReferenceText(), s.getCollectedAt()))));
        return new McpEconomicIndicators("GLOBAL", category, items);
    }

    public static McpEconomicIndicators recent(List<EcosIndicatorLatest> ecos, List<GlobalIndicatorLatest> global) {
        List<Item> items = new ArrayList<>();
        ecos.forEach(i -> items.add(new Item("ECOS", "대한민국", i.getClassName(), i.getKeystatName(),
                i.getDataValue(), i.getPreviousDataValue(), null, i.getCycle(), i.getUpdatedAt())));
        global.forEach(i -> items.add(new Item("GLOBAL", i.getCountryName(),
                i.getIndicatorType().getCategory().getDisplayName(), i.getIndicatorType().getDisplayName(),
                i.getDataValue(), i.getPreviousDataValue(), i.getUnit(), i.getCycle(), i.getUpdatedAt())));
        items.sort(Comparator.comparing(Item::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return new McpEconomicIndicators("RECENT", null, items);
    }

    private static String text(IndicatorValue v) {
        return v == null ? null : v.getRawText();
    }

    private static String unit(IndicatorValue v) {
        return v == null ? null : v.getUnit();
    }
}
