package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialIndexResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailGroup;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineIndexGroup;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineRow;
import com.thlee.stock.market.stockmarket.stock.application.dto.TimelineColumnData;
import com.thlee.stock.market.stockmarket.stock.domain.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

/**
 * 수집된 연도별 데이터를 "항목 × 연도" 매트릭스로 병합한다.
 * 행 순서는 최신 연도의 등장 순서를 우선한다 (과거에만 존재한 계정은 뒤에 붙음).
 */
@Component
public class FinancialTimelineAssembler {

    private static final String UNUSED_ACCOUNT_ID = "-표준계정코드 미사용-";
    private static final String CASH_FLOW_DIV = "CF";
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
        return mergeRows(data, column -> summaryAccountsOf(column, fsDiv),
                FinancialAccount::accountName, FinancialAccount::accountName, FinancialAccount::currentTermAmount);
    }

    private List<FinancialAccount> summaryAccountsOf(TimelineColumnData column, String fsDiv) {
        return column.accounts().stream()
                .filter(account -> fsDiv.equals(account.fsDiv()))
                .toList();
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
                .filter(item -> accountId.equals(item.accountId()) || accountName.equals(item.accountName()))
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
        return new TimelineDetailGroup(statementDiv, statementName, rows);
    }

    private List<FullFinancialStatement> detailsOf(TimelineColumnData column, String statementDiv) {
        return column.details().stream()
                .filter(item -> statementDiv.equals(item.statementDiv()))
                .toList();
    }

    /**
     * 연도 간 행 매칭 키: account_id 우선, 표준계정코드 미사용이면 계정명 폴백
     */
    private String detailRowKey(FullFinancialStatement item) {
        if (item.accountId() == null || UNUSED_ACCOUNT_ID.equals(item.accountId())) {
            return item.accountName();
        }
        return item.accountId();
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
