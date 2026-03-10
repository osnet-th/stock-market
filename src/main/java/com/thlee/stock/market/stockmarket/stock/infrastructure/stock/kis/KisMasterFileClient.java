package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.config.KisProperties;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisMasterStock;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisMasterFileClient {

    private final KisProperties properties;
    private final KisDomesticMasterFileParser domesticParser;
    private final KisOverseasMasterFileParser overseasParser;

    /**
     * MarketType → ExchangeCode 매핑 (해외)
     * 국내는 모두 KRX이므로 매핑 불필요
     */
    private static final Map<MarketType, ExchangeCode> OVERSEAS_EXCHANGE_MAP = Map.ofEntries(
        Map.entry(MarketType.NASDAQ, ExchangeCode.NAS),
        Map.entry(MarketType.NYSE, ExchangeCode.NYS),
        Map.entry(MarketType.AMEX, ExchangeCode.AMS),
        Map.entry(MarketType.SHANGHAI, ExchangeCode.SHS),
        Map.entry(MarketType.SHANGHAI_INDEX, ExchangeCode.SHI),
        Map.entry(MarketType.SHENZHEN, ExchangeCode.SZS),
        Map.entry(MarketType.SHENZHEN_INDEX, ExchangeCode.SZI),
        Map.entry(MarketType.TOKYO, ExchangeCode.TSE),
        Map.entry(MarketType.HONGKONG, ExchangeCode.HKS),
        Map.entry(MarketType.HANOI, ExchangeCode.HNX),
        Map.entry(MarketType.HOCHIMINH, ExchangeCode.HSX)
    );

    /**
     * 전체 시장 마스터파일 다운로드/파싱
     */
    public List<KisMasterStock> downloadAllStocks() {
        List<KisMasterStock> allStocks = new ArrayList<>();
        String baseUrl = properties.getMaster().getBaseUrl();

        for (MarketType market : MarketType.values()) {
            try {
                if (market.isDomestic()) {
                    String url = baseUrl + "/" + market.getMasterFileCode() + ".mst.zip";
                    allStocks.addAll(downloadAndParseDomestic(url, market));
                } else {
                    String url = baseUrl + "/" + market.getMasterFileCode() + "mst.cod.zip";
                    ExchangeCode exchange = OVERSEAS_EXCHANGE_MAP.get(market);
                    allStocks.addAll(downloadAndParseOverseas(url, market, exchange));
                }
            } catch (Exception e) {
                log.warn("마스터파일 다운로드 실패 ({}): {}", market, e.getMessage());
            }
        }

        log.info("KIS 마스터파일 로드 완료: 총 {}개 종목", allStocks.size());
        return allStocks;
    }

    private List<KisMasterStock> downloadAndParseDomestic(String url, MarketType marketType) {
        Path tempZip = null;
        try {
            tempZip = downloadZip(url);
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tempZip))) {
                if (zis.getNextEntry() == null) return List.of();
                return domesticParser.parse(zis, marketType);
            }
        } catch (Exception e) {
            throw new KisApiException("국내 마스터파일 처리 실패 (" + marketType + "): " + e.getMessage(), e);
        } finally {
            deleteTempFile(tempZip);
        }
    }

    private List<KisMasterStock> downloadAndParseOverseas(String url, MarketType marketType,
                                                           ExchangeCode exchangeCode) {
        Path tempZip = null;
        try {
            tempZip = downloadZip(url);
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(tempZip))) {
                if (zis.getNextEntry() == null) return List.of();
                return overseasParser.parse(zis, marketType, exchangeCode);
            }
        } catch (Exception e) {
            throw new KisApiException("해외 마스터파일 처리 실패 (" + marketType + "): " + e.getMessage(), e);
        } finally {
            deleteTempFile(tempZip);
        }
    }

    private Path downloadZip(String url) throws IOException, InterruptedException {
        Path tempZip = Files.createTempFile("kis_master_", ".zip");
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        client.send(request, HttpResponse.BodyHandlers.ofFile(tempZip));
        return tempZip;
    }

    private void deleteTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("임시 파일 삭제 실패: {}", path);
            }
        }
    }
}