package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialIndexResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineColumn;
import com.thlee.stock.market.stockmarket.stock.application.dto.TimelineColumnData;
import com.thlee.stock.market.stockmarket.stock.domain.model.*;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockFinancialPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config.FinancialTimelineExecutorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 다년 재무 타임라인 조립 유스케이스.
 * 과거 연도는 사업보고서, 사업보고서가 없는 연도(올해 등)는 공시검색으로 탐지한
 * 최신 정기보고서(진행중)를 기준 컬럼으로 쓴다.
 */
@Slf4j
@Service
public class FinancialTimelineService {

    private static final String PERIODIC_DISCLOSURE_TYPE = "A";
    private static final int MIN_YEARS = 1;
    private static final int MAX_YEARS = 10;

    private final StockFinancialPort stockFinancialPort;
    private final StockFinancialService stockFinancialService;
    private final FinancialTimelineAssembler assembler;
    private final Executor timelineExecutor;

    public FinancialTimelineService(
            StockFinancialPort stockFinancialPort,
            StockFinancialService stockFinancialService,
            FinancialTimelineAssembler assembler,
            @Qualifier(FinancialTimelineExecutorConfig.EXECUTOR_NAME) Executor timelineExecutor) {
        this.stockFinancialPort = stockFinancialPort;
        this.stockFinancialService = stockFinancialService;
        this.assembler = assembler;
        this.timelineExecutor = timelineExecutor;
    }

    public FinancialTimelineResponse getTimeline(String stockCode, int years, String fsDiv, Set<TimelineItem> items) {
        validate(stockCode, years, fsDiv);
        Set<TimelineItem> targets = normalizeItems(items);
        List<TimelineColumn> columns = resolveColumns(stockCode, years);
        List<TimelineColumnData> data = fetchColumns(stockCode, columns, fsDiv, targets);
        return assembler.assemble(data, fsDiv, targets);
    }

    // === 입력 검증 ===

    private void validate(String stockCode, int years, String fsDiv) {
        if (stockCode == null || !stockCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("종목코드는 6자리 숫자여야 합니다: " + stockCode);
        }
        if (years < MIN_YEARS || years > MAX_YEARS) {
            throw new IllegalArgumentException("조회 연수는 1~10 사이여야 합니다: " + years);
        }
        validateFsDiv(fsDiv);
    }

    private void validateFsDiv(String fsDiv) {
        if (!"CFS".equals(fsDiv) && !"OFS".equals(fsDiv)) {
            throw new IllegalArgumentException("fsDiv는 CFS 또는 OFS만 가능합니다: " + fsDiv);
        }
    }

    private Set<TimelineItem> normalizeItems(Set<TimelineItem> items) {
        if (items == null || items.isEmpty()) {
            return EnumSet.allOf(TimelineItem.class);
        }
        return EnumSet.copyOf(items);
    }

    // === 컬럼(연도별 기준 보고서) 결정 ===

    private List<TimelineColumn> resolveColumns(String stockCode, int years) {
        int currentYear = Year.now().getValue();
        int firstYear = currentYear - years + 1;
        Map<Integer, List<PeriodicReport>> reportsByYear = fetchReportsByYear(stockCode, firstYear);
        return buildColumns(reportsByYear, firstYear, currentYear);
    }

    /**
     * 정기공시(A) 목록 1콜로 대상 기간의 전체 정기보고서를 사업연도별로 그룹핑한다.
     * (사업연도 Y의 사업보고서는 Y+1년 초에 접수되므로 조회 종료일은 오늘)
     */
    private Map<Integer, List<PeriodicReport>> fetchReportsByYear(String stockCode, int firstYear) {
        List<Disclosure> disclosures = stockFinancialPort.getDisclosures(
                stockCode, firstYear + "0101", today(), PERIODIC_DISCLOSURE_TYPE);
        return disclosures.stream()
                .map(disclosure -> PeriodicReport.parse(disclosure.reportName()))
                .flatMap(Optional::stream)
                .distinct()
                .collect(Collectors.groupingBy(PeriodicReport::year));
    }

    private String today() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    private List<TimelineColumn> buildColumns(
            Map<Integer, List<PeriodicReport>> reportsByYear, int firstYear, int currentYear) {
        List<TimelineColumn> columns = new ArrayList<>();
        for (int year = firstYear; year <= currentYear; year++) {
            selectReport(reportsByYear.get(year)).map(this::toColumn).ifPresent(columns::add);
        }
        return columns;
    }

    /**
     * 같은 연도에 여러 정기보고서가 있으면 보고 기간이 가장 늦은 것(사업 > 3분기 > 반기 > 1분기)을 쓴다
     */
    private Optional<PeriodicReport> selectReport(List<PeriodicReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return Optional.empty();
        }
        return reports.stream().max(Comparator.comparingInt(PeriodicReport::periodEndMonth));
    }

    private TimelineColumn toColumn(PeriodicReport report) {
        return new TimelineColumn(
                String.valueOf(report.year()),
                report.reportCode().getCode(),
                report.reportCode().getLabel(),
                !report.isAnnual());
    }

    // === 연도별 병렬 수집 ===

    private List<TimelineColumnData> fetchColumns(
            String stockCode, List<TimelineColumn> columns, String fsDiv, Set<TimelineItem> items) {
        List<CompletableFuture<TimelineColumnData>> futures = columns.stream()
                .map(column -> fetchColumnAsync(stockCode, column, fsDiv, items))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private CompletableFuture<TimelineColumnData> fetchColumnAsync(
            String stockCode, TimelineColumn column, String fsDiv, Set<TimelineItem> items) {
        var accounts = supplyIf(items.contains(TimelineItem.ACCOUNTS),
                () -> stockFinancialPort.getFinancialAccounts(stockCode, column.getYear(), column.getReportCode()));
        var indices = fetchIndicesAsync(stockCode, column, items);
        var shares = supplyIf(items.contains(TimelineItem.SHARES),
                () -> stockFinancialPort.getStockQuantities(stockCode, column.getYear(), column.getReportCode()));
        var details = supplyIf(needsDetails(items),
                () -> stockFinancialPort.getFullFinancialStatements(
                        stockCode, column.getYear(), column.getReportCode(), fsDiv));
        return combine(column, accounts, indices, shares, details);
    }

    private CompletableFuture<TimelineColumnData> combine(TimelineColumn column,
            CompletableFuture<List<FinancialAccount>> accounts,
            CompletableFuture<List<FinancialIndexResponse>> indices,
            CompletableFuture<List<StockQuantity>> shares,
            CompletableFuture<List<FullFinancialStatement>> details) {
        return CompletableFuture.allOf(accounts, indices, shares, details)
                .thenApply(unused -> new TimelineColumnData(
                        column, accounts.join(), indices.join(), shares.join(), details.join()));
    }

    /**
     * 재무지표는 분류당 1콜이라 4분류를 각각 병렬 호출 후 평탄화한다.
     * 기존 서비스 메서드를 경유해 지표 캐시(@Cacheable)를 재사용한다.
     */
    private CompletableFuture<List<FinancialIndexResponse>> fetchIndicesAsync(
            String stockCode, TimelineColumn column, Set<TimelineItem> items) {
        if (!items.contains(TimelineItem.INDICES)) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<CompletableFuture<List<FinancialIndexResponse>>> futures = Arrays.stream(IndexClassCode.values())
                .map(classCode -> supply(() -> stockFinancialService.getFinancialIndices(
                        stockCode, column.getYear(), column.getReportCode(), classCode.getCode())))
                .toList();
        return flatten(futures);
    }

    private <T> CompletableFuture<List<T>> flatten(List<CompletableFuture<List<T>>> futures) {
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(unused -> futures.stream().flatMap(future -> future.join().stream()).toList());
    }

    private boolean needsDetails(Set<TimelineItem> items) {
        return items.contains(TimelineItem.DETAILS) || items.contains(TimelineItem.FCF);
    }

    private <T> CompletableFuture<List<T>> supplyIf(boolean condition, Supplier<List<T>> supplier) {
        if (!condition) {
            return CompletableFuture.completedFuture(List.of());
        }
        return supply(supplier);
    }

    /**
     * 개별 항목 실패는 빈 목록으로 대체해 타임라인 전체 실패를 막는다 (해당 연도 셀만 비게 됨)
     */
    private <T> CompletableFuture<List<T>> supply(Supplier<List<T>> supplier) {
        return CompletableFuture.supplyAsync(supplier, timelineExecutor)
                .exceptionally(this::logAndReturnEmpty);
    }

    private <T> List<T> logAndReturnEmpty(Throwable e) {
        log.warn("타임라인 항목 조회 실패: {}", e.getMessage());
        return List.of();
    }
}
