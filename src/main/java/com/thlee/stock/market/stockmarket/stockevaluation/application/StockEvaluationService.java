package com.thlee.stock.market.stockmarket.stockevaluation.application;

import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.CreditEligibilityResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.EstimatePerformResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.KisTableResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.StockBasicInfoResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.StockSummaryResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisSearchStockInfoOutput;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.FinanceStatementType;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.KsdScheduleType;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.KisFinanceClient;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.KisKsdScheduleClient;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.KisStockInfoClient;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisEstimatePerformResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisSearchInfoOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 종목 평가 서비스.
 * KIS 국내주식 종목정보 API를 호출하고 결과를 응답 DTO로 변환한다.
 * DB 저장 없이 온디맨드 pass-through 방식.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockEvaluationService {

    private final KisStockInfoClient kisStockInfoClient;
    private final KisFinanceClient kisFinanceClient;
    private final KisKsdScheduleClient kisKsdScheduleClient;

    // 종목추정실적 행 라벨 (KIS 응답이 data1~N 위치값만 제공 → 실제값 대조로 확정한 표준 레이아웃 기준)
    private static final List<String> ESTIMATE_INCOME_LABELS = List.of(
        "매출액(억원)", "매출액 증감률(%)", "영업이익(억원)", "영업이익 증감률(%)",
        "당기순이익(억원)", "당기순이익 증감률(%)");
    // output3은 EPS(2번째 행)만 실제값으로 확정, 나머지는 포털 명세 확인 필요
    private static final List<String> ESTIMATE_METRIC_LABELS = List.of(
        "지표1", "EPS(원)", "지표3", "지표4", "지표5", "지표6", "지표7", "지표8");

    private static LinkedHashMap<String, String> estimateBasicLabels() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("sht_cd", "종목코드");
        m.put("item_kor_nm", "종목명");
        m.put("name1", "애널리스트");
        m.put("estdate", "추정일자");
        m.put("rcmd_name", "투자의견");
        m.put("capital", "자본금(억원)");
        m.put("forn_item_lmtrt", "외국인한도율");
        return m;
    }

    /**
     * 종목 기본정보 조회 (상품기본조회).
     */
    public StockBasicInfoResponse getBasicInfo(String stockCode) {
        try {
            KisSearchInfoOutput output = kisStockInfoClient.searchInfo(stockCode);
            if (output == null) {
                throw new KisApiException("종목 기본정보를 불러올 수 없습니다");
            }

            return StockBasicInfoResponse.builder()
                .stockCode(stockCode)
                .productTypeCode(output.getProductTypeCode())
                .productTypeName(productTypeName(output.getProductTypeCode()))
                .productClassCode(output.getProductClassCode())
                .saleStatusCode(output.getSaleStatusCode())
                .saleStartDate(formatDate(output.getSaleStartDate()))
                .firstRegisterDate(formatDate(output.getFirstRegisterDate()))
                .build();
        } catch (KisApiException e) {
            log.error("종목 기본정보 조회 실패 [{}]: {}", stockCode, e.getMessage());
            throw new KisApiException("종목 기본정보를 불러올 수 없습니다");
        }
    }

    /**
     * 종목 요약 조회 (주식기본조회) — 평가 화면 최상단 카드용.
     */
    public StockSummaryResponse getStockSummary(String stockCode) {
        try {
            KisSearchStockInfoOutput o = kisStockInfoClient.searchStockInfo(stockCode);
            if (o == null) {
                throw new KisApiException("종목 요약 정보를 불러올 수 없습니다");
            }
            List<StockSummaryResponse.SummaryItem> details = new ArrayList<>();
            addItem(details, "상장주식수", withSuffix(formatNumber(o.getListedShares()), "주"));
            addItem(details, "자본금", withSuffix(formatNumber(o.getCapital()), "원"));
            addItem(details, "액면가", withSuffix(formatNumber(o.getParValue()), "원"));
            addItem(details, "결산월", formatSettlementMonth(o.getSettlementMonthDay()));
            addItem(details, "상장일", formatDate(listingDate(o)));
            addItem(details, "외국인한도", foreignLimit(o.getForeignLimitRate()));

            String name = (o.getAbbrName() != null && !o.getAbbrName().isBlank()) ? o.getAbbrName() : o.getName();
            return StockSummaryResponse.builder()
                .stockCode(stockCode)
                .stockName(name)
                .englishName(o.getEngAbbrName())
                .stdCode(o.getStdCode())
                .market(market(o))
                .industry(o.getIndustryName())
                .closePrice(withSuffix(formatNumber(o.getClosePrice()), "원"))
                .prevClosePrice(withSuffix(formatNumber(o.getPrevClosePrice()), "원"))
                .kospi200("Y".equals(o.getKospi200Yn()))
                .managed("Y".equals(o.getManagedYn()))
                .tradingHalted("Y".equals(o.getTradingStopYn()))
                .details(details)
                .build();
        } catch (KisApiException e) {
            log.error("종목 요약 조회 실패 [{}]: {}", stockCode, e.getMessage());
            throw new KisApiException("종목 요약 정보를 불러올 수 없습니다");
        }
    }

    private void addItem(List<StockSummaryResponse.SummaryItem> items, String label, String value) {
        items.add(StockSummaryResponse.SummaryItem.builder()
            .label(label)
            .value(value == null || value.isBlank() ? "-" : value)
            .build());
    }

    private String market(KisSearchStockInfoOutput o) {
        if ("STK".equals(o.getMarketIdCode())) return "코스피";
        if ("KSQ".equals(o.getMarketIdCode())) return "코스닥";
        if (o.getKosdaqListingDate() != null && !o.getKosdaqListingDate().isBlank()) return "코스닥";
        if (o.getKospiListingDate() != null && !o.getKospiListingDate().isBlank()) return "코스피";
        return "기타";
    }

    private String listingDate(KisSearchStockInfoOutput o) {
        if (o.getKosdaqListingDate() != null && !o.getKosdaqListingDate().isBlank()) {
            return o.getKosdaqListingDate();
        }
        return o.getKospiListingDate();
    }

    private String formatSettlementMonth(String mmdd) {
        if (mmdd == null || mmdd.isBlank()) return "";
        String mm = mmdd.length() >= 2 ? mmdd.substring(0, 2) : mmdd;
        try {
            return Integer.parseInt(mm) + "월";
        } catch (NumberFormatException e) {
            return mmdd;
        }
    }

    private String foreignLimit(String rate) {
        if (rate == null || rate.isBlank()) return "";
        try {
            double r = Double.parseDouble(rate);
            return r <= 0 ? "한도없음" : String.format("%.2f%%", r);
        } catch (NumberFormatException e) {
            return rate;
        }
    }

    /** KIS가 ×10로 인코딩한 값(증감률·EPS)을 실제 값으로 정규화. 표시 콤마는 프론트가 처리하므로 raw 숫자 반환. */
    private String scaleBy10(String v) {
        if (v == null || v.isBlank()) return v;
        String s = v.trim();
        if (!s.matches("-?\\d+(\\.\\d+)?")) return v;
        try {
            // 소수 1자리 유지(EPS 등 정수도 프론트 콤마가 적용되도록). 표시 형식은 프론트 담당.
            return new BigDecimal(s).divide(BigDecimal.TEN).toPlainString();
        } catch (ArithmeticException e) {
            return v;
        }
    }

    private String formatNumber(String num) {
        if (num == null || num.isBlank()) return "";
        try {
            return String.format("%,d", Long.parseLong(num.trim()));
        } catch (NumberFormatException e) {
            try {
                return String.format("%,.0f", Double.parseDouble(num.trim()));
            } catch (NumberFormatException e2) {
                return num;
            }
        }
    }

    private String withSuffix(String value, String suffix) {
        return (value == null || value.isBlank()) ? "" : value + suffix;
    }

    /**
     * 재무 항목 조회 (대차대조표/손익계산서/각종 비율).
     */
    public KisTableResponse getFinanceStatement(String stockCode, FinanceStatementType type, String divCls) {
        try {
            List<Map<String, String>> rows = kisFinanceClient.fetch(type, stockCode, divCls);
            // 값은 raw로 반환 (콤마 등 표시 형식은 프론트에서 처리)
            return KisTableResponse.from(rows, type.getLabels());
        } catch (KisApiException e) {
            log.error("재무 조회 실패 [{}:{}]: {}", type, stockCode, e.getMessage());
            throw new KisApiException("재무 정보를 불러올 수 없습니다");
        }
    }

    /**
     * 종목추정실적 조회.
     * output4(기간축: 결산년월)를 컬럼 헤더로, output2/3의 위치값(data1~N)을 항목 라벨과 함께 표로 구성한다.
     */
    public EstimatePerformResponse getEstimatePerform(String stockCode) {
        try {
            KisEstimatePerformResponse res = kisStockInfoClient.estimatePerform(stockCode);
            List<String> periods = extractPeriods(res.getOutput4());
            List<EstimatePerformResponse.Section> sections = new ArrayList<>();

            if (res.getOutput1() != null && !res.getOutput1().isEmpty()) {
                sections.add(section("기본정보", basicInfoTable(res.getOutput1())));
            }
            if (res.getOutput2() != null && !res.getOutput2().isEmpty()) {
                sections.add(section("추정 손익", metricTable(ESTIMATE_INCOME_LABELS, res.getOutput2(), periods)));
            }
            if (res.getOutput3() != null && !res.getOutput3().isEmpty()) {
                sections.add(section("추정 투자지표", metricTable(ESTIMATE_METRIC_LABELS, res.getOutput3(), periods)));
            }
            return EstimatePerformResponse.builder().sections(sections).build();
        } catch (KisApiException e) {
            log.error("종목추정실적 조회 실패 [{}]: {}", stockCode, e.getMessage());
            throw new KisApiException("종목추정실적을 불러올 수 없습니다");
        }
    }

    private EstimatePerformResponse.Section section(String title, KisTableResponse table) {
        return EstimatePerformResponse.Section.builder().title(title).table(table).build();
    }

    /** output4(dt 목록) → 기간 컬럼 라벨. */
    private List<String> extractPeriods(List<Map<String, String>> output4) {
        List<String> periods = new ArrayList<>();
        if (output4 != null) {
            for (Map<String, String> m : output4) {
                periods.add(m.getOrDefault("dt", ""));
            }
        }
        return periods;
    }

    /** 기본정보(단일 객체) → "항목 / 값" 2열 표. */
    private KisTableResponse basicInfoTable(Map<String, String> output1) {
        List<List<String>> rows = new ArrayList<>();
        estimateBasicLabels().forEach((key, label) -> {
            if (output1.containsKey(key)) {
                rows.add(List.of(label, output1.getOrDefault(key, "")));
            }
        });
        return KisTableResponse.builder().columns(List.of("항목", "값")).rows(rows).build();
    }

    /** output2/3(행=항목, data1~N=기간) → "항목 + 기간별 값" 표. */
    private KisTableResponse metricTable(List<String> labels, List<Map<String, String>> rows, List<String> periods) {
        List<String> columns = new ArrayList<>();
        columns.add("항목");
        columns.addAll(periods);
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> rec = rows.get(i);
            String label = i < labels.size() ? labels.get(i) : ("항목 " + (i + 1));
            boolean scaled = label.contains("증감률") || label.startsWith("EPS"); // KIS가 ×10로 인코딩한 확정 항목만 정규화
            List<String> row = new ArrayList<>();
            row.add(label);
            for (int j = 0; j < periods.size(); j++) {
                String raw = rec.getOrDefault("data" + (j + 1), "");
                row.add(scaled ? scaleBy10(raw) : raw); // 금액은 raw, 표시 콤마는 프론트
            }
            out.add(row);
        }
        return KisTableResponse.builder().columns(columns).rows(out).build();
    }

    /**
     * 신용거래 가능 여부 조회.
     * 당사 신용가능종목(전체 목록)에서 종목코드 포함 여부로 판정한다.
     */
    public CreditEligibilityResponse getCreditEligibility(String stockCode) {
        try {
            List<Map<String, String>> list = kisStockInfoClient.creditEligibleList();
            if (list != null) {
                for (Map<String, String> row : list) {
                    if (stockCode.equals(row.get("stck_shrn_iscd"))) {
                        return CreditEligibilityResponse.builder()
                            .stockCode(stockCode)
                            .eligible(true)
                            .creditRate(row.getOrDefault("crdt_rate", ""))
                            .build();
                    }
                }
            }
            return CreditEligibilityResponse.builder()
                .stockCode(stockCode)
                .eligible(false)
                .creditRate("")
                .build();
        } catch (KisApiException e) {
            log.error("신용가능종목 조회 실패 [{}]: {}", stockCode, e.getMessage());
            throw new KisApiException("신용거래 가능 여부를 불러올 수 없습니다");
        }
    }

    /**
     * 예탁원 일정 조회 (배당/증자/합병 등 12종).
     */
    public KisTableResponse getSchedule(String stockCode, KsdScheduleType type, String fromDate, String toDate) {
        try {
            List<Map<String, String>> rows = kisKsdScheduleClient.fetch(type, stockCode, fromDate, toDate);
            return KisTableResponse.from(rows, type.getLabels());
        } catch (KisApiException e) {
            log.error("예탁원 일정 조회 실패 [{}:{}]: {}", type, stockCode, e.getMessage());
            throw new KisApiException("일정 정보를 불러올 수 없습니다");
        }
    }

    private String productTypeName(String code) {
        if ("300".equals(code)) {
            return "주식";
        }
        return code;
    }

    /**
     * KIS 날짜(YYYYMMDD) → "YYYY-MM-DD" 변환.
     */
    private String formatDate(String date) {
        if (date == null || date.length() < 8) {
            return "";
        }
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
    }
}
