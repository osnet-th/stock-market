package com.thlee.stock.market.stockmarket.stock.domain.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 정기보고서 참조 (사업연도 + 보고서 구분).
 * 공시 보고서명("사업보고서 (2025.12)" 등)에서 파싱한다.
 * 정정공시의 "[기재정정]" 등 접두어를 허용하도록 포함-문자열 기반으로 매칭한다.
 */
public record PeriodicReport(int year, ReportCode reportCode) {

    private static final Pattern REPORT_NAME_PATTERN =
            Pattern.compile("(사업|반기|분기)보고서\\s*\\((\\d{4})\\.(\\d{2})\\)");

    public static Optional<PeriodicReport> parse(String reportName) {
        if (reportName == null) {
            return Optional.empty();
        }
        Matcher matcher = REPORT_NAME_PATTERN.matcher(reportName);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return toReportCode(matcher.group(1), matcher.group(3))
                .map(code -> new PeriodicReport(Integer.parseInt(matcher.group(2)), code));
    }

    private static Optional<ReportCode> toReportCode(String type, String month) {
        return switch (type + month) {
            case "사업12" -> Optional.of(ReportCode.ANNUAL);
            case "반기06" -> Optional.of(ReportCode.SEMI_ANNUAL);
            case "분기03" -> Optional.of(ReportCode.Q1);
            case "분기09" -> Optional.of(ReportCode.Q3);
            default -> Optional.empty();
        };
    }

    public boolean isAnnual() {
        return reportCode == ReportCode.ANNUAL;
    }

    /**
     * 보고 기간 종료 월 — 같은 연도 내 최신 보고서 선택 기준
     */
    public int periodEndMonth() {
        return switch (reportCode) {
            case ANNUAL -> 12;
            case Q3 -> 9;
            case SEMI_ANNUAL -> 6;
            case Q1 -> 3;
        };
    }
}
