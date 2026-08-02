package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.BulkHoldingRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.CompanyProfileData;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.ColumnMeta;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.DividendRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.ShareholderRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.Shareholders;
import com.thlee.stock.market.stockmarket.stock.application.CompanyInfoService;
import com.thlee.stock.market.stockmarket.stock.application.DisclosureQueryService;
import com.thlee.stock.market.stockmarket.stock.application.FinancialTimelineService;
import com.thlee.stock.market.stockmarket.stock.application.StockFinancialService;
import com.thlee.stock.market.stockmarket.stock.application.ValuationMetricService;
import com.thlee.stock.market.stockmarket.stock.application.dto.BulkHoldingReportResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.CompanyProfileResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.DisclosureResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.DividendInfoResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.MajorShareholderResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockQuantityResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.ValuationMetricResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.PublicationType;
import com.thlee.stock.market.stockmarket.stock.domain.model.ReportCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.TimelineItem;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config.FinancialTimelineExecutorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * 국내(KR/DART) 리포트 스냅샷 조립.
 * 타임라인(10년) + 기업개황/지분공시/주가지표/배당을 조합하며, 개별 항목 실패는 null/빈 값으로 대체해
 * 부분 스냅샷을 허용한다 (전체 실패 방지).
 */
@Slf4j
@Component
public class KrReportSnapshotAssembler implements ReportSnapshotAssembler {

    private static final int TIMELINE_YEARS = 10;
    private static final int SHAREHOLDER_YEARS = 10;
    private static final int CAPITAL_INCREASE_LOOKBACK_YEARS = 5;
    private static final int BULK_HOLDING_LIMIT = 50;
    private static final Set<TimelineItem> TIMELINE_ITEMS =
            EnumSet.of(TimelineItem.ACCOUNTS, TimelineItem.FCF, TimelineItem.DETAILS);

    private final FinancialTimelineService timelineService;
    private final StockFinancialService stockFinancialService;
    private final ValuationMetricService valuationMetricService;
    private final CompanyInfoService companyInfoService;
    private final DisclosureQueryService disclosureQueryService;
    private final SnapshotFinancialExtractor extractor;
    private final Executor executor;

    public KrReportSnapshotAssembler(
            FinancialTimelineService timelineService,
            StockFinancialService stockFinancialService,
            ValuationMetricService valuationMetricService,
            CompanyInfoService companyInfoService,
            DisclosureQueryService disclosureQueryService,
            SnapshotFinancialExtractor extractor,
            @Qualifier(FinancialTimelineExecutorConfig.EXECUTOR_NAME) Executor executor) {
        this.timelineService = timelineService;
        this.stockFinancialService = stockFinancialService;
        this.valuationMetricService = valuationMetricService;
        this.companyInfoService = companyInfoService;
        this.disclosureQueryService = disclosureQueryService;
        this.extractor = extractor;
        this.executor = executor;
    }

    @Override
    public boolean supports(String stockCode) {
        return stockCode != null && stockCode.matches("\\d{6}");
    }

    @Override
    public ReportSnapshot assemble(String stockCode) {
        TimelineResult timeline = fetchTimelineWithFallback(stockCode);
        String baseYear = extractor.latestAnnualYear(timeline.response());
        SideData side = fetchSideData(stockCode, baseYear);
        return buildSnapshot(stockCode, timeline, baseYear, side);
    }

    // === 타임라인 (연결 우선, 데이터 없으면 개별 폴백) ===

    private TimelineResult fetchTimelineWithFallback(String stockCode) {
        FinancialTimelineResponse consolidated =
                timelineService.getTimeline(stockCode, TIMELINE_YEARS, "CFS", TIMELINE_ITEMS);
        if (hasSummaryData(consolidated)) {
            return new TimelineResult("CFS", consolidated);
        }
        return new TimelineResult("OFS",
                timelineService.getTimeline(stockCode, TIMELINE_YEARS, "OFS", TIMELINE_ITEMS));
    }

    private boolean hasSummaryData(FinancialTimelineResponse timeline) {
        List<FinancialTimelineResponse.TimelineRow> rows = timeline.getSummaryAccounts();
        return rows != null && rows.stream().anyMatch(row -> !row.getValues().isEmpty());
    }

    private record TimelineResult(String fsDiv, FinancialTimelineResponse response) {}

    // === 부가 데이터 병렬 수집 ===

    /**
     * 최대주주 이력·증자이력 조회는 내부적으로 같은 executor에 하위 태스크를 제출·join하므로,
     * executor 스레드가 아닌 호출 스레드에서 실행한다 (중첩 join으로 인한 풀 고갈/데드락 방지).
     */
    private SideData fetchSideData(String stockCode, String baseYear) {
        var profile = supplySafely(() -> companyInfoService.getCompanyProfile(stockCode), "기업개황");
        var bulkHoldings = supplySafely(() -> companyInfoService.getBulkHoldingReports(stockCode), "대량보유");
        var valuation = supplySafely(() -> valuationMetricService.calculate(stockCode), "주가지표");
        var dividends = supplySafely(
                () -> stockFinancialService.getDividendInfos(stockCode, baseYear, ReportCode.ANNUAL.getCode()), "배당");
        var quantities = supplySafely(
                () -> stockFinancialService.getStockQuantities(stockCode, baseYear, ReportCode.ANNUAL.getCode()), "주식총수");
        List<MajorShareholderResponse> shareholders = fetchShareholdersSafely(stockCode);
        Boolean capitalIncrease = fetchCapitalIncreaseSafely(stockCode);
        return new SideData(profile.join(), shareholders, bulkHoldings.join(),
                valuation.join(), dividends.join(), quantities.join(), capitalIncrease);
    }

    private List<MajorShareholderResponse> fetchShareholdersSafely(String stockCode) {
        try {
            return companyInfoService.getMajorShareholderHistory(stockCode, SHAREHOLDER_YEARS);
        } catch (Exception e) {
            log.warn("스냅샷 부가 데이터 조회 실패 [최대주주]: {}", e.getMessage());
            return List.of();
        }
    }

    private Boolean fetchCapitalIncreaseSafely(String stockCode) {
        try {
            return hasCapitalIncrease(stockCode);
        } catch (Exception e) {
            log.warn("스냅샷 부가 데이터 조회 실패 [증자이력]: {}", e.getMessage());
            return null;
        }
    }

    private record SideData(
            CompanyProfileResponse profile,
            List<MajorShareholderResponse> shareholders,
            List<BulkHoldingReportResponse> bulkHoldings,
            ValuationMetricResponse valuation,
            List<DividendInfoResponse> dividends,
            List<StockQuantityResponse> quantities,
            Boolean capitalIncrease
    ) {}

    private <T> CompletableFuture<T> supplySafely(Supplier<T> supplier, String label) {
        return CompletableFuture.supplyAsync(supplier, executor)
                .exceptionally(e -> {
                    log.warn("스냅샷 부가 데이터 조회 실패 [{}]: {}", label, e.getMessage());
                    return null;
                });
    }

    /**
     * 최근 5년 주요사항보고(B) 중 보고서명에 "유상증자"가 있으면 true (최대 100건 조회 한계 내 근사)
     */
    private Boolean hasCapitalIncrease(String stockCode) {
        String from = LocalDate.now().minusYears(CAPITAL_INCREASE_LOOKBACK_YEARS)
                .format(DateTimeFormatter.BASIC_ISO_DATE);
        List<DisclosureResponse> disclosures =
                disclosureQueryService.getDisclosures(stockCode, from, null, List.of(PublicationType.B));
        return disclosures.stream()
                .anyMatch(d -> d.getReportName() != null && d.getReportName().contains("유상증자"));
    }

    // === 스냅샷 조립 ===

    private ReportSnapshot buildSnapshot(String stockCode, TimelineResult timeline, String baseYear, SideData side) {
        FinancialTimelineResponse response = timeline.response();
        return new ReportSnapshot(
                ReportSnapshot.CURRENT_SCHEMA_VERSION,
                stockCode,
                resolveStockName(stockCode, side.profile()),
                ReportSnapshot.COUNTRY_KR,
                ReportSnapshot.CURRENCY_KRW,
                List.of(),
                timeline.fsDiv(),
                toProfileData(side.profile()),
                toColumns(response),
                extractor.performanceRows(response),
                extractor.statementRows(response),
                extractor.ratioRows(response, baseYear),
                extractor.priceMetrics(response, baseYear, side.valuation(), side.quantities()),
                extractor.valuationInputs(response, baseYear),
                toShareholders(side),
                extractor.riskSignals(response, baseYear, side.capitalIncrease()));
    }

    private String resolveStockName(String stockCode, CompanyProfileResponse profile) {
        if (profile != null && profile.getStockName() != null && !profile.getStockName().isBlank()) {
            return profile.getStockName();
        }
        return stockCode;
    }

    private List<ColumnMeta> toColumns(FinancialTimelineResponse response) {
        return response.getColumns().stream()
                .map(column -> new ColumnMeta(column.getYear(), column.getReportLabel(), column.isPartial()))
                .toList();
    }

    private CompanyProfileData toProfileData(CompanyProfileResponse profile) {
        if (profile == null) {
            return null;
        }
        return new CompanyProfileData(
                profile.getCorpName(), profile.getCorpNameEng(), profile.getStockName(),
                profile.getCeoName(), profile.getAddress(), profile.getHomepageUrl(),
                profile.getIndustryCode(), profile.getEstablishedDate(), profile.getAccountSettlementMonth());
    }

    private Shareholders toShareholders(SideData side) {
        return new Shareholders(
                toShareholderRows(side.shareholders()),
                toBulkHoldingRows(side.bulkHoldings()),
                toDividendRows(side.dividends()),
                treasuryStockCount(side.quantities()));
    }

    private List<ShareholderRow> toShareholderRows(List<MajorShareholderResponse> shareholders) {
        if (shareholders == null) {
            return List.of();
        }
        return shareholders.stream()
                .map(s -> new ShareholderRow(s.getBsnsYear(), s.getStockKind(), s.getName(), s.getRelation(),
                        s.getBaseQuantity(), s.getBaseRatio(), s.getTermEndQuantity(), s.getTermEndRatio(),
                        s.getRemark()))
                .toList();
    }

    private List<BulkHoldingRow> toBulkHoldingRows(List<BulkHoldingReportResponse> reports) {
        if (reports == null) {
            return List.of();
        }
        return reports.stream()
                .sorted(Comparator.comparing(BulkHoldingReportResponse::getReceiptDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(BULK_HOLDING_LIMIT)
                .map(r -> new BulkHoldingRow(r.getReceiptNo(), r.getReceiptDate(), r.getReporter(),
                        r.getHoldingQuantity(), r.getQuantityChange(), r.getHoldingRatio(), r.getRatioChange(),
                        r.getReportReason()))
                .toList();
    }

    private List<DividendRow> toDividendRows(List<DividendInfoResponse> dividends) {
        if (dividends == null) {
            return List.of();
        }
        return dividends.stream()
                .map(d -> new DividendRow(d.getCategory(), d.getStockKind(), d.getCurrentTerm(),
                        d.getPreviousTerm(), d.getBeforePreviousTerm()))
                .toList();
    }

    private String treasuryStockCount(List<StockQuantityResponse> quantities) {
        if (quantities == null) {
            return null;
        }
        return quantities.stream()
                .filter(q -> q.getCategory() != null && q.getCategory().contains("보통주"))
                .map(StockQuantityResponse::getTreasuryStockCount)
                .findFirst()
                .orElse(null);
    }
}
