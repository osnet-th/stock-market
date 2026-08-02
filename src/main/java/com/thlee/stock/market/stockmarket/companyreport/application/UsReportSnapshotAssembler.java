package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.CompanyProfileData;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.Shareholders;
import com.thlee.stock.market.stockmarket.stock.domain.model.CachedStockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.Stock;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsCompanyFacts;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsCompanyProfile;
import com.thlee.stock.market.stockmarket.stock.domain.service.SecFinancialPort;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPort;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 미국(US/SEC) 리포트 스냅샷 조립.
 * companyfacts 연간 팩트(10년) + submissions 프로필 + KIS 해외 현재가를 조합한다.
 * 재무 팩트 조달 실패는 전체 실패로 전파하고(리포트 불가), 프로필·현재가 실패는 null로 대체해 부분 스냅샷을 허용한다.
 * 최대주주·5%룰 대량보유는 미국에 구조화 소스가 없어 unsupportedSections로 명시한다 (배당은 제공).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsReportSnapshotAssembler implements ReportSnapshotAssembler {

    private static final int TIMELINE_YEARS = 10;
    private static final Pattern TICKER_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9.\\-]{0,9}$");
    private static final List<String> UNSUPPORTED_SECTIONS =
            List.of("shareholders.major", "shareholders.bulkHoldings");

    private final SecFinancialPort secFinancialPort;
    private final StockPort stockPort;
    private final StockPricePort stockPricePort;
    private final UsSnapshotFinancialExtractor extractor;

    @Override
    public boolean supports(String stockCode) {
        return stockCode != null && TICKER_PATTERN.matcher(stockCode).matches();
    }

    @Override
    public ReportSnapshot assemble(String stockCode) {
        String ticker = stockCode.toUpperCase();
        UsCompanyFacts facts = secFinancialPort.getAnnualReportFacts(ticker, TIMELINE_YEARS);
        String baseYear = extractor.latestAnnualYear(facts);
        UsCompanyProfile profile = fetchProfileSafely(ticker);
        PriceInfo price = fetchPriceSafely(ticker, profile);
        return new ReportSnapshot(
                ReportSnapshot.CURRENT_SCHEMA_VERSION,
                ticker,
                resolveStockName(ticker, profile),
                ReportSnapshot.COUNTRY_US,
                ReportSnapshot.CURRENCY_USD,
                UNSUPPORTED_SECTIONS,
                "CFS",
                toProfileData(profile),
                extractor.columns(facts),
                extractor.performanceRows(facts),
                extractor.statementRows(facts),
                extractor.ratioRows(facts, baseYear),
                extractor.priceMetrics(facts, baseYear, price.price(), price.date()),
                extractor.valuationInputs(facts, baseYear),
                new Shareholders(List.of(), List.of(), extractor.dividendRows(facts, baseYear), null),
                extractor.riskSignals(facts, baseYear));
    }

    private UsCompanyProfile fetchProfileSafely(String ticker) {
        try {
            return secFinancialPort.getCompanyProfile(ticker);
        } catch (Exception e) {
            log.warn("US 스냅샷 부가 데이터 조회 실패 [기업개황]: {}", e.getMessage());
            return null;
        }
    }

    private String resolveStockName(String ticker, UsCompanyProfile profile) {
        if (profile != null && profile.name() != null && !profile.name().isBlank()) {
            return profile.name();
        }
        return ticker;
    }

    /**
     * SEC 미제공 필드(CEO명·설립일)는 null — 프론트는 "-" 표시
     */
    private CompanyProfileData toProfileData(UsCompanyProfile profile) {
        if (profile == null) {
            return null;
        }
        return new CompanyProfileData(
                profile.name(), profile.name(), profile.ticker(),
                null, profile.address(), profile.website(),
                industryLabel(profile), null, settlementMonth(profile.fiscalYearEnd()));
    }

    private String industryLabel(UsCompanyProfile profile) {
        if (profile.sicCode() == null) {
            return profile.sicDescription();
        }
        return profile.sicDescription() == null ? profile.sicCode()
                : profile.sicCode() + " " + profile.sicDescription();
    }

    /** SEC fiscalYearEnd는 MMDD — 결산월(MM)만 취한다 (KR 표기와 동일 자리) */
    private String settlementMonth(String fiscalYearEnd) {
        if (fiscalYearEnd == null || fiscalYearEnd.length() < 2) {
            return null;
        }
        return fiscalYearEnd.substring(0, 2);
    }

    // === 현재가 (KIS 해외) ===

    private PriceInfo fetchPriceSafely(String ticker, UsCompanyProfile profile) {
        try {
            Listing listing = resolveListing(ticker, profile);
            if (listing == null) {
                log.warn("US 스냅샷 현재가 생략 [거래소 미확인]: {}", ticker);
                return PriceInfo.empty();
            }
            CachedStockPrice cached = stockPricePort.getPriceWithCacheInfo(
                    ticker, listing.marketType(), listing.exchangeCode());
            BigDecimal price = parsePrice(cached.stockPrice().currentPrice());
            String date = LocalDate.ofInstant(cached.cachedAt(), ZoneId.systemDefault()).toString();
            return new PriceInfo(price, date);
        } catch (Exception e) {
            log.warn("US 스냅샷 부가 데이터 조회 실패 [현재가]: {}", e.getMessage());
            return PriceInfo.empty();
        }
    }

    /** 거래소 해석: KIS 종목 마스터 우선, 없으면 SEC 프로필의 거래소명 매핑 */
    private Listing resolveListing(String ticker, UsCompanyProfile profile) {
        Stock stock = stockPort.findByCode(ticker).orElse(null);
        if (stock != null && stock.marketType() != null && !stock.marketType().isDomestic()) {
            return new Listing(stock.marketType(), stock.exchangeCode());
        }
        return mapSecExchange(profile == null ? null : profile.exchange());
    }

    private Listing mapSecExchange(String exchange) {
        if (exchange == null) {
            return null;
        }
        String normalized = exchange.toUpperCase();
        if (normalized.contains("NASDAQ")) {
            return new Listing(MarketType.NASDAQ, ExchangeCode.NAS);
        }
        if (normalized.contains("NYSE")) {
            return new Listing(MarketType.NYSE, ExchangeCode.NYS);
        }
        if (normalized.contains("AMEX") || normalized.contains("AMERICAN")) {
            return new Listing(MarketType.AMEX, ExchangeCode.AMS);
        }
        return null;
    }

    private BigDecimal parsePrice(String currentPrice) {
        if (currentPrice == null || currentPrice.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(currentPrice.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record Listing(MarketType marketType, ExchangeCode exchangeCode) {}

    private record PriceInfo(BigDecimal price, String date) {
        static PriceInfo empty() {
            return new PriceInfo(null, null);
        }
    }
}
