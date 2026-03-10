package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisMasterStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 국내주식 마스터파일(.mst) 파서
 * 파일 구조: [단축코드(9)][표준코드(12)][한글명(가변)][부가정보(228바이트)]
 */
@Slf4j
@Component
public class KisDomesticMasterFileParser {

    private static final Charset CP949 = Charset.forName("CP949");
    private static final int SUFFIX_BYTE_LENGTH = 228;

    /**
     * .mst 파일 InputStream을 파싱하여 종목 리스트 반환
     */
    public List<KisMasterStock> parse(InputStream inputStream, MarketType marketType) throws IOException {
        byte[] allBytes = inputStream.readAllBytes();
        String content = new String(allBytes, CP949);

        List<KisMasterStock> stocks = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                KisMasterStock stock = parseLine(line, marketType);
                if (stock != null) {
                    stocks.add(stock);
                }
            }
        }

        return stocks;
    }

    private KisMasterStock parseLine(String line, MarketType marketType) {
        try {
            byte[] lineBytes = line.getBytes(CP949);
            if (lineBytes.length <= SUFFIX_BYTE_LENGTH + 21) {
                return null;
            }

            int prefixLength = lineBytes.length - SUFFIX_BYTE_LENGTH;
            String prefix = new String(lineBytes, 0, prefixLength, CP949);

            String shortCode = prefix.substring(0, 9).trim();
            String koreanName = prefix.substring(21).trim();

            if (shortCode.isEmpty() || koreanName.isEmpty()) {
                return null;
            }

            return new KisMasterStock(shortCode, koreanName, null, marketType, ExchangeCode.KRX);
        } catch (Exception e) {
            log.warn("국내 마스터파일 행 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}