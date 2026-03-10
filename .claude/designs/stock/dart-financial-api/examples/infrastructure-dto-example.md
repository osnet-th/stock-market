# DART 응답 DTO 구현 예시

## DartApiResponse (공통 응답 Wrapper)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class DartApiResponse<T> {

    @JsonProperty("status")
    private String status;

    @JsonProperty("message")
    private String message;

    @JsonProperty("list")
    private List<T> list;

    public boolean isSuccess() {
        return "000".equals(status);
    }
}
```

## DartSinglAcntItem (단일회사 재무계정)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartSinglAcntItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("bsns_year")
    private String bsnsYear;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("reprt_code")
    private String reprtCode;

    @JsonProperty("account_nm")
    private String accountNm;

    @JsonProperty("fs_div")
    private String fsDiv;

    @JsonProperty("fs_nm")
    private String fsNm;

    @JsonProperty("sj_div")
    private String sjDiv;

    @JsonProperty("sj_nm")
    private String sjNm;

    @JsonProperty("thstrm_nm")
    private String thstrmNm;

    @JsonProperty("thstrm_amount")
    private String thstrmAmount;

    @JsonProperty("frmtrm_nm")
    private String frmtrmNm;

    @JsonProperty("frmtrm_amount")
    private String frmtrmAmount;

    @JsonProperty("bfefrmtrm_nm")
    private String bfefrmtrmNm;

    @JsonProperty("bfefrmtrm_amount")
    private String bfefrmtrmAmount;

    @JsonProperty("ord")
    private String ord;

    @JsonProperty("currency")
    private String currency;
}
```

## DartMultiAcntItem (다중회사 재무계정)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartMultiAcntItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("bsns_year")
    private String bsnsYear;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("reprt_code")
    private String reprtCode;

    @JsonProperty("account_nm")
    private String accountNm;

    @JsonProperty("fs_div")
    private String fsDiv;

    @JsonProperty("fs_nm")
    private String fsNm;

    @JsonProperty("sj_div")
    private String sjDiv;

    @JsonProperty("sj_nm")
    private String sjNm;

    @JsonProperty("thstrm_nm")
    private String thstrmNm;

    @JsonProperty("thstrm_dt")
    private String thstrmDt;

    @JsonProperty("thstrm_amount")
    private String thstrmAmount;

    @JsonProperty("thstrm_add_amount")
    private String thstrmAddAmount;

    @JsonProperty("frmtrm_nm")
    private String frmtrmNm;

    @JsonProperty("frmtrm_dt")
    private String frmtrmDt;

    @JsonProperty("frmtrm_amount")
    private String frmtrmAmount;

    @JsonProperty("frmtrm_add_amount")
    private String frmtrmAddAmount;

    @JsonProperty("bfefrmtrm_nm")
    private String bfefrmtrmNm;

    @JsonProperty("bfefrmtrm_dt")
    private String bfefrmtrmDt;

    @JsonProperty("bfefrmtrm_amount")
    private String bfefrmtrmAmount;

    @JsonProperty("ord")
    private String ord;

    @JsonProperty("currency")
    private String currency;
}
```

## DartSinglIndxItem (단일회사 재무지표)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartSinglIndxItem {

    @JsonProperty("reprt_code")
    private String reprtCode;

    @JsonProperty("bsns_year")
    private String bsnsYear;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("stlm_dt")
    private String stlmDt;

    @JsonProperty("idx_cl_code")
    private String idxClCode;

    @JsonProperty("idx_cl_nm")
    private String idxClNm;

    @JsonProperty("idx_code")
    private String idxCode;

    @JsonProperty("idx_nm")
    private String idxNm;

    @JsonProperty("idx_val")
    private String idxVal;
}
```

## DartCmpnyIndxItem (다중회사 재무지표)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartCmpnyIndxItem {

    @JsonProperty("reprt_code")
    private String reprtCode;

    @JsonProperty("bsns_year")
    private String bsnsYear;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("stock_code")
    private String stockCode;

    @JsonProperty("stlm_dt")
    private String stlmDt;

    @JsonProperty("idx_cl_code")
    private String idxClCode;

    @JsonProperty("idx_cl_nm")
    private String idxClNm;

    @JsonProperty("idx_code")
    private String idxCode;

    @JsonProperty("idx_nm")
    private String idxNm;

    @JsonProperty("idx_val")
    private String idxVal;
}
```

## DartSinglAcntAllItem (단일회사 전체 재무제표)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartSinglAcntAllItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("reprt_code")
    private String reprtCode;

    @JsonProperty("bsns_year")
    private String bsnsYear;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("sj_div")
    private String sjDiv;

    @JsonProperty("sj_nm")
    private String sjNm;

    @JsonProperty("account_id")
    private String accountId;

    @JsonProperty("account_nm")
    private String accountNm;

    @JsonProperty("account_detail")
    private String accountDetail;

    @JsonProperty("thstrm_nm")
    private String thstrmNm;

    @JsonProperty("thstrm_amount")
    private String thstrmAmount;

    @JsonProperty("frmtrm_nm")
    private String frmtrmNm;

    @JsonProperty("frmtrm_amount")
    private String frmtrmAmount;

    @JsonProperty("ord")
    private String ord;

    @JsonProperty("currency")
    private String currency;
}
```

## DartStockTotqyItem (주식의 총수 현황)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartStockTotqyItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("se")
    private String se;

    @JsonProperty("isu_stock_totqy")
    private String isuStockTotqy;

    @JsonProperty("now_to_isu_stock_totqy")
    private String nowToIsuStockTotqy;

    @JsonProperty("now_to_dcrs_stock_totqy")
    private String nowToDcrsStockTotqy;

    @JsonProperty("redc")
    private String redc;

    @JsonProperty("profit_incnr")
    private String profitIncnr;

    @JsonProperty("rdmstk_repy")
    private String rdmstkRepy;

    @JsonProperty("etc")
    private String etc;

    @JsonProperty("istc_totqy")
    private String istcTotqy;

    @JsonProperty("tesstk_co")
    private String tesstkCo;

    @JsonProperty("distb_stock_co")
    private String distbStockCo;

    @JsonProperty("stlm_dt")
    private String stlmDt;
}
```

## DartAlotMatterItem (배당에 관한 사항)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartAlotMatterItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("se")
    private String se;

    @JsonProperty("stock_knd")
    private String stockKnd;

    @JsonProperty("thstrm")
    private String thstrm;

    @JsonProperty("frmtrm")
    private String frmtrm;

    @JsonProperty("lwfr")
    private String lwfr;

    @JsonProperty("stlm_dt")
    private String stlmDt;
}
```

## DartLawsuitItem (소송 등의 제기)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DartLawsuitItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("icnm")
    private String icnm;

    @JsonProperty("ac_ap")
    private String acAp;

    @JsonProperty("rq_cn")
    private String rqCn;

    @JsonProperty("cpct")
    private String cpct;

    @JsonProperty("ft_ctp")
    private String ftCtp;

    @JsonProperty("lgd")
    private String lgd;

    @JsonProperty("cfd")
    private String cfd;
}
```

## DartPrivateFundItem (사모자금 사용내역)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사모자금 사용내역 응답 DTO
 * - 2018.01.18 이전: payAmount, cptalUsePlan, realCptalUseSttus
 * - 2018.01.19 이후: mtrptCptalUsePlan*, realCptalUseDtls*
 */
@Getter
@NoArgsConstructor
public class DartPrivateFundItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("se_nm")
    private String seNm;

    @JsonProperty("tm")
    private String tm;

    @JsonProperty("pay_de")
    private String payDe;

    // 2018.01.18 이전 필드
    @JsonProperty("pay_amount")
    private String payAmount;

    @JsonProperty("cptal_use_plan")
    private String cptalUsePlan;

    @JsonProperty("real_cptal_use_sttus")
    private String realCptalUseSttus;

    // 2018.01.19 이후 필드
    @JsonProperty("mtrpt_cptal_use_plan_useprps")
    private String mtrptCptalUsePlanUseprps;

    @JsonProperty("mtrpt_cptal_use_plan_prcure_amount")
    private String mtrptCptalUsePlanPrcureAmount;

    @JsonProperty("real_cptal_use_dtls_cn")
    private String realCptalUseDtlsCn;

    @JsonProperty("real_cptal_use_dtls_amount")
    private String realCptalUseDtlsAmount;

    @JsonProperty("dffrnc_occrrnc_resn")
    private String dffrncOccrrncResn;

    @JsonProperty("stlm_dt")
    private String stlmDt;
}
```

## DartPublicFundItem (공모자금 사용내역)

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공모자금 사용내역 응답 DTO
 * - 2018.01.18 이전: payAmount, onDclrtCptalUsePlan, realCptalUseSttus
 * - 2018.01.19 이후: rsCptalUsePlan*, realCptalUseDtls*
 */
@Getter
@NoArgsConstructor
public class DartPublicFundItem {

    @JsonProperty("rcept_no")
    private String rceptNo;

    @JsonProperty("corp_cls")
    private String corpCls;

    @JsonProperty("corp_code")
    private String corpCode;

    @JsonProperty("corp_name")
    private String corpName;

    @JsonProperty("se_nm")
    private String seNm;

    @JsonProperty("tm")
    private String tm;

    @JsonProperty("pay_de")
    private String payDe;

    // 2018.01.18 이전 필드
    @JsonProperty("pay_amount")
    private String payAmount;

    @JsonProperty("on_dclrt_cptal_use_plan")
    private String onDclrtCptalUsePlan;

    @JsonProperty("real_cptal_use_sttus")
    private String realCptalUseSttus;

    // 2018.01.19 이후 필드
    @JsonProperty("rs_cptal_use_plan_useprps")
    private String rsCptalUsePlanUseprps;

    @JsonProperty("rs_cptal_use_plan_prcure_amount")
    private String rsCptalUsePlanPrcureAmount;

    @JsonProperty("real_cptal_use_dtls_cn")
    private String realCptalUseDtlsCn;

    @JsonProperty("real_cptal_use_dtls_amount")
    private String realCptalUseDtlsAmount;

    @JsonProperty("dffrnc_occrrnc_resn")
    private String dffrncOccrrncResn;

    @JsonProperty("stlm_dt")
    private String stlmDt;
}
```