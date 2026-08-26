package com.thlee.stock.market.stockmarket.stock.presentation;

import com.thlee.stock.market.stockmarket.stock.application.DisclosureQueryService;
import com.thlee.stock.market.stockmarket.stock.application.FinancialTimelineService;
import com.thlee.stock.market.stockmarket.stock.application.StockFinancialService;
import com.thlee.stock.market.stockmarket.stock.application.dto.*;
import com.thlee.stock.market.stockmarket.stock.domain.model.IndexClassCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.PublicationType;
import com.thlee.stock.market.stockmarket.stock.domain.model.ReportCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.TimelineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockFinancialController {

    private final StockFinancialService stockFinancialService;
    private final FinancialTimelineService financialTimelineService;
    private final DisclosureQueryService disclosureQueryService;

    // === 단일회사 API ===

    @GetMapping("/{stockCode}/financial/accounts")
    public ResponseEntity<List<FinancialAccountResponse>> getFinancialAccounts(
            @PathVariable String stockCode,
            @RequestParam String year,
            @RequestParam ReportCode reportCode) {
        return ResponseEntity.ok(
                stockFinancialService.getFinancialAccounts(stockCode, year, reportCode.getCode()));
    }

    @GetMapping("/{stockCode}/financial/indices")
    public ResponseEntity<List<FinancialIndexResponse>> getFinancialIndices(
            @PathVariable String stockCode,
            @RequestParam String year,
            @RequestParam ReportCode reportCode,
            @RequestParam IndexClassCode indexClassCode) {
        return ResponseEntity.ok(
                stockFinancialService.getFinancialIndices(
                        stockCode, year, reportCode.getCode(), indexClassCode.getCode()));
    }

    @GetMapping("/{stockCode}/financial/full-statements")
    public ResponseEntity<List<FullFinancialStatementResponse>> getFullFinancialStatements(
            @PathVariable String stockCode,
            @RequestParam String year,
            @RequestParam ReportCode reportCode,
            @RequestParam String fsDiv) {
        return ResponseEntity.ok(
                stockFinancialService.getFullFinancialStatements(stockCode, year, reportCode.getCode(), fsDiv));
    }

    @GetMapping("/{stockCode}/financial/stock-quantities")
    public ResponseEntity<List<StockQuantityResponse>> getStockQuantities(
            @PathVariable String stockCode,
            @RequestParam String year,
            @RequestParam ReportCode reportCode) {
        return ResponseEntity.ok(
                stockFinancialService.getStockQuantities(stockCode, year, reportCode.getCode()));
    }

    @GetMapping("/{stockCode}/financial/dividends")
    public ResponseEntity<List<DividendInfoResponse>> getDividendInfos(
            @PathVariable String stockCode,
            @RequestParam String year,
            @RequestParam ReportCode reportCode) {
        return ResponseEntity.ok(
                stockFinancialService.getDividendInfos(stockCode, year, reportCode.getCode()));
    }

    @GetMapping("/{stockCode}/financial/lawsuits")
    public ResponseEntity<List<LawsuitResponse>> getLawsuits(
            @PathVariable String stockCode,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(stockFinancialService.getLawsuits(stockCode, startDate, endDate));
    }

    @GetMapping("/{stockCode}/financial/private-fund-usages")
    public ResponseEntity<List<FundUsageResponse>> getPrivateFundUsages(
            @PathVariable String stockCode,
            @RequestParam String year,
            @RequestParam ReportCode reportCode) {
        return ResponseEntity.ok(
                stockFinancialService.getPrivateFundUsages(stockCode, year, reportCode.getCode()));
    }

    @GetMapping("/{stockCode}/financial/public-fund-usages")
    public ResponseEntity<List<FundUsageResponse>> getPublicFundUsages(
            @PathVariable String stockCode,
            @RequestParam String year,
            @RequestParam ReportCode reportCode) {
        return ResponseEntity.ok(
                stockFinancialService.getPublicFundUsages(stockCode, year, reportCode.getCode()));
    }

    // === 타임라인 API ===

    @GetMapping("/{stockCode}/financial/timeline")
    public ResponseEntity<FinancialTimelineResponse> getFinancialTimeline(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "5") int years,
            @RequestParam(defaultValue = "CFS") String fsDiv,
            @RequestParam(required = false) Set<TimelineItem> items) {
        return ResponseEntity.ok(financialTimelineService.getTimeline(stockCode, years, fsDiv, items));
    }

    // === 공시 목록 API ===

    @GetMapping("/{stockCode}/disclosures")
    public ResponseEntity<List<DisclosureResponse>> getDisclosures(
            @PathVariable String stockCode,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) List<PublicationType> types) {
        return ResponseEntity.ok(
                disclosureQueryService.getDisclosures(stockCode, fromDate, toDate, types));
    }

    // === 옵션 API ===

    @GetMapping("/financial/options")
    public ResponseEntity<FinancialOptionsResponse> getFinancialOptions() {
        return ResponseEntity.ok(FinancialOptionsResponse.create());
    }

    // === 다중회사 API ===

    @GetMapping("/financial/accounts")
    public ResponseEntity<List<FinancialAccountResponse>> getMultiFinancialAccounts(
            @RequestParam List<String> stockCodes,
            @RequestParam String year,
            @RequestParam ReportCode reportCode) {
        return ResponseEntity.ok(
                stockFinancialService.getMultiFinancialAccounts(stockCodes, year, reportCode.getCode()));
    }

    @GetMapping("/financial/indices")
    public ResponseEntity<List<FinancialIndexResponse>> getMultiFinancialIndices(
            @RequestParam List<String> stockCodes,
            @RequestParam String year,
            @RequestParam ReportCode reportCode,
            @RequestParam IndexClassCode indexClassCode) {
        return ResponseEntity.ok(
                stockFinancialService.getMultiFinancialIndices(
                        stockCodes, year, reportCode.getCode(), indexClassCode.getCode()));
    }
}