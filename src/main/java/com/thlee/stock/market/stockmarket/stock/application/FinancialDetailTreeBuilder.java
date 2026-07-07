package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailNode;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineRow;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 병합된 세부 계정 평면 행을 IFRS 앵커(account_id) + 순서 기반으로 2단계 계층 노드로 변환한다.
 * 계층은 DART 응답에 명시되지 않고 account_id에 암묵적으로만 존재한다 (brainstorm 참고).
 *
 * 앵커 2종:
 * - CATEGORY: 뒤따르는 평면 항목을 자식으로 흡수(유동자산·영업활동 등). 자식이 0개면 총계(강조행)로 창발.
 * - TERMINAL: 단독 강조행. 현재 카테고리를 닫되 자식을 흡수하지 않음(현금흐름표의 기초/기말/외화환산/순증감).
 * 앵커가 없는 재무제표(IS/CIS/SCE)는 전 행을 ITEM 평면 노드로 둔다(회귀 없음).
 */
@Component
public class FinancialDetailTreeBuilder {

    private static final String TOTAL = "TOTAL";
    private static final String CATEGORY = "CATEGORY";
    private static final String ITEM = "ITEM";
    private static final String MISC_ID = "__misc__";
    private static final String MISC_NAME = "기타(미분류)";

    /** statementDiv별 앵커. Phase 0 실호출(삼성전자 2023 CFS)로 확정. */
    private static final Map<String, Anchors> REGISTRY = Map.of(
            "BS", new Anchors(
                    Set.of("ifrs-full_Assets", "ifrs-full_CurrentAssets", "ifrs-full_NoncurrentAssets",
                            "ifrs-full_Liabilities", "ifrs-full_CurrentLiabilities", "ifrs-full_NoncurrentLiabilities",
                            "ifrs-full_Equity", "ifrs-full_EquityAndLiabilities"),
                    Set.of()),
            "CF", new Anchors(
                    Set.of("ifrs-full_CashFlowsFromUsedInOperatingActivities",
                            "ifrs-full_CashFlowsFromUsedInInvestingActivities",
                            "ifrs-full_CashFlowsFromUsedInFinancingActivities"),
                    Set.of("dart_CashAndCashEquivalentsAtBeginningOfPeriodCf",
                            "dart_CashAndCashEquivalentsAtEndOfPeriodCf",
                            "ifrs-full_EffectOfExchangeRateChangesOnCashAndCashEquivalents",
                            "ifrs-full_IncreaseDecreaseInCashAndCashEquivalents")));

    /**
     * 평면 재무제표에서 굵게 강조(TOTAL)할 손익 마일스톤 account_id. Phase 0 실측 확정.
     * 일부 종목은 별도 IS 없이 CIS에 손익 내용을 담으므로 IS/CIS 모두에 적용(SCE 미적용).
     */
    private static final Set<String> INCOME_MILESTONES = Set.of(
            "ifrs-full_Revenue",                            // 매출액/영업수익
            "ifrs-full_GrossProfit",                        // 매출총이익
            "dart_OperatingIncomeLoss",                     // 영업이익
            "ifrs-full_ProfitLossFromOperatingActivities",  // 영업이익 (대체 표준 id)
            "ifrs-full_ProfitLossBeforeTax",                // 법인세비용차감전순이익
            "ifrs-full_ProfitLossFromContinuingOperations", // 계속영업이익
            "ifrs-full_ProfitLoss",                         // 당기순이익
            "ifrs-full_OtherComprehensiveIncome",           // 기타포괄손익
            "ifrs-full_ComprehensiveIncome");               // 총포괄손익

    private static final Map<String, Set<String>> EMPHASIS = Map.of(
            "IS", INCOME_MILESTONES, "CIS", INCOME_MILESTONES);

    public List<TimelineDetailNode> buildNodes(String statementDiv, List<TimelineRow> rows) {
        Anchors anchors = REGISTRY.get(statementDiv);
        if (anchors == null) {
            return flatNodes(statementDiv, rows);
        }
        return groupedNodes(anchors, rows);
    }

    // === 평면 모드 (앵커 없는 재무제표: IS/CIS/SCE) — 핵심 소계는 강조(TOTAL) ===

    private List<TimelineDetailNode> flatNodes(String statementDiv, List<TimelineRow> rows) {
        Set<String> emphasis = EMPHASIS.getOrDefault(statementDiv, Set.of());
        return rows.stream().map(row -> flatNode(row, emphasis)).toList();
    }

    private static TimelineDetailNode flatNode(TimelineRow row, Set<String> emphasis) {
        String id = row.getId();
        return (id != null && emphasis.contains(id)) ? totalNode(row) : itemNode(row);
    }

    // === 컨테이너 모드 (BS/CF) ===

    private List<TimelineDetailNode> groupedNodes(Anchors anchors, List<TimelineRow> rows) {
        GroupingState state = new GroupingState();
        rows.forEach(row -> state.place(anchors, row));
        return state.finish();
    }

    // === 노드 팩토리 ===

    private static TimelineDetailNode anchorNode(TimelineRow row, List<TimelineRow> children) {
        String role = children.isEmpty() ? TOTAL : CATEGORY;
        return new TimelineDetailNode(role, row, List.copyOf(children));
    }

    private static TimelineDetailNode totalNode(TimelineRow row) {
        return new TimelineDetailNode(TOTAL, row, List.of());
    }

    private static TimelineDetailNode itemNode(TimelineRow row) {
        return new TimelineDetailNode(ITEM, row, List.of());
    }

    private static TimelineDetailNode miscNode(List<TimelineRow> children) {
        return new TimelineDetailNode(CATEGORY, new TimelineRow(MISC_ID, MISC_NAME, Map.of()), List.copyOf(children));
    }

    /** category=자식 흡수 앵커, terminal=단독 강조행 앵커 */
    private record Anchors(Set<String> category, Set<String> terminal) {}

    /**
     * 순서 기반 1-pass 그룹핑 상태. 열린 카테고리(openRow)에 평면 항목을 자식으로 붙이고,
     * 앵커/종료 시 flush한다. 첫 앵커 이전의 평면 항목만 orphans로 모여 "기타(미분류)"가 된다.
     */
    private static final class GroupingState {

        private final List<TimelineDetailNode> nodes = new ArrayList<>();
        private final List<TimelineRow> orphans = new ArrayList<>();
        private TimelineRow openRow;
        private List<TimelineRow> openChildren;

        void place(Anchors anchors, TimelineRow row) {
            String id = row.getId();
            if (id != null && anchors.category().contains(id)) {
                openCategory(row);
            } else if (id != null && anchors.terminal().contains(id)) {
                addTerminal(row);
            } else {
                addPlain(row);
            }
        }

        List<TimelineDetailNode> finish() {
            flushOpen();
            if (!orphans.isEmpty()) {
                nodes.add(miscNode(orphans));
            }
            return nodes;
        }

        private void openCategory(TimelineRow row) {
            flushOpen();
            openRow = row;
            openChildren = new ArrayList<>();
        }

        private void addTerminal(TimelineRow row) {
            flushOpen();
            nodes.add(totalNode(row));
        }

        private void addPlain(TimelineRow row) {
            (openChildren != null ? openChildren : orphans).add(row);
        }

        private void flushOpen() {
            if (openRow == null) {
                return;
            }
            nodes.add(anchorNode(openRow, openChildren));
            openRow = null;
            openChildren = null;
        }
    }
}
