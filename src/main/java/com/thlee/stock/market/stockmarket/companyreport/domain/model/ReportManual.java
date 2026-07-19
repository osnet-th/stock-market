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

    // v2: MetricInputs에 당기순이익·자본총계 추가(EPS·BPS 공식형), PartnerItem에 품목 추가. eps·bps는 v1 폴백용 유지.
    public static final int CURRENT_SCHEMA_VERSION = 2;

    /** 자유 메모 길이 상한 */
    public static final int TEXT_MAX_LENGTH = 10000;

    /** 행 항목 개별 필드 길이 상한 */
    public static final int FIELD_MAX_LENGTH = 500;

    /** 행 개수 상한 (보안/저장 보호) */
    public static final int LIST_MAX_SIZE = 100;

    /** 회사 연혁: 연도별 사건 */
    public record HistoryItem(String year, String content) {}

    /** 판매처/매입처: 업체 + 품목(판매처=제품·매입처=원자재) + 비중 + 비고 */
    public record PartnerItem(String name, String item, String share, String note) {}

    /** 경쟁사 비교: 개별 사업부문끼리 비교 */
    public record CompetitorItem(String name, String segment, String note) {}

    /** 재무제표 급변 항목: 연도 + 항목 + 원인 */
    public record FinancialChangeItem(String year, String item, String reason) {}

    /** 주주 이벤트: 시기 + 내용 (자사주 소각, 대주주 변동 등) */
    public record ShareholderEventItem(String period, String content) {}

    /** 예상 실적: 연도 + 분기별 예상 매출(q1~q4) + 분기별 예상 순이익(ni1~ni4, 선택) — 금액 단위는 amountUnit */
    public record RevenueForecast(String year, String q1, String q2, String q3, String q4,
                                  String ni1, String ni2, String ni3, String ni4) {}

    /**
     * 주가지표 계산기 재료 (선택 입력, 비우면 자동 산출값 사용).
     * netIncome(당기순이익)·equity(자본총계)·revenue·operatingCf는 amountUnit, price·shares는 원/주.
     * EPS·BPS는 분자(당기순이익·자본총계) ÷ 유통주식수로 산출한다. eps·bps는 구버전 직접입력 폴백용(신규 UI 미노출).
     */
    public record MetricInputs(String price, String shares, String netIncome, String equity,
                               String revenue, String operatingCf, String eps, String bps) {}

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
        ifPresent(history, item -> requireYearOrMonth(item.year(), "연혁"));
        ifPresent(financialChanges, item -> requireYear(item.year(), "급변 항목"));
        ifPresent(revenueForecasts, item -> requireYear(item.year(), "예상 매출"));
        ifPresent(revenueForecasts, item -> requireAmounts("예상 매출",
                item.q1(), item.q2(), item.q3(), item.q4()));
        ifPresent(revenueForecasts, item -> requireAmounts("예상 순이익",
                item.ni1(), item.ni2(), item.ni3(), item.ni4()));
        validateUnitAndMetricInputs();
        ifPresent(history, item -> requireFieldsWithin("연혁", item.year(), item.content()));
        ifPresent(customers, row -> requireFieldsWithin("판매처", row.name(), row.item(), row.share(), row.note()));
        ifPresent(suppliers, row -> requireFieldsWithin("매입처", row.name(), row.item(), row.share(), row.note()));
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
            // 당기순이익·자본총계는 적자·자본잠식(음수) 허용
            requireSignedAmounts("주가지표 재료", metricInputs.netIncome(), metricInputs.equity());
        }
    }

    /** 금액/수량 입력: 숫자(소수점 허용, 음수 불가) */
    private static void requireAmounts(String label, String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !value.matches("\\d{1,15}(\\.\\d{1,6})?")) {
                throw new IllegalArgumentException(label + " 금액은 숫자여야 합니다: " + value);
            }
        }
    }

    /** 손익/자본 입력: 음수(적자·자본잠식) 허용 숫자 */
    private static void requireSignedAmounts(String label, String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !value.matches("-?\\d{1,15}(\\.\\d{1,6})?")) {
                throw new IllegalArgumentException(label + " 금액은 숫자여야 합니다: " + value);
            }
        }
    }

    private static void requireYear(String year, String label) {
        if (year != null && !year.isBlank() && !year.matches("\\d{4}")) {
            throw new IllegalArgumentException(label + " 연도는 4자리 숫자여야 합니다: " + year);
        }
    }

    /** 연혁 날짜: YYYY 또는 YYYY.MM(월 1~12) 허용 */
    private static void requireYearOrMonth(String value, String label) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!value.matches("\\d{4}(\\.\\d{1,2})?")) {
            throw new IllegalArgumentException(label + " 날짜는 YYYY 또는 YYYY.MM 형식이어야 합니다: " + value);
        }
        int dot = value.indexOf('.');
        if (dot >= 0) {
            int month = Integer.parseInt(value.substring(dot + 1));
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException(label + " 월은 1~12 사이여야 합니다: " + value);
            }
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
