package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.BulkHoldingReportResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.CompanyProfileResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.MajorShareholderResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.MajorShareholder;
import com.thlee.stock.market.stockmarket.stock.domain.model.ReportCode;
import com.thlee.stock.market.stockmarket.stock.domain.service.CompanyDisclosurePort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config.FinancialTimelineExecutorConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

/**
 * 기업개황/지분공시 조회 유스케이스.
 * 최대주주 이력은 사업연도별 사업보고서를 병렬 조회해 합친다(실패 연도는 skip).
 */
@Service
public class CompanyInfoService {

    private final CompanyDisclosurePort companyDisclosurePort;
    private final Executor executor;

    public CompanyInfoService(
            CompanyDisclosurePort companyDisclosurePort,
            @Qualifier(FinancialTimelineExecutorConfig.EXECUTOR_NAME) Executor executor) {
        this.companyDisclosurePort = companyDisclosurePort;
        this.executor = executor;
    }

    public CompanyProfileResponse getCompanyProfile(String stockCode) {
        return companyDisclosurePort.getCompanyProfile(stockCode)
                .map(CompanyProfileResponse::from)
                .orElse(null);
    }

    /**
     * 최근 N개 사업연도의 최대주주 소유 현황 (연도 오름차순)
     */
    public List<MajorShareholderResponse> getMajorShareholderHistory(String stockCode, int years) {
        int latestYear = LocalDate.now().getYear() - 1;
        List<CompletableFuture<List<MajorShareholder>>> futures = IntStream.range(0, years)
                .mapToObj(offset -> fetchYearSafely(stockCode, latestYear - offset))
                .toList();
        return mergeSortedByYear(futures);
    }

    public List<BulkHoldingReportResponse> getBulkHoldingReports(String stockCode) {
        return companyDisclosurePort.getBulkHoldingReports(stockCode).stream()
                .map(BulkHoldingReportResponse::from)
                .toList();
    }

    private CompletableFuture<List<MajorShareholder>> fetchYearSafely(String stockCode, int year) {
        return CompletableFuture.supplyAsync(
                        () -> companyDisclosurePort.getMajorShareholders(
                                stockCode, String.valueOf(year), ReportCode.ANNUAL.getCode()),
                        executor)
                .exceptionally(e -> List.of());
    }

    private List<MajorShareholderResponse> mergeSortedByYear(
            List<CompletableFuture<List<MajorShareholder>>> futures) {
        return futures.stream()
                .flatMap(future -> future.join().stream())
                .sorted(Comparator.comparing(MajorShareholder::bsnsYear))
                .map(MajorShareholderResponse::from)
                .toList();
    }
}
