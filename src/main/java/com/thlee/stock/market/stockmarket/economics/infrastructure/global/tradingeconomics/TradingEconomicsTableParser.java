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