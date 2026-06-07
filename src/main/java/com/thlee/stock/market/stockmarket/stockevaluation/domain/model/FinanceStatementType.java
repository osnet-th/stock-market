package com.thlee.stock.market.stockmarket.stockevaluation.domain.model;

import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * KIS 국내주식 재무 API 종류.
 * URL 코드, API 경로, tr_id, 응답 필드 라벨(kis_key → 한글, 순서 유지)을 보유한다.
 * 7종 모두 공통 파라미터(FID_DIV_CLS_CODE, FID_COND_MRKT_DIV_CODE=J, FID_INPUT_ISCD)와 단일 output(시계열) 구조.
 */
@Getter
public enum FinanceStatementType {

    BALANCE_SHEET("balance-sheet", "/uapi/domestic-stock/v1/finance/balance-sheet", "FHKST66430100",
        om("stac_yymm", "결산년월", "cras", "유동자산", "fxas", "고정자산", "total_aset", "자산총계",
           "flow_lblt", "유동부채", "fix_lblt", "고정부채", "total_lblt", "부채총계", "cpfn", "자본금",
           "cfp_surp", "자본잉여금", "prfi_surp", "이익잉여금", "total_cptl", "자본총계")),

    INCOME_STATEMENT("income-statement", "/uapi/domestic-stock/v1/finance/income-statement", "FHKST66430200",
        om("stac_yymm", "결산년월", "sale_account", "매출액", "sale_cost", "매출원가", "sale_totl_prfi", "매출총이익",
           "depr_cost", "감가상각비", "sell_mang", "판매관리비", "bsop_prti", "영업이익", "bsop_non_ernn", "영업외수익",
           "bsop_non_expn", "영업외비용", "op_prfi", "경상이익", "spec_prfi", "특별이익", "spec_loss", "특별손실",
           "thtr_ntin", "당기순이익")),

    FINANCIAL_RATIO("financial-ratio", "/uapi/domestic-stock/v1/finance/financial-ratio", "FHKST66430300",
        om("stac_yymm", "결산년월", "grs", "매출액증가율", "bsop_prfi_inrt", "영업이익증가율", "ntin_inrt", "순이익증가율",
           "roe_val", "ROE", "eps", "EPS", "sps", "주당매출액", "bps", "BPS", "rsrv_rate", "유보비율", "lblt_rate", "부채비율")),

    PROFIT_RATIO("profit-ratio", "/uapi/domestic-stock/v1/finance/profit-ratio", "FHKST66430400",
        om("stac_yymm", "결산년월", "cptl_ntin_rate", "총자본순이익율", "self_cptl_ntin_inrt", "자기자본순이익율",
           "sale_ntin_rate", "매출액순이익율", "sale_totl_rate", "매출액총이익율")),

    OTHER_MAJOR_RATIOS("other-major-ratios", "/uapi/domestic-stock/v1/finance/other-major-ratios", "FHKST66430500",
        om("stac_yymm", "결산년월", "payout_rate", "배당성향", "eva", "EVA", "ebitda", "EBITDA", "ev_ebitda", "EV/EBITDA")),

    STABILITY_RATIO("stability-ratio", "/uapi/domestic-stock/v1/finance/stability-ratio", "FHKST66430600",
        om("stac_yymm", "결산년월", "lblt_rate", "부채비율", "bram_depn", "차입금의존도", "crnt_rate", "유동비율", "quck_rate", "당좌비율")),

    GROWTH_RATIO("growth-ratio", "/uapi/domestic-stock/v1/finance/growth-ratio", "FHKST66430800",
        om("stac_yymm", "결산년월", "grs", "매출액증가율", "bsop_prfi_inrt", "영업이익증가율", "equt_inrt", "자기자본증가율",
           "totl_aset_inrt", "총자산증가율"));

    private final String code;
    private final String path;
    private final String trId;
    private final LinkedHashMap<String, String> labels;

    FinanceStatementType(String code, String path, String trId, LinkedHashMap<String, String> labels) {
        this.code = code;
        this.path = path;
        this.trId = trId;
        this.labels = labels;
    }

    public static FinanceStatementType fromCode(String code) {
        return Arrays.stream(values())
            .filter(t -> t.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 재무 항목: " + code));
    }

    private static LinkedHashMap<String, String> om(String... kv) {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }
}
