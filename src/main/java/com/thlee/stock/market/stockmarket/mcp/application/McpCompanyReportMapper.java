package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportValuation;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpCompanyReportSummary;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 기업 리포트 스냅샷 → Claude 응답용 요약. 최근 5개 연도만 남긴다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class McpCompanyReportMapper {

    static final int RECENT_YEARS = 5;
    static final String RATIO_UNIT = "%";

    public static McpCompanyReportSummary toSummary(ReportSnapshot snapshot, ReportValuation valuation) {
        List<ReportSnapshot.ColumnMeta> columns = recentColumns(snapshot.columns());
        Set<String> years = columns.stream().map(ReportSnapshot.ColumnMeta::year).collect(Collectors.toSet());
        return new McpCompanyReportSummary(
                snapshot.stockCode(),
                snapshot.stockName(),
                snapshot.country(),
                snapshot.currency(),
                baseYear(snapshot.columns()),
                RATIO_UNIT,
                columns.stream().map(c -> new McpCompanyReportSummary.Column(c.year(), c.reportLabel(), c.partial())).toList(),
                priceMetrics(snapshot.priceMetrics()),
                snapshot.ratios() == null ? List.of() : snapshot.ratios().stream()
                        .map(row -> new McpCompanyReportSummary.Ratio(row.key(), row.name(), row.category(),
                                onlyYears(row.values(), years), row.judgment()))
                        .toList(),
                riskSignals(snapshot.riskSignals()),
                valuation(valuation),
                snapshot.srimBasis() != null && snapshot.srimBasis().equity() != null
                        && snapshot.srimBasis().shares() != null,
                snapshot.unsupportedSections());
    }

    private static List<ReportSnapshot.ColumnMeta> recentColumns(List<ReportSnapshot.ColumnMeta> columns) {
        if (columns == null) {
            return List.of();
        }
        List<ReportSnapshot.ColumnMeta> sorted = columns.stream()
                .sorted(Comparator.comparing(ReportSnapshot.ColumnMeta::year))
                .toList();
        return sorted.subList(Math.max(0, sorted.size() - RECENT_YEARS), sorted.size());
    }

    /** 부분 연도를 뺀 최근 연도 */
    private static String baseYear(List<ReportSnapshot.ColumnMeta> columns) {
        if (columns == null) {
            return null;
        }
        return columns.stream()
                .filter(c -> !c.partial())
                .map(ReportSnapshot.ColumnMeta::year)
                .max(String::compareTo)
                .orElse(null);
    }

    private static Map<String, String> onlyYears(Map<String, String> values, Set<String> years) {
        Map<String, String> filtered = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((year, value) -> {
                if (years.contains(year)) {
                    filtered.put(year, value);
                }
            });
        }
        return filtered;
    }

    private static McpCompanyReportSummary.PriceMetrics priceMetrics(ReportSnapshot.PriceMetrics p) {
        if (p == null) {
            return null;
        }
        return new McpCompanyReportSummary.PriceMetrics(p.termName(), p.referencePriceDate(), p.referencePrice(),
                p.marketCap(), p.eps(), p.bps(), p.per(), p.pbr(), p.psr(), p.pcfr(), p.evEbitda(), p.roic(),
                p.warnings());
    }

    private static McpCompanyReportSummary.RiskSignals riskSignals(ReportSnapshot.RiskSignals r) {
        if (r == null) {
            return null;
        }
        return new McpCompanyReportSummary.RiskSignals(r.currentLiabilitiesExceedCurrentAssets(), r.lowEquityRatio(),
                r.negativeEquity(), r.receivablesSurge(), r.inventorySurge(), r.recentCapitalIncrease(), r.evidence());
    }

    private static McpCompanyReportSummary.Valuation valuation(ReportValuation v) {
        if (v == null || (v.liquidation() == null && v.dcf() == null)) {
            return null;
        }
        ReportValuation.LiquidationValuation l = v.liquidation();
        ReportValuation.DcfValuation d = v.dcf();
        return new McpCompanyReportSummary.Valuation(
                l == null ? null : l.baseYear(),
                l == null ? null : l.liquidationValue(),
                l == null ? null : l.marketCapToLiquidation(),
                d != null && d.calculable(),
                d == null ? null : d.reason(),
                d == null ? null : d.conservativeValue(),
                d == null ? null : d.optimisticValue(),
                l != null ? l.marketCap() : d == null ? null : d.marketCap(),
                d == null ? null : d.marketCapToConservative(),
                d == null ? null : d.marketCapToOptimistic());
    }
}
