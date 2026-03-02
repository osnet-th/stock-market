# Infrastructure Parsing 구현 예시

## RawTableRow (infrastructure/global/tradingeconomics/dto)

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto;

/**
 * 파싱된 raw 테이블 행
 * 모든 값은 원문 문자열 그대로 보관
 */
public record RawTableRow(
    String country,
    String last,
    String previous,
    String reference,
    String unit
) {}
```

## ParsedTable (infrastructure/global/tradingeconomics/dto)

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto;

import java.util.List;
import java.util.Map;

public record ParsedTable(
    Map<String, Integer> headerIndex,
    List<RawTableRow> rows
) {}
```

## TradingEconomicsTableParser (infrastructure/global/tradingeconomics)

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto.ParsedTable;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto.RawTableRow;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception.TradingEconomicsParseException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TradingEconomicsTableParser {

    // 한글/영문 헤더 → 표준 키 매핑
    private static final Map<String, String> HEADER_ALIASES = Map.of(
        "국가", "country",
        "country", "country",
        "마지막", "last",
        "last", "last",
        "이전", "previous",
        "previous", "previous",
        "참고", "reference",
        "reference", "reference",
        "단위", "unit",
        "unit", "unit"
    );

    public ParsedTable parse(Document doc) {
        Element table = doc.selectFirst("table.table-heatmap");
        if (table == null) {
            throw new TradingEconomicsParseException("테이블을 찾을 수 없습니다.");
        }

        Map<String, Integer> headerIndex = buildHeaderIndex(table.select("thead th"));
        validateRequiredHeaders(headerIndex);

        List<RawTableRow> rows = new ArrayList<>();
        for (Element tr : table.select("tbody tr")) {
            Elements tds = tr.select("td");
            rows.add(new RawTableRow(
                textAt(tds, headerIndex.get("country")),
                textAt(tds, headerIndex.get("last")),
                textAt(tds, headerIndex.get("previous")),
                textAt(tds, headerIndex.get("reference")),
                textAt(tds, headerIndex.get("unit"))
            ));
        }

        return new ParsedTable(headerIndex, rows);
    }

    private Map<String, Integer> buildHeaderIndex(Elements ths) {
        Map<String, Integer> index = new LinkedHashMap<>();
        for (int i = 0; i < ths.size(); i++) {
            String raw = ths.get(i).text().trim().toLowerCase();
            String normalized = HEADER_ALIASES.getOrDefault(raw, raw);
            index.put(normalized, i);
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerIndex) {
        if (!headerIndex.containsKey("country") || !headerIndex.containsKey("last")) {
            throw new TradingEconomicsParseException(
                "필수 헤더 누락. 발견된 헤더: " + headerIndex.keySet());
        }
    }

    private String textAt(Elements tds, Integer index) {
        if (index == null || index >= tds.size()) {
            return null;
        }
        return tds.get(index).text().trim();
    }
}
```

## TradingEconomicsValueNormalizer (infrastructure/global/tradingeconomics)

```java
package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorValue;
import com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto.RawTableRow;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class TradingEconomicsValueNormalizer {

    private static final BigxLogger log = BigxLogger.create(TradingEconomicsValueNormalizer.class);

    public List<CountryIndicatorSnapshot> normalize(
            GlobalEconomicIndicatorType indicatorType,
            List<RawTableRow> rows) {

        return rows.stream()
            .map(row -> toSnapshot(indicatorType, row))
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<CountryIndicatorSnapshot> toSnapshot(
            GlobalEconomicIndicatorType indicatorType,
            RawTableRow row) {
        try {
            return Optional.of(CountryIndicatorSnapshot.builder()
                .countryName(row.country())
                .indicatorType(indicatorType)
                .lastValue(toIndicatorValue(row.last(), row.unit()))
                .previousValue(toIndicatorValue(row.previous(), row.unit()))
                .referenceText(row.reference())
                .collectedAt(LocalDateTime.now())
                .build());
        } catch (Exception e) {
            log.warn("row 정규화 실패, 스킵. country={}, cause={}", row.country(), e.getMessage());
            return Optional.empty();
        }
    }

    private IndicatorValue toIndicatorValue(String rawText, String unit) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }
        return new IndicatorValue(rawText, parseNumeric(rawText), unit);
    }

    private BigDecimal parseNumeric(String text) {
        try {
            String cleaned = text.replaceAll("[,%\\s]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
```