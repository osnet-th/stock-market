package com.thlee.stock.market.stockmarket.notification.infrastructure.email;

import com.thlee.stock.market.stockmarket.notification.application.ReportRenderer;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioEvaluation.ItemEvaluation;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 평가 결과를 HTML 이메일 본문으로 렌더링
 * 포트폴리오 화면과 동일한 섹션 구성: 국내 주식 → 해외 주식 → 채권 → 부동산 → 펀드 → 현금 → 기타
 */
@Component
public class PortfolioReportRenderer implements ReportRenderer {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<String, String> SECTION_LABELS = new LinkedHashMap<>();
    static {
        SECTION_LABELS.put("STOCK_KR", "국내 주식");
        SECTION_LABELS.put("STOCK_OVERSEAS", "해외 주식");
        SECTION_LABELS.put("BOND", "채권");
        SECTION_LABELS.put("REAL_ESTATE", "부동산");
        SECTION_LABELS.put("FUND", "펀드");
        SECTION_LABELS.put("CRYPTO", "암호화폐");
        SECTION_LABELS.put("GOLD", "금");
        SECTION_LABELS.put("COMMODITY", "원자재");
        SECTION_LABELS.put("CASH", "현금성 자산");
        SECTION_LABELS.put("OTHER", "기타");
    }

    private static final Map<String, String> SECTION_COLORS = new LinkedHashMap<>();
    static {
        SECTION_COLORS.put("STOCK_KR", "#2563eb");
        SECTION_COLORS.put("STOCK_OVERSEAS", "#7c3aed");
        SECTION_COLORS.put("BOND", "#059669");
        SECTION_COLORS.put("REAL_ESTATE", "#d97706");
        SECTION_COLORS.put("FUND", "#0891b2");
        SECTION_COLORS.put("CRYPTO", "#e11d48");
        SECTION_COLORS.put("GOLD", "#ca8a04");
        SECTION_COLORS.put("COMMODITY", "#9333ea");
        SECTION_COLORS.put("CASH", "#6b7280");
        SECTION_COLORS.put("OTHER", "#6b7280");
    }

    @Override
    public String renderReport(PortfolioEvaluation evaluation, LocalDate date) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>");
        sb.append("<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">");

        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append("<tr><td align=\"center\" style=\"padding:20px 0;\">");

        sb.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
          .append("style=\"background-color:#ffffff;border:1px solid #e0e0e0;\">");

        // 헤더
        sb.append("<tr><td style=\"background-color:#1a56db;padding:20px 30px;\">");
        sb.append("<h1 style=\"margin:0;color:#ffffff;font-size:18px;\">포트폴리오 마감 리포트</h1>");
        sb.append("<p style=\"margin:4px 0 0;color:#c3dafe;font-size:13px;\">").append(date.format(DATE_FORMAT)).append("</p>");
        sb.append("</td></tr>");

        // 요약
        renderSummary(sb, evaluation);

        // 섹션별 항목 테이블
        Map<String, List<ItemEvaluation>> sectionMap = groupBySection(evaluation.getItems());
        for (Map.Entry<String, String> entry : SECTION_LABELS.entrySet()) {
            String sectionKey = entry.getKey();
            List<ItemEvaluation> items = sectionMap.get(sectionKey);
            if (items != null && !items.isEmpty()) {
                renderSection(sb, entry.getValue(), SECTION_COLORS.get(sectionKey), items, sectionKey.startsWith("STOCK"));
            }
        }

        // 면책 문구
        sb.append("<tr><td style=\"padding:15px 30px;border-top:1px solid #e0e0e0;\">");
        sb.append("<p style=\"margin:0;font-size:11px;color:#999999;line-height:1.5;\">");
        sb.append("본 메일은 참고용이며, 실제 평가금액과 차이가 있을 수 있습니다. 투자 판단은 본인의 책임입니다.");
        sb.append("</p></td></tr>");

        sb.append("</table>");
        sb.append("</td></tr></table>");
        sb.append("</body></html>");

        return sb.toString();
    }

    private Map<String, List<ItemEvaluation>> groupBySection(List<ItemEvaluation> items) {
        return items.stream().collect(Collectors.groupingBy(item -> {
            if ("STOCK".equals(item.getAssetType())) {
                return "KR".equals(item.getCountry()) ? "STOCK_KR" : "STOCK_OVERSEAS";
            }
            return item.getAssetType();
        }));
    }

    private void renderSummary(StringBuilder sb, PortfolioEvaluation evaluation) {
        BigDecimal totalInvested = evaluation.getTotalInvested();
        BigDecimal totalEvaluated = evaluation.getTotalEvaluated();
        BigDecimal profitAmount = totalEvaluated.subtract(totalInvested);
        BigDecimal profitRate = BigDecimal.ZERO;
        if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
            profitRate = profitAmount.multiply(BigDecimal.valueOf(100))
                    .divide(totalInvested, 2, RoundingMode.HALF_UP);
        }

        boolean isProfit = profitAmount.compareTo(BigDecimal.ZERO) >= 0;
        String profitColor = isProfit ? "#dc2626" : "#2563eb";
        String profitSign = isProfit ? "+" : "";

        sb.append("<tr><td style=\"padding:20px 30px;\">");
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        sb.append("<tr>");

        sb.append("<td width=\"33%\" style=\"text-align:center;\">");
        sb.append("<p style=\"margin:0;font-size:11px;color:#666;\">총 투자금</p>");
        sb.append("<p style=\"margin:4px 0 0;font-size:16px;font-weight:bold;color:#333;\">")
          .append(formatKrw(totalInvested)).append("원</p>");
        sb.append("</td>");

        sb.append("<td width=\"33%\" style=\"text-align:center;\">");
        sb.append("<p style=\"margin:0;font-size:11px;color:#666;\">총 평가금</p>");
        sb.append("<p style=\"margin:4px 0 0;font-size:16px;font-weight:bold;color:#333;\">")
          .append(formatKrw(totalEvaluated)).append("원</p>");
        sb.append("</td>");

        sb.append("<td width=\"33%\" style=\"text-align:center;\">");
        sb.append("<p style=\"margin:0;font-size:11px;color:#666;\">총 수익</p>");
        sb.append("<p style=\"margin:4px 0 0;font-size:16px;font-weight:bold;color:").append(profitColor).append(";\">")
          .append(profitSign).append(formatKrw(profitAmount)).append("원 (")
          .append(profitSign).append(profitRate).append("%)</p>");
        sb.append("</td>");

        sb.append("</tr></table>");
        sb.append("</td></tr>");
    }

    private void renderSection(StringBuilder sb, String sectionLabel, String sectionColor,
                                List<ItemEvaluation> items, boolean isStock) {
        // 섹션 소계
        BigDecimal sectionInvested = items.stream()
                .map(ItemEvaluation::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sectionEvaluated = items.stream()
                .map(ItemEvaluation::getEvaluatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sectionProfit = sectionEvaluated.subtract(sectionInvested);
        boolean isSectionProfit = sectionProfit.compareTo(BigDecimal.ZERO) >= 0;

        sb.append("<tr><td style=\"padding:0 30px 15px;\">");

        // 섹션 헤더
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" ")
          .append("style=\"margin-bottom:8px;\">");
        sb.append("<tr>");
        sb.append("<td style=\"padding:8px 0;border-bottom:2px solid ").append(sectionColor).append(";\">");
        sb.append("<span style=\"font-size:14px;font-weight:bold;color:").append(sectionColor).append(";\">")
          .append(sectionLabel).append("</span>");
        sb.append("<span style=\"font-size:12px;color:#999;margin-left:8px;\">")
          .append(items.size()).append("개</span>");
        sb.append("</td>");
        sb.append("<td style=\"text-align:right;padding:8px 0;border-bottom:2px solid ").append(sectionColor).append(";\">");
        sb.append("<span style=\"font-size:13px;font-weight:bold;color:#333;\">")
          .append(formatKrw(sectionEvaluated)).append("원</span>");
        sb.append("<span style=\"font-size:11px;color:").append(isSectionProfit ? "#dc2626" : "#2563eb").append(";margin-left:6px;\">")
          .append(isSectionProfit ? "+" : "").append(formatKrw(sectionProfit)).append("</span>");
        sb.append("</td>");
        sb.append("</tr></table>");

        // 항목 테이블
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"6\" cellspacing=\"0\" ")
          .append("style=\"border-collapse:collapse;font-size:12px;\">");

        for (ItemEvaluation item : items) {
            BigDecimal profit = item.getEvaluatedAmount().subtract(item.getInvestedAmount());
            boolean isItemProfit = profit.compareTo(BigDecimal.ZERO) >= 0;
            String itemProfitColor = isItemProfit ? "#dc2626" : "#2563eb";
            String itemProfitSign = isItemProfit ? "+" : "";

            sb.append("<tr>");

            // 종목명
            sb.append("<td style=\"border-bottom:1px solid #f0f0f0;padding:6px;\">");
            sb.append(HtmlUtils.htmlEscape(item.getItemName()));
            if (isStock && item.getQuantity() != null) {
                sb.append(" <span style=\"color:#999;font-size:10px;\">").append(item.getQuantity()).append("주</span>");
            }
            sb.append("</td>");

            // 투자금
            sb.append("<td style=\"text-align:right;border-bottom:1px solid #f0f0f0;padding:6px;color:#666;\">")
              .append(formatKrw(item.getInvestedAmount())).append("</td>");

            // 평가금
            sb.append("<td style=\"text-align:right;border-bottom:1px solid #f0f0f0;padding:6px;font-weight:bold;\">")
              .append(formatKrw(item.getEvaluatedAmount())).append("</td>");

            // 손익
            sb.append("<td style=\"text-align:right;border-bottom:1px solid #f0f0f0;padding:6px;color:")
              .append(itemProfitColor).append(";\">")
              .append(itemProfitSign).append(formatKrw(profit)).append("</td>");

            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("</td></tr>");
    }

    private String formatKrw(BigDecimal amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.KOREA);
        return format.format(amount.setScale(0, RoundingMode.HALF_UP));
    }
}