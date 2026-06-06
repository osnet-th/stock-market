package com.thlee.stock.market.stockmarket.stockevaluation.domain.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * KIS 예탁원정보(일정) API 종류 (12종).
 * URL 코드, 경로, tr_id, 응답 필드 라벨, 종류별 고정 추가 파라미터(GB1/HIGH_GB/MARKET_GB)를 보유한다.
 * 공통 파라미터: CTS, F_DT, T_DT, SHT_CD(종목 필터). 단일 output1(일정 목록).
 */
@Getter
public enum KsdScheduleType {

    DIVIDEND("dividend", "/uapi/domestic-stock/v1/ksdinfo/dividend", "HHKDB669102C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "divi_kind", "배당종류", "face_val", "액면가",
           "per_sto_divi_amt", "현금배당금", "divi_rate", "현금배당률(%)", "stk_divi_rate", "주식배당률(%)",
           "divi_pay_dt", "배당금지급일", "stk_div_pay_dt", "주식배당지급일", "odd_pay_dt", "단주대금지급일",
           "stk_kind", "주식종류", "high_divi_gb", "고배당여부"),
        om("GB1", "0", "HIGH_GB", "")),

    PURCHASE_REQUEST("purchase-request", "/uapi/domestic-stock/v1/ksdinfo/purreq", "HHKDB669103C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "stk_kind", "주식종류", "opp_opi_rcpt_term", "반대의사접수시한",
           "buy_req_rcpt_term", "매수청구접수시한", "buy_req_price", "매수청구가격", "buy_amt_pay_dt", "매수대금지급일",
           "meet_dt", "주총일"),
        om()),

    MERGER_SPLIT("merger-split", "/uapi/domestic-stock/v1/ksdinfo/merger-split", "HHKDB669104C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "opp_cust_cd", "피합병회사코드", "opp_cust_nm", "피합병회사명",
           "cust_cd", "합병회사코드", "cust_nm", "합병회사명", "merge_type", "합병사유", "merge_rate", "비율",
           "td_stop_dt", "매매거래정지기간", "list_dt", "상장/등록일", "odd_amt_pay_dt", "단주대금지급일",
           "tot_issue_stk_qty", "발행주식", "issue_stk_qty", "발행할주식", "seq", "연번"),
        om()),

    FACE_VALUE_CHANGE("face-value-change", "/uapi/domestic-stock/v1/ksdinfo/rev-split", "HHKDB669105C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "inter_bf_face_amt", "변경전액면가", "inter_af_face_amt", "변경후액면가",
           "td_stop_dt", "매매거래정지기간", "list_dt", "상장/등록일"),
        om("MARKET_GB", "")),

    CAPITAL_REDUCTION("capital-reduction", "/uapi/domestic-stock/v1/ksdinfo/cap-dcrs", "HHKDB669106C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "stk_kind", "주식종류", "reduce_cap_type", "감자구분",
           "reduce_cap_rate", "감자배정율", "comp_way", "계산방법", "td_stop_dt", "매매거래정지기간", "list_dt", "상장/등록일"),
        om()),

    LISTING("listing", "/uapi/domestic-stock/v1/ksdinfo/list-info", "HHKDB669107C0",
        om("list_dt", "상장/등록일", "sht_cd", "종목코드", "stk_kind", "주식종류", "issue_type", "사유",
           "issue_stk_qty", "상장주식수", "tot_issue_stk_qty", "총발행주식수", "issue_price", "발행가"),
        om()),

    PUBLIC_OFFERING("public-offering", "/uapi/domestic-stock/v1/ksdinfo/pub-offer", "HHKDB669108C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "fix_subscr_pri", "공모가", "face_value", "액면가",
           "subscr_dt", "청약기간", "pay_dt", "납입일", "refund_dt", "환불일", "list_dt", "상장/등록일",
           "lead_mgr", "주간사", "pub_bf_cap", "공모전자본금", "pub_af_cap", "공모후자본금", "assign_stk_qty", "당사배정물량"),
        om()),

    FORFEITED("forfeited", "/uapi/domestic-stock/v1/ksdinfo/forfeit", "HHKDB669109C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "subscr_dt", "청약일", "subscr_price", "공모가",
           "subscr_stk_qty", "공모주식수", "refund_dt", "환불일", "list_dt", "상장/등록일", "lead_mgr", "주간사"),
        om()),

    MANDATORY_DEPOSIT("mandatory-deposit", "/uapi/domestic-stock/v1/ksdinfo/mand-deposit", "HHKDB669110C0",
        om("sht_cd", "종목코드", "stk_qty", "주식수", "depo_date", "예치일", "depo_reason", "사유",
           "tot_issue_qty_per_rate", "총발행주식수대비비율(%)"),
        om()),

    PAID_IN_CAPITAL("paid-in-capital", "/uapi/domestic-stock/v1/ksdinfo/paidin-capin", "HHKDB669100C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "tot_issue_stk_qty", "발행주식", "issue_stk_qty", "발행할주식",
           "fix_rate", "확정배정율", "disc_rate", "할인율", "fix_price", "발행예정가", "right_dt", "권리락일",
           "sub_term_ft", "청약기간시작", "sub_term", "청약기간종료", "list_date", "상장/등록일", "stk_kind", "주식종류"),
        om("GB1", "1")),

    BONUS_ISSUE("bonus-issue", "/uapi/domestic-stock/v1/ksdinfo/bonus-issue", "HHKDB669101C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "fix_rate", "확정배정율", "odd_rec_price", "단주기준가",
           "right_dt", "권리락일", "odd_pay_dt", "단주대금지급일", "list_date", "상장/등록일",
           "tot_issue_stk_qty", "발행주식", "issue_stk_qty", "발행할주식", "stk_kind", "주식종류"),
        om()),

    SHAREHOLDERS_MEETING("shareholders-meeting", "/uapi/domestic-stock/v1/ksdinfo/sharehld-meet", "HHKDB669111C0",
        om("record_date", "기준일", "sht_cd", "종목코드", "gen_meet_dt", "주총일자", "gen_meet_type", "주총사유",
           "agenda", "주총의안", "vote_tot_qty", "의결권주식총수"),
        om());

    private final String code;
    private final String path;
    private final String trId;
    private final LinkedHashMap<String, String> labels;
    private final LinkedHashMap<String, String> extraParams;

    KsdScheduleType(String code, String path, String trId,
                    LinkedHashMap<String, String> labels, LinkedHashMap<String, String> extraParams) {
        this.code = code;
        this.path = path;
        this.trId = trId;
        this.labels = labels;
        this.extraParams = extraParams;
    }

    public static KsdScheduleType fromCode(String code) {
        return Arrays.stream(values())
            .filter(t -> t.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 일정 종류: " + code));
    }

    private static LinkedHashMap<String, String> om(String... kv) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }
}
