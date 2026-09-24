package com.thlee.stock.market.stockmarket.companyreport.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

/** 누락은 임시저장에서 허용하되 입력된 값의 형식/범위는 항상 검증한다. */
public final class SrimValidator {
    private SrimValidator() {}

    public static void validate(SrimInput input, boolean draft) {
        if (input == null) throw new IllegalArgumentException("S-RIM 입력이 필요합니다.");
        validateCommon(input, draft);
        if (input.years().size() > 30) fail("예상 연도는 최대 30개입니다.");
        if (!draft && input.years().isEmpty()) fail("예상 연도를 추가하세요.");
        validateYears(input, draft);
    }

    private static void validateCommon(SrimInput i, boolean draft) {
        positive(i.equity(), "지배주주지분", draft);
        positive(i.shares(), "총 주식수", draft);
        validateShares(i.shares());
        positiveRate(i.requiredReturn(), draft);
        validateDates(i, draft);
        validateCurrency(i.currency());
        positive(i.referencePrice(), "비교 기준 주가", true);
    }

    private static void validateDates(SrimInput i, boolean draft) {
        date(i.equityDate(), "자본 기준일", draft);
        date(i.sharesDate(), "주식수 기준일", draft);
        date(i.referencePriceDate(), "주가 기준일", draft || i.referencePrice() == null);
    }

    private static void validateCurrency(String currency) {
        if (!"KRW".equals(currency) && !"USD".equals(currency)) fail("통화는 KRW 또는 USD여야 합니다.");
    }

    private static void validateShares(BigDecimal value) {
        if (value != null && value.stripTrailingZeros().scale() > 0) fail("총 주식수는 정수여야 합니다.");
    }

    private static void positiveRate(BigDecimal value, boolean draft) {
        number(value, "요구수익률", 6, 12);
        if (value == null && !draft) fail("요구수익률을 입력하세요.");
        if (value != null && value.signum() <= 0) fail("요구수익률은 0보다 커야 합니다.");
    }

    private static void validateYears(SrimInput input, boolean draft) {
        Set<Integer> years = new HashSet<>();
        for (SrimInput.Year row : input.years()) {
            validateYear(row, draft);
            if (row.year() != null && !years.add(row.year())) fail("기준 연도가 중복되었습니다.");
        }
    }

    private static void validateYear(SrimInput.Year row, boolean draft) {
        if (row == null) fail("연도 행이 올바르지 않습니다.");
        if (row.year() == null && !draft) fail("기준 연도를 입력하세요.");
        if (row.year() != null && (row.year() < 1900 || row.year() > 2200)) fail("연도는 1900~2200 사이입니다.");
        if (!"DIRECT".equals(row.mode()) && !"CALCULATED".equals(row.mode())) fail("ROE 입력 방식을 선택하세요.");
        validateRowNumbers(row);
        if (!draft) requireRowValues(row);
        validateAverage(row);
    }

    private static void validateRowNumbers(SrimInput.Year row) {
        number(row.directRoe(), "예상 ROE", 6, 12);
        number(row.previousEquity(), "전기말 지분", 24, 8);
        number(row.expectedEquity(), "당기말 지분", 24, 8);
        number(row.expectedIncome(), "예상 순이익", 24, 8);
    }

    private static void requireRowValues(SrimInput.Year row) {
        if ("DIRECT".equals(row.mode()) && row.directRoe() == null) fail("예상 ROE를 입력하세요.");
        if ("CALCULATED".equals(row.mode()) && !hasCalculationValues(row)) fail("ROE 계산 근거 세 값을 입력하세요.");
    }

    public static boolean hasCalculationValues(SrimInput.Year row) {
        return row.previousEquity() != null && row.expectedEquity() != null && row.expectedIncome() != null;
    }

    private static void validateAverage(SrimInput.Year row) {
        if (!"CALCULATED".equals(row.mode()) || !hasCalculationValues(row)) return;
        if (row.previousEquity().add(row.expectedEquity()).signum() <= 0) fail("평균 지배주주지분은 0보다 커야 합니다.");
    }

    private static void positive(BigDecimal value, String label, boolean optional) {
        number(value, label, 24, 8);
        if (value == null && !optional) fail(label + "을 입력하세요.");
        if (value != null && value.signum() <= 0) fail(label + "은 0보다 커야 합니다.");
    }

    private static void number(BigDecimal value, String label, int integerDigits, int fractionDigits) {
        if (value == null) return;
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.precision() - stripped.scale() > integerDigits || stripped.scale() > fractionDigits)
            fail(label + "의 자릿수가 허용 범위를 초과합니다.");
    }

    private static void date(String value, String label, boolean optional) {
        if (value == null || value.isBlank()) {
            if (!optional) fail(label + "을 입력하세요.");
            return;
        }
        parseDate(value, label);
    }

    private static void parseDate(String value, String label) {
        try {
            if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) fail(label + "은 YYYY-MM-DD 형식이어야 합니다.");
            LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            fail(label + "이 올바르지 않습니다.");
        }
    }

    private static void fail(String message) { throw new IllegalArgumentException(message); }
}
