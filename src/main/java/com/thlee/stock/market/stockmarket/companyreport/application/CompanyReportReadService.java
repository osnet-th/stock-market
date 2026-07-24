package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.CompanyReportResults;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportValuation;
import com.thlee.stock.market.stockmarket.companyreport.domain.exception.CompanyReportNotFoundException;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.CompanyAnalysisReport;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;
import com.thlee.stock.market.stockmarket.companyreport.domain.repository.CompanyAnalysisReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 리포트 읽기 유스케이스.
 * 상세 조회 시 청산가치/DCF는 스냅샷 원시 입력 × 저장 파라미터로 파생 계산한다 (외부 API 무호출).
 */
@Service
@RequiredArgsConstructor
public class CompanyReportReadService {

    private final CompanyAnalysisReportRepository repository;
    private final CompanyReportSnapshotService snapshotService;
    private final ReportSnapshotJsonMapper snapshotJsonMapper;
    private final LiquidationValueCalculator liquidationCalculator;
    private final DcfCalculator dcfCalculator;
    private final GradeSuggestionCalculator gradeSuggestionCalculator;

    @Transactional(readOnly = true)
    public CompanyReportResults.ListResult findList(Long userId, String stockNameKeyword, int page, int size) {
        List<CompanyAnalysisReport> reports = repository.findPage(userId, stockNameKeyword, page, size);
        long totalCount = repository.count(userId, stockNameKeyword);
        return new CompanyReportResults.ListResult(
                reports.stream().map(this::toListItem).toList(), totalCount, page, size);
    }

    @Transactional(readOnly = true)
    public CompanyReportResults.Detail findDetail(Long id, Long userId) {
        CompanyAnalysisReport report = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CompanyReportNotFoundException(id));
        ReportSnapshot snapshot = snapshotJsonMapper.fromJson(report.getSnapshotJson());
        return toDetail(report, snapshot, computeValuation(snapshot, report.getValuationParams()));
    }

    /**
     * 저장 전 미리보기: 스냅샷 조립(외부 API 호출) + 기본 파라미터 가치평가. 트랜잭션 없음.
     */
    public CompanyReportResults.Preview preview(String stockCode) {
        ReportSnapshot snapshot = snapshotService.assemble(stockCode);
        ReportValuation valuation = computeValuation(snapshot, ValuationParams.defaults());
        return new CompanyReportResults.Preview(snapshot, valuation,
                gradeSuggestionCalculator.calculate(snapshot, valuation));
    }

    // === 내부 헬퍼 ===

    private CompanyReportResults.ListItem toListItem(CompanyAnalysisReport report) {
        return new CompanyReportResults.ListItem(
                report.getId(), report.getStockCode(), report.getStockName(),
                report.getGrades(), report.getGrades().hasBuySignal(),
                report.isDraft(), report.getDraftStep(),
                report.getSnapshotAt(), report.getUpdatedAt());
    }

    private CompanyReportResults.Detail toDetail(
            CompanyAnalysisReport report, ReportSnapshot snapshot, ReportValuation valuation) {
        return new CompanyReportResults.Detail(
                report.getId(), report.getStockCode(), report.getStockName(),
                report.getManual(), report.getGrades(), report.getGrades().hasBuySignal(),
                report.isDraft(), report.getDraftStep(),
                report.getValuationParams(), snapshot, valuation,
                gradeSuggestionCalculator.calculate(snapshot, valuation),
                report.getSnapshotAt(), report.getCreatedAt(), report.getUpdatedAt());
    }

    private ReportValuation computeValuation(ReportSnapshot snapshot, ValuationParams params) {
        if (snapshot == null || snapshot.valuationInputs() == null) {
            return new ReportValuation(params, null, null);
        }
        BigDecimal marketCap = snapshot.priceMetrics() != null ? snapshot.priceMetrics().marketCap() : null;
        return new ReportValuation(params,
                liquidationCalculator.calculate(snapshot.valuationInputs(), params.adjustmentRatios(), marketCap),
                dcfCalculator.calculate(snapshot.valuationInputs(), params, marketCap));
    }
}
