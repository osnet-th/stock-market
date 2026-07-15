package com.thlee.stock.market.stockmarket.companyreport.domain.model;

import java.util.List;

/**
 * 리포트 정성 입력(수동) — 항목별 구조화 입력 묶음 (JSON 컬럼으로 저장).
 * 리스트 항목은 사용자가 행 단위로 추가/삭제한다. 모든 항목 선택 입력.
 */
public record ReportManual(
        int schemaVersion,
        List<HistoryItem> history,
        String philosophyNote,
        List<PartnerItem> customers,
        List<PartnerItem> suppliers,
        String partnerAssessment,
        List<CompetitorItem> competitors,
        String performanceNote,
        List<FinancialChangeItem> financialChanges,
        List<ShareholderEventItem> shareholderEvents,
        String shareholderNote,
        String judgmentComment,
        List<RevenueForecast> revenueForecasts,
        String amountUnit,
        MetricInputs metricInputs
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    /** 자유 메모 길이 상한 */
    public static final int TEXT_MAX_LENGTH = 10000;

    /** 행 항목 개별 필드 길이 상한 */
    public static final int FIELD_MAX_LENGTH = 500;

    /** 행 개수 상한 (보안/저장 보호) */
    public static final int LIST_MAX_SIZE = 100;

    /** 회사 연혁: 연도별 사건 */
    public record HistoryItem(String year, String content) {}

    /** 판매처/매입처: 업체 + 비중 + 비고 */
    public record PartnerItem(String name, String share, String note) {}

    /** 경쟁사 비교: 개별 사업부문끼리 비교 */
    public record CompetitorItem(String name, String segment, String note) {}

    /** 재무제표 급변 항목: 연도 + 항목 + 원인 */
    public record FinancialChangeItem(String year, String item, String reason) {}

    /** 주주 이벤트: 시기 + 내용 (자사주 소각, 대주주 변동 등) */
    public record ShareholderEventItem(String period, String content) {}

    /** 예상 실적: 연도 + 분기별 예상 매출(q1~q4) + 분기별 예상 순이익(ni1~ni4, 선택) — 금액 단위는 amountUnit */
    public record RevenueForecast(String year, String q1, String q2, String q3, String q4,
                                  String ni1, String ni2, String ni3, String ni4) {}

    /** 주가지표 계산기 재료 (선택 입력, 비우면 자동 산출값 사용). 주당 값은 원, 금액은 amountUnit */
    public record MetricInputs(String price, String shares, String eps, String bps, String revenue, String operatingCf) {}

    public static ReportManual empty() {
        return new ReportManual(CURRENT_SCHEMA_VERSION,
                List.of(), null, List.of(), List.of(), null,
                List.of(), null, List.of(), List.of(), null, null,
                List.of(), null, null);
    }

    /**
     * 길이/개수 상한 검증 (위반 시 IllegalArgumentException)
     */
    public void validate() {
        requireTextWithin(philosophyNote, "경영이념·사업내용");
        requireTextWithin(partnerAssessment, "매입처/판매처 분산 평가");
        requireTextWithin(performanceNote, "실적 추이 메모");
        requireTextWithin(shareholderNote, "주주 동향 메모");
        requireTextWithin(judgmentComment, "투자판단 코멘트");
        validateLists();
    }

    private void validateLists() {
        requireSizeWithin(history, "연혁");
        requireSizeWithin(revenueForecasts, "예상 매출");
        requireSizeWithin(customers, "판매처");
        requireSizeWithin(suppliers, "매입처");
        requireSizeWithin(competitors, "경쟁사 비교");
        requireSizeWithin(financialChanges, "급변 항목");
        requireSizeWithin(shareholderEvents, "주주 이벤트");
        validateListFields();
    }

    private void validateListFields() {
        ifPresent(history, item -> requireYear(item.year(), "연혁"));
        ifPresent(financialChanges, item -> requireYear(item.year(), "급변 항목"));
        ifPresent(revenueForecasts, item -> requireYear(item.year(), "예상 매출"));
        ifPresent(revenueForecasts, item -> requireAmounts("예상 매출",
                item.q1(), item.q2(), item.q3(), item.q4()));
        ifPresent(revenueForecasts, item -> requireAmounts("예상 순이익",
                item.ni1(), item.ni2(), item.ni3(), item.ni4()));
        validateUnitAndMetricInputs();
        ifPresent(history, item -> requireFieldsWithin("연혁", item.year(), item.content()));
        ifPresent(customers, item -> requireFieldsWithin("판매처", item.name(), item.share(), item.note()));
        ifPresent(suppliers, item -> requireFieldsWithin("매입처", item.name(), item.share(), item.note()));
        ifPresent(competitors, item -> requireFieldsWithin("경쟁사 비교", item.name(), item.segment(), item.note()));
        ifPresent(financialChanges, item -> requireFieldsWithin("급변 항목", item.year(), item.item(), item.reason()));
        ifPresent(shareholderEvents, item -> requireFieldsWithin("주주 이벤트", item.period(), item.content()));
    }

    private static <T> void ifPresent(List<T> items, java.util.function.Consumer<T> validator) {
        if (items != null) {
            items.forEach(validator);
        }
    }

    private static void requireTextWithin(String text, String label) {
        if (text != null && text.length() > TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException(label + "은(는) " + TEXT_MAX_LENGTH + "자 이하여야 합니다.");
        }
    }

    private static void requireSizeWithin(List<?> items, String label) {
        if (items != null && items.size() > LIST_MAX_SIZE) {
            throw new IllegalArgumentException(label + " 행은 " + LIST_MAX_SIZE + "개 이하여야 합니다.");
        }
    }

    private void validateUnitAndMetricInputs() {
        if (amountUnit != null && !amountUnit.isBlank()
                && !"억".equals(amountUnit) && !"조".equals(amountUnit)) {
            throw new IllegalArgumentException("금액 단위는 억 또는 조여야 합니다: " + amountUnit);
        }
        if (metricInputs != null) {
            requireAmounts("주가지표 재료", metricInputs.price(), metricInputs.shares(),
                    metricInputs.eps(), metricInputs.bps(), metricInputs.revenue(), metricInputs.operatingCf());
        }
    }

    /** 금액/수량 입력: 숫자(소수점 허용)만 허용 */
    private static void requireAmounts(String label, String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !value.matches("\\d{1,15}(\\.\\d{1,6})?")) {
                throw new IllegalArgumentException(label + " 금액은 숫자여야 합니다: " + value);
            }
        }
    }

    private static void requireYear(String year, String label) {
        if (year != null && !year.isBlank() && !year.matches("\\d{4}")) {
            throw new IllegalArgumentException(label + " 연도는 4자리 숫자여야 합니다: " + year);
        }
    }

    private static void requireFieldsWithin(String label, String... values) {
        for (String value : values) {
            if (value != null && value.length() > FIELD_MAX_LENGTH) {
                throw new IllegalArgumentException(label + " 항목 값은 " + FIELD_MAX_LENGTH + "자 이하여야 합니다.");
            }
        }
    }
}
