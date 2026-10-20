package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.SrimInputData;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondCreditGrade;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYield;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * 종목코드만으로 S-RIM 입력을 조립한다 (화면의 자동 채움과 같은 근거).
 * <ul>
 *   <li>자기자본·주식수·기준일: 리포트 스냅샷 srimBasis (국내 연결재무제표만 있다)</li>
 *   <li>기준 주가: 스냅샷 주가지표의 기준 주가</li>
 *   <li>요구수익률: 인자 → 없으면 회사채(공모 무보증) BBB- 5년 금리</li>
 *   <li>ROE: 인자 → 없으면 리포트 비율의 최근 연간(부분 연도 제외) ROE. 연도 행은 올해 1개를 직접 입력(DIRECT)으로 만든다</li>
 * </ul>
 * 금리·ROE는 % 값으로 받아 계산 입력에는 소수로 넣는다.
 */
@Component
public class McpSrimInputAssembler {

    static final int DEFAULT_RATE_MATURITY_MONTHS = 60;

    /**
     * 조립 결과. input이 null이면 failureReason에 이유가 있다.
     */
    public record Assembly(SrimInputData input, String failureReason,
                           BigDecimal requiredReturnPercent, String requiredReturnSource,
                           BigDecimal roePercent, String roeSource) {

        static Assembly failure(String reason) {
            return new Assembly(null, reason, null, null, null, null);
        }
    }

    public Assembly assemble(ReportSnapshot snapshot, BigDecimal requiredReturnPercent, BigDecimal roePercent,
                             BondYieldLookup bondYields, int currentYear) {
        ReportSnapshot.SrimBasis basis = snapshot.srimBasis();
        if (basis == null || basis.equity() == null || basis.shares() == null) {
            return Assembly.failure("자동 계산 불가: 이 종목은 S-RIM 근거(국내 연결재무제표의 지배주주지분·유통주식수)가 없습니다.");
        }

        BigDecimal rate = requiredReturnPercent;
        String rateSource = "입력값";
        if (rate == null) {
            Optional<BondYield> bbbMinus5y = defaultRate(bondYields);
            if (bbbMinus5y.isEmpty()) {
                return Assembly.failure("기본 요구수익률(회사채 BBB- 5년) 금리를 찾지 못했습니다. requiredReturnPercent를 넣어 주세요.");
            }
            rate = bbbMinus5y.get().rate();
            rateSource = "회사채 BBB- 5년 (기준일 " + bondYields.baseDate() + ", " + bondYields.source() + ")";
        }

        BigDecimal roe = roePercent;
        String roeSource = "입력값";
        if (roe == null) {
            Optional<Map.Entry<String, BigDecimal>> latest = latestAnnualRoe(snapshot);
            if (latest.isEmpty()) {
                return Assembly.failure("리포트에 연간 ROE가 없습니다. roePercent를 넣어 주세요.");
            }
            roe = latest.get().getValue();
            roeSource = "리포트 " + latest.get().getKey() + " 연간 ROE";
        }

        ReportSnapshot.PriceMetrics price = snapshot.priceMetrics();
        SrimInputData input = new SrimInputData(
                new BigDecimal(basis.equity()), basis.equityDate(),
                new BigDecimal(basis.shares()), basis.sharesDate(),
                rate.movePointLeft(2),
                snapshot.currency(),
                price == null ? null : price.referencePrice(),
                price == null ? null : price.referencePriceDate(),
                List.of(new SrimInputData.Year(currentYear, "DIRECT", roe.movePointLeft(2),
                        null, null, null)));
        return new Assembly(input, null, rate, rateSource, roe, roeSource);
    }

    private Optional<BondYield> defaultRate(BondYieldLookup lookup) {
        if (lookup == null || lookup.yields() == null) {
            return Optional.empty();
        }
        return lookup.yields().stream()
                .filter(y -> y.type() == BondYieldType.CORPORATE_PUBLIC_UNSECURED)
                .filter(y -> y.grade() == BondCreditGrade.BBB_MINUS)
                .filter(y -> y.maturityMonths() == DEFAULT_RATE_MATURITY_MONTHS)
                .filter(y -> y.rate() != null)
                .findFirst();
    }

    /** 부분 연도(분기·반기)를 뺀 최근 연도의 ROE(%) */
    private Optional<Map.Entry<String, BigDecimal>> latestAnnualRoe(ReportSnapshot snapshot) {
        if (snapshot.ratios() == null || snapshot.columns() == null) {
            return Optional.empty();
        }
        Optional<ReportSnapshot.RatioRow> roeRow = snapshot.ratios().stream()
                .filter(row -> "roe".equals(row.key()))
                .findFirst();
        if (roeRow.isEmpty() || roeRow.get().values() == null) {
            return Optional.empty();
        }
        Map<String, String> values = roeRow.get().values();
        return snapshot.columns().stream()
                .filter(column -> !column.partial())
                .map(ReportSnapshot.ColumnMeta::year)
                .filter(year -> values.get(year) != null)
                .max(String::compareTo)
                .map(year -> Map.entry(year, new BigDecimal(values.get(year))));
    }
}
