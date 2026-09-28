package com.thlee.stock.market.stockmarket.stock.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.stock.domain.model.PeriodicReport;
import com.thlee.stock.market.stockmarket.stock.domain.model.ReportCode;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockFinancialPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 주식총수 현황 조회 기준 보고서 선택.
 * 분기보고서는 주식총수 값이 비어 있으므로 사업·반기보고서만 후보로 두고,
 * (사업연도, 기간 종료월) 쌍으로 가장 최신인 보고서를 고른다. 후보가 없거나 공시 조회가 실패하면 기준연도 사업보고서로 폴백한다.
 * 리포트 조립·주가지표·챗봇이 같은 종목을 연달아 조회하므로 종목별 정기보고서 목록을 짧게 캐시한다 (조회 실패는 캐시하지 않음).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShareReportSelector {

    private static final String PERIODIC_DISCLOSURE_TYPE = "A";
    /** 전년 반기·전전년 사업보고서까지 후보에 들도록 2년 전 1월 1일부터 조회 */
    private static final int LOOKBACK_YEARS = 2;

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final int CACHE_MAX_SIZE = 1_000;

    private final StockFinancialPort stockFinancialPort;
    private final Cache<String, List<PeriodicReport>> reportsCache = Caffeine.newBuilder()
            .expireAfterWrite(CACHE_TTL)
            .maximumSize(CACHE_MAX_SIZE)
            .build();

    /**
     * 공시 목록을 조회해 기준 보고서를 고른다. 공시 조회 실패는 폴백으로 흡수한다.
     *
     * @return 선택된 보고서. 폴백할 기준연도도 없으면 null
     */
    public PeriodicReport latest(String stockCode, String fallbackYear) {
        try {
            return select(reportsCache.get(stockCode, this::fetchPeriodicReports), fallbackYear);
        } catch (Exception e) {
            log.warn("주식총수 기준 보고서 조회 실패, 사업보고서로 폴백: stockCode={}, {}", stockCode, e.getMessage());
            return fallback(fallbackYear);
        }
    }

    public PeriodicReport select(List<PeriodicReport> reports, String fallbackYear) {
        return reports.stream()
                .filter(report -> report.reportCode() == ReportCode.ANNUAL
                        || report.reportCode() == ReportCode.SEMI_ANNUAL)
                .max(Comparator.comparingInt(PeriodicReport::year).thenComparingInt(PeriodicReport::periodEndMonth))
                .orElseGet(() -> fallback(fallbackYear));
    }

    private List<PeriodicReport> fetchPeriodicReports(String stockCode) {
        LocalDate today = LocalDate.now();
        String from = today.minusYears(LOOKBACK_YEARS).withDayOfYear(1).format(DateTimeFormatter.BASIC_ISO_DATE);
        return stockFinancialPort.getDisclosures(stockCode, from, today.format(DateTimeFormatter.BASIC_ISO_DATE),
                        PERIODIC_DISCLOSURE_TYPE).stream()
                .map(disclosure -> PeriodicReport.parse(disclosure.reportName()))
                .flatMap(Optional::stream)
                .toList();
    }

    private PeriodicReport fallback(String fallbackYear) {
        if (fallbackYear == null || !fallbackYear.matches("\\d{4}")) {
            return null;
        }
        return new PeriodicReport(Integer.parseInt(fallbackYear), ReportCode.ANNUAL);
    }
}
