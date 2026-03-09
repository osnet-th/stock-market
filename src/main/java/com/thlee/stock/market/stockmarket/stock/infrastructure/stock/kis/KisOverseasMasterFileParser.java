package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisMasterStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 해외주식 마스터파일(.cod) 파서
 * 파일 구조: 탭 구분 24개 컬럼
 */
@Slf4j
@Component
public class KisOverseasMasterFileParser {

    private static final Charset CP949 = Charset.forName("CP949");
    private static final int MIN_COLUMN_COUNT = 8;

    /**
     * .cod 파일 InputStream을 파싱하여 종목 리스트 반환
     */
    public List<KisMasterStock> parse(InputStream inputStream, MarketType marketType,
                                       ExchangeCode exchangeCode) throws IOException {
        List<KisMasterStock> stocks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CP949))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                KisMasterStock stock = parseLine(line, marketType, exchangeCode);
                if (stock != null) {
                    stocks.add(stock);
                }
            }
        }

        return stocks;
    }

    private KisMasterStock parseLine(String line, MarketType marketType, ExchangeCode exchangeCode) {
        try {
            String[] columns = line.split("\t");
            if (columns.length < MIN_COLUMN_COUNT) {
                return null;
            }

            String symbol = columns[4].trim();
            String koreanName = columns[6].trim();
            String englishName = columns[7].trim();

            if (symbol.isEmpty()) {
                return null;
            }

            return new KisMasterStock(symbol, koreanName, englishName, marketType, exchangeCode);
        } catch (Exception e) {
            log.warn("해외 마스터파일 행 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}