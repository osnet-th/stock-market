package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialIndexResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailGroup;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineIndexGroup;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineRow;
import com.thlee.stock.market.stockmarket.stock.application.dto.TimelineColumnData;
import com.thlee.stock.market.stockmarket.stock.domain.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 수집된 연도별 데이터를 "항목 × 연도" 매트릭스로 병합한다.
 * 행 순서는 최신 연도의 등장 순서를 우선한다 (과거에만 존재한 계정은 뒤에 붙음).
 */
@Component
@RequiredArgsConstructor
public class FinancialTimelineAssembler {

    private final FinancialDetailTreeBuilder detailTreeBuilder;

    private static final String UNUSED_ACCOUNT_ID = "-표준계정코드 미사용-";
    private static final String CASH_FLOW_DIV = "CF";
    private static final String INCOME_STMT_DIV = "IS";
    private static final String COMPREHENSIVE_INCOME_DIV = "CIS";
    /** 손익 요약 총계 표준 계정 id (요약 결측 연도 보강용) */
    private static final String REVENUE_ID = "ifrs-full_Revenue";
    private static final String OPERATING_PROFIT_ID = "ifrs-full_ProfitLossFromOperatingActivities";
    private static final String NET_INCOME_ID = "ifrs-full_ProfitLoss";
    private static final String FCF_NAME = "잉여현금흐름";

    /** Phase 0 실호출로 확정한 FCF 구성 계정의 IFRS 표준 ID (plan Open Questions 참고) */
    private static final String OPERATING_CF_ID = "ifrs-full_CashFlowsFromUsedInOperatingActivities";
    private static final String PPE_PURCHASE_ID =
            "ifrs-full_PurchaseOfPropertyPlantAndEquipmentClassifiedAsInvestingActivities";
    private static final String INTANGIBLE_PURCHASE_ID =
            "ifrs-full_PurchaseOfIntangibleAssetsClassifiedAsInvestingActivities";

    /** 표준계정코드 미사용 종목 대비 — 계정명 폴백 (account_id 매칭 실패 시) */
    private static final String OPERATING_CF_NAME = "영업활동현금흐름";
    private static final String PPE_PURCHASE_NAME = "유형자산의 취득";
    private static final String INTANGIBLE_PURCHASE_NAME = "무형자산의 취득";

    /** IFRS 계정 id 접두사 — 구(ifrs_) 신고분을 신(ifrs-full_) 표준형으로 정규화해 연도 간 병합 */
    private static final String IFRS_LEGACY_PREFIX = "ifrs_";
    private static final String IFRS_FULL_PREFIX = "ifrs-full_";

    public FinancialTimelineResponse assemble(List<TimelineColumnData> data, String fsDiv, Set<TimelineItem> items) {
        return FinancialTimelineResponse.builder()
                .columns(data.stream().map(TimelineColumnData::column).toList())
                .summaryAccounts(items.contains(TimelineItem.ACCOUNTS) ? toSummaryRows(data, fsDiv) : null)
                .indices(items.contains(TimelineItem.INDICES) ? toIndexGroups(data) : null)
                .shares(items.contains(TimelineItem.SHARES) ? toShareRows(data) : null)
                .fcf(items.contains(TimelineItem.FCF) ? toFcfRow(data) : null)
                .details(items.contains(TimelineItem.DETAILS) ? toDetailGroups(data) : null)
                .build();
    }

    // === 요약 재무계정 ===

    private List<TimelineRow> toSummaryRows(List<TimelineColumnData> data, String fsDiv) {
        List<TimelineRow> rows = new ArrayList<>(mergeRows(data, column -> summaryAccountsOf(column, fsDiv),
                FinancialAccount::accountName, FinancialAccount::accountName, FinancialAccount::currentTermAmount));
        backfillSummaryAmounts(rows, data);
        return rows;
    }

    private List<FinancialAccount> summaryAccountsOf(TimelineColumnData column, String fsDiv) {
        return column.accounts().stream()
                .filter(account -> fsDiv.equals(account.fsDiv()))
                .toList();
    }

    /**
     * 요약 손익 금액(매출액·영업이익·당기순이익) 결측 연도 보강 — 주요계정(fnlttSinglAcnt)에
     * 없는 연도를 전체재무제표 IS 총계로 채운다. 기존 값은 보존(putIfAbsent).
     */
    private void backfillSummaryAmounts(List<TimelineRow> rows, List<TimelineColumnData> data) {
        backfillSummaryRow(rows, "매출액", detailTotalByYear(data, REVENUE_ID, this::isRevenueTotalName));
        backfillSummaryRow(rows, "영업이익", detailTotalByYear(data, OPERATING_PROFIT_ID, this::isOperatingProfitTotalName));
        backfillSummaryRow(rows, "당기순이익", detailTotalByYear(data, NET_INCOME_ID, this::isNetIncomeTotalName));
    }

    private void backfillSummaryRow(List<TimelineRow> rows, String namePrefix, Map<String, String> byYear) {
        if (byYear.isEmpty()) {
            return;
        }
        TimelineRow target = rows.stream()
                .filter(row -> normalize(row.getName()).startsWith(namePrefix))
                .findFirst()
                .orElse(null);
        if (target == null) {
            rows.add(new TimelineRow(namePrefix, namePrefix, new LinkedHashMap<>(byYear)));
            return;
        }
        byYear.forEach((year, value) -> target.getValues().putIfAbsent(year, value));
    }

    /** 전체재무제표 IS/CIS에서 연도별 총계 금액 — 표준 id 우선, 무표준코드는 이름 폴백 */
    private Map<String, String> detailTotalByYear(List<TimelineColumnData> data,
            String standardId, Predicate<FullFinancialStatement> nameMatch) {
        Map<String, String> series = new LinkedHashMap<>();
        for (TimelineColumnData column : data) {
            incomeStatementTotal(column.details(), standardId, nameMatch)
                    .ifPresent(item -> series.putIfAbsent(column.year(), item.currentTermAmount()));
        }
        return series;
    }

    /** 손익계산서 총계 행: 표준 id(접두사 정규화) 우선, 없으면(무표준코드) 이름 매칭 폴백 */
    private Optional<FullFinancialStatement> incomeStatementTotal(List<FullFinancialStatement> details,
            String standardId, Predicate<FullFinancialStatement> nameMatch) {
        List<FullFinancialStatement> incomeRows = details.stream()
                .filter(item -> INCOME_STMT_DIV.equals(item.statementDiv())
                        || COMPREHENSIVE_INCOME_DIV.equals(item.statementDiv()))
                .toList();
        return incomeRows.stream()
                .filter(item -> standardId.equals(canonicalAccountId(item.accountId())))
                .findFirst()
                .or(() -> incomeRows.stream().filter(nameMatch).findFirst());
    }

    private boolean isRevenueTotalName(FullFinancialStatement item) {
        String name = normalize(item.accountName());
        return name.contains("매출액") && !name.contains("률");
    }

    private boolean isOperatingProfitTotalName(FullFinancialStatement item) {
        String name = normalize(item.accountName());
        return name.contains("영업이익") && !name.contains("률");
    }

    /** 연결 총계 당기순이익: 지배·비지배·계속영업·중단영업·주당(EPS) 라인 제외 */
    private boolean isNetIncomeTotalName(FullFinancialStatement item) {
        String name = normalize(item.accountName());
        return name.contains("당기순이익")
                && !name.contains("주당")
                && !name.contains("지배") && !name.contains("비지배")
                && !name.contains("계속영업") && !name.contains("중단영업");
    }

    private String normalize(String name) {
        return name == null ? "" : name.replace(" ", "");
    }

    // === 재무지표 (4분류) ===

    private List<TimelineIndexGroup> toIndexGroups(List<TimelineColumnData> data) {
        return Arrays.stream(IndexClassCode.values())
                .map(classCode -> toIndexGroup(data, classCode))
                .toList();
    }

    private TimelineIndexGroup toIndexGroup(List<TimelineColumnData> data, IndexClassCode classCode) {
        List<TimelineRow> rows = mergeRows(data, column -> indicesOf(column, classCode),
                FinancialIndexResponse::getIndexName, FinancialIndexResponse::getIndexName,
                FinancialIndexResponse::getIndexValue);
        return new TimelineIndexGroup(classCode.getCode(), classCode.getLabel(), rows);
    }

    private List<FinancialIndexResponse> indicesOf(TimelineColumnData column, IndexClassCode classCode) {
        return column.indices().stream()
                .filter(index -> classCode.getCode().equals(index.getIndexClassCode()))
                .toList();
    }

    // === 발행 주식수 (주식 종류별 행: 보통주/우선주/합계) ===

    private List<TimelineRow> toShareRows(List<TimelineColumnData> data) {
        return mergeRows(data, TimelineColumnData::shares,
                StockQuantity::category, StockQuantity::category, StockQuantity::issuedTotalQuantity);
    }

    // === 잉여현금흐름 (파생) ===

    private TimelineRow toFcfRow(List<TimelineColumnData> data) {
        Map<String, String> values = new LinkedHashMap<>();
        data.forEach(column -> values.put(column.year(), calculateFcf(column.details())));
        return new TimelineRow(FCF_NAME, FCF_NAME, values);
    }

    /**
     * FCF = 영업활동현금흐름 − |유형자산 취득| − |무형자산 취득|
     * 영업활동현금흐름이 없으면 null (0 대체 금지), 취득액은 없으면 0으로 간주.
     */
    private String calculateFcf(List<FullFinancialStatement> details) {
        BigDecimal operating = findCashFlowAmount(details, OPERATING_CF_ID, OPERATING_CF_NAME);
        if (operating == null) {
            return null;
        }
        return operating
                .subtract(absOrZero(findCashFlowAmount(details, PPE_PURCHASE_ID, PPE_PURCHASE_NAME)))
                .subtract(absOrZero(findCashFlowAmount(details, INTANGIBLE_PURCHASE_ID, INTANGIBLE_PURCHASE_NAME)))
                .toPlainString();
    }

    private BigDecimal findCashFlowAmount(List<FullFinancialStatement> details, String accountId, String accountName) {
        return details.stream()
                .filter(item -> CASH_FLOW_DIV.equals(item.statementDiv()))
                .filter(item -> accountId.equals(canonicalAccountId(item.accountId())) || accountName.equals(item.accountName()))
                .findFirst()
                .map(item -> parseAmount(item.currentTermAmount()))
                .orElse(null);
    }

    private BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(amount.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal absOrZero(BigDecimal value) {
        return value != null ? value.abs() : BigDecimal.ZERO;
    }

    // === 전체 재무제표 세부 계정 (재무제표 종류별 그룹) ===

    private List<TimelineDetailGroup> toDetailGroups(List<TimelineColumnData> data) {
        return collectStatementNames(data).entrySet().stream()
                .map(entry -> toDetailGroup(data, entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<String, String> collectStatementNames(List<TimelineColumnData> data) {
        Map<String, String> names = new LinkedHashMap<>();
        newestFirst(data).forEach(column ->
                column.details().forEach(item -> names.putIfAbsent(item.statementDiv(), item.statementName())));
        return names;
    }

    private TimelineDetailGroup toDetailGroup(List<TimelineColumnData> data, String statementDiv, String statementName) {
        List<TimelineRow> rows = mergeRows(data, column -> detailsOf(column, statementDiv),
                this::detailRowKey, FullFinancialStatement::accountName, FullFinancialStatement::currentTermAmount);
        return new TimelineDetailGroup(statementDiv, statementName, detailTreeBuilder.buildNodes(statementDiv, rows));
    }

    private List<FullFinancialStatement> detailsOf(TimelineColumnData column, String statementDiv) {
        return column.details().stream()
                .filter(item -> statementDiv.equals(item.statementDiv()))
                .toList();
    }

    /**
     * 연도 간 행 매칭 키: account_id(접두사 정규화) 우선, 표준계정코드 미사용이면 계정명 폴백
     */
    private String detailRowKey(FullFinancialStatement item) {
        if (item.accountId() == null || UNUSED_ACCOUNT_ID.equals(item.accountId())) {
            return item.accountName();
        }
        return canonicalAccountId(item.accountId());
    }

    /**
     * IFRS 계정 id 접두사 정규화: 구 {@code ifrs_} → 신 {@code ifrs-full_}(표준형).
     * 동일 taxonomy 요소가 연도별로 다른 접두사(2018년 이전 ifrs_, 이후 ifrs-full_)로 신고돼도 한 행으로 병합한다.
     * {@code ifrs-full_}·{@code dart_}·기타 접두사는 그대로 둔다.
     */
    private String canonicalAccountId(String accountId) {
        if (accountId != null && accountId.startsWith(IFRS_LEGACY_PREFIX)) {
            return IFRS_FULL_PREFIX + accountId.substring(IFRS_LEGACY_PREFIX.length());
        }
        return accountId;
    }

    // === 공통 병합 헬퍼 ===

    private <T> List<TimelineRow> mergeRows(List<TimelineColumnData> data,
            Function<TimelineColumnData, List<T>> extractor,
            Function<T, String> keyFn, Function<T, String> nameFn, Function<T, String> valueFn) {
        Map<String, TimelineRow> rows = new LinkedHashMap<>();
        newestFirst(data).forEach(column -> accumulate(rows, column, extractor, keyFn, nameFn, valueFn));
        return List.copyOf(rows.values());
    }

    private <T> void accumulate(Map<String, TimelineRow> rows, TimelineColumnData column,
            Function<TimelineColumnData, List<T>> extractor,
            Function<T, String> keyFn, Function<T, String> nameFn, Function<T, String> valueFn) {
        for (T item : extractor.apply(column)) {
            TimelineRow row = rows.computeIfAbsent(keyFn.apply(item),
                    key -> new TimelineRow(key, nameFn.apply(item), new LinkedHashMap<>()));
            row.getValues().putIfAbsent(column.year(), valueFn.apply(item));
        }
    }

    private List<TimelineColumnData> newestFirst(List<TimelineColumnData> data) {
        return data.stream()
                .sorted(Comparator.comparing(TimelineColumnData::year).reversed())
                .toList();
    }
}
