package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.companyreport.application.CompanyReportReadService;
import com.thlee.stock.market.stockmarket.companyreport.application.SrimService;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.CompanyReportResults;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.SrimData;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.SrimInputData;
import com.thlee.stock.market.stockmarket.economics.application.BondYieldQueryService;
import com.thlee.stock.market.stockmarket.economics.domain.exception.BondYieldFetchException;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpCompanyReportDetail;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpCompanyReportSummary;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpSrimResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 기업 리포트·S-RIM 조회를 Claude 응답 형태로 묶는다. 트랜잭션은 기존 서비스의 것을 쓴다.
 */
@Service
@RequiredArgsConstructor
public class McpCompanyReportQueryService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    static final int MAX_PAGE_SIZE = 50;

    private final CompanyReportReadService companyReportReadService;
    private final SrimService srimService;
    private final BondYieldQueryService bondYieldQueryService;
    private final McpSrimInputAssembler srimInputAssembler;

    public McpCompanyReportSummary summary(String stockCode) {
        CompanyReportResults.Preview preview = companyReportReadService.preview(stockCode);
        return McpCompanyReportMapper.toSummary(preview.snapshot(), preview.valuation());
    }

    public CompanyReportResults.ListResult list(Long userId, String stockName, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return companyReportReadService.findList(userId, stockName, safePage, safeSize);
    }

    public McpCompanyReportDetail detail(Long reportId, Long userId) {
        CompanyReportResults.Detail d = companyReportReadService.findDetail(reportId, userId);
        McpCompanyReportSummary summary = d.snapshot() == null ? null
                : McpCompanyReportMapper.toSummary(d.snapshot(), d.valuation());
        return new McpCompanyReportDetail(d.id(), d.stockCode(), d.stockName(), d.grades(), d.hasBuySignal(),
                d.draft(), d.manual(), summary, d.srim(), d.snapshotAt(), d.updatedAt());
    }

    public McpSrimResult srim(String stockCode, BigDecimal requiredReturnPercent, BigDecimal roePercent) {
        ReportSnapshot snapshot = companyReportReadService.preview(stockCode).snapshot();
        BondYieldLookup bondYields = requiredReturnPercent == null ? todayBondYields() : null;
        McpSrimInputAssembler.Assembly assembly = srimInputAssembler.assemble(
                snapshot, requiredReturnPercent, roePercent, bondYields, LocalDate.now(KST).getYear());
        if (assembly.input() == null) {
            return McpSrimResult.notCalculable(snapshot.stockCode(), snapshot.stockName(), assembly.failureReason());
        }
        SrimData data = srimService.preview(assembly.input());
        SrimData.YearResult year = data.results().isEmpty() ? null : data.results().get(0);
        SrimInputData input = assembly.input();
        return new McpSrimResult(snapshot.stockCode(), snapshot.stockName(), true, null, input.currency(),
                input.equity(), input.equityDate(), snapshot.srimBasis().equityReport(),
                input.shares(), input.sharesDate(), snapshot.srimBasis().sharesReport(),
                assembly.requiredReturnPercent(), assembly.requiredReturnSource(),
                assembly.roePercent(), assembly.roeSource(),
                input.referencePrice(), input.referencePriceDate(),
                year == null ? null : year.year(),
                year == null ? null : scenario(year.perpetual()),
                year == null ? null : scenario(year.decline10()),
                year == null ? null : scenario(year.decline20()));
    }

    /** 금리 출처 통신 실패면 null을 돌려 요구수익률 입력을 요구하게 한다 */
    private BondYieldLookup todayBondYields() {
        try {
            return bondYieldQueryService.lookup(null);
        } catch (BondYieldFetchException e) {
            return null;
        }
    }

    private static McpSrimResult.Scenario scenario(SrimData.Scenario s) {
        return s == null ? null : new McpSrimResult.Scenario(s.price(), s.differencePercent());
    }
}
