# DartFinancialService / 응답 DTO 구현 예시

## DartFinancialService

```java
package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.*;
import com.thlee.stock.market.stockmarket.stock.domain.service.DartFinancialPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DartFinancialService {

    private final DartFinancialPort dartFinancialPort;

    public List<FinancialAccountResponse> getSingleAccount(String corpCode, String bsnsYear, String reprtCode) {
        return dartFinancialPort.getSingleAccount(corpCode, bsnsYear, reprtCode).stream()
                .map(FinancialAccountResponse::from)
                .toList();
    }

    public List<FinancialAccountResponse> getMultiAccount(String corpCodes, String bsnsYear, String reprtCode) {
        return dartFinancialPort.getMultiAccount(corpCodes, bsnsYear, reprtCode).stream()
                .map(FinancialAccountResponse::from)
                .toList();
    }

    public List<FinancialIndexResponse> getSingleFinancialIndex(
            String corpCode, String bsnsYear, String reprtCode, String idxClCode) {
        return dartFinancialPort.getSingleFinancialIndex(corpCode, bsnsYear, reprtCode, idxClCode).stream()
                .map(FinancialIndexResponse::from)
                .toList();
    }

    public List<FinancialIndexResponse> getMultiFinancialIndex(
            String corpCodes, String bsnsYear, String reprtCode, String idxClCode) {
        return dartFinancialPort.getMultiFinancialIndex(corpCodes, bsnsYear, reprtCode, idxClCode).stream()
                .map(FinancialIndexResponse::from)
                .toList();
    }

    public List<FullFinancialStatementResponse> getFullFinancialStatement(
            String corpCode, String bsnsYear, String reprtCode, String fsDiv) {
        return dartFinancialPort.getFullFinancialStatement(corpCode, bsnsYear, reprtCode, fsDiv).stream()
                .map(FullFinancialStatementResponse::from)
                .toList();
    }

    public List<StockTotalQuantityResponse> getStockTotalQuantity(String corpCode, String bsnsYear, String reprtCode) {
        return dartFinancialPort.getStockTotalQuantity(corpCode, bsnsYear, reprtCode).stream()
                .map(StockTotalQuantityResponse::from)
                .toList();
    }

    public List<DividendInfoResponse> getDividendInfo(String corpCode, String bsnsYear, String reprtCode) {
        return dartFinancialPort.getDividendInfo(corpCode, bsnsYear, reprtCode).stream()
                .map(DividendInfoResponse::from)
                .toList();
    }

    public List<LawsuitInfoResponse> getLawsuits(String corpCode, String bgnDe, String endDe) {
        return dartFinancialPort.getLawsuits(corpCode, bgnDe, endDe).stream()
                .map(LawsuitInfoResponse::from)
                .toList();
    }

    public List<FundUsageResponse> getPrivateFundUsage(String corpCode, String bsnsYear, String reprtCode) {
        return dartFinancialPort.getPrivateFundUsage(corpCode, bsnsYear, reprtCode).stream()
                .map(FundUsageResponse::from)
                .toList();
    }

    public List<FundUsageResponse> getPublicFundUsage(String corpCode, String bsnsYear, String reprtCode) {
        return dartFinancialPort.getPublicFundUsage(corpCode, bsnsYear, reprtCode).stream()
                .map(FundUsageResponse::from)
                .toList();
    }
}
```

## 응답 DTO

### FinancialAccountResponse (대표 예시 - 전체 구현)

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.FinancialAccount;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FinancialAccountResponse {

    private final String rceptNo;
    private final String bsnsYear;
    private final String stockCode;
    private final String reprtCode;
    private final String accountNm;
    private final String fsDiv;
    private final String fsNm;
    private final String sjDiv;
    private final String sjNm;
    private final String thstrmNm;
    private final String thstrmDt;
    private final String thstrmAmount;
    private final String thstrmAddAmount;
    private final String frmtrmNm;
    private final String frmtrmDt;
    private final String frmtrmAmount;
    private final String frmtrmAddAmount;
    private final String bfefrmtrmNm;
    private final String bfefrmtrmDt;
    private final String bfefrmtrmAmount;
    private final String ord;
    private final String currency;

    public static FinancialAccountResponse from(FinancialAccount account) {
        return new FinancialAccountResponse(
                account.rceptNo(), account.bsnsYear(), account.stockCode(), account.reprtCode(),
                account.accountNm(), account.fsDiv(), account.fsNm(),
                account.sjDiv(), account.sjNm(),
                account.thstrmNm(), account.thstrmDt(), account.thstrmAmount(), account.thstrmAddAmount(),
                account.frmtrmNm(), account.frmtrmDt(), account.frmtrmAmount(), account.frmtrmAddAmount(),
                account.bfefrmtrmNm(), account.bfefrmtrmDt(), account.bfefrmtrmAmount(),
                account.ord(), account.currency()
        );
    }
}
```

### FinancialIndexResponse

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.FinancialIndex;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FinancialIndexResponse {

    private final String reprtCode;
    private final String bsnsYear;
    private final String corpCode;
    private final String stockCode;
    private final String stlmDt;
    private final String idxClCode;
    private final String idxClNm;
    private final String idxCode;
    private final String idxNm;
    private final String idxVal;

    public static FinancialIndexResponse from(FinancialIndex index) {
        return new FinancialIndexResponse(
                index.reprtCode(), index.bsnsYear(), index.corpCode(), index.stockCode(),
                index.stlmDt(), index.idxClCode(), index.idxClNm(),
                index.idxCode(), index.idxNm(), index.idxVal()
        );
    }
}
```

### FullFinancialStatementResponse

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.FullFinancialStatement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FullFinancialStatementResponse {

    private final String rceptNo;
    private final String reprtCode;
    private final String bsnsYear;
    private final String corpCode;
    private final String sjDiv;
    private final String sjNm;
    private final String accountId;
    private final String accountNm;
    private final String accountDetail;
    private final String thstrmNm;
    private final String thstrmAmount;
    private final String frmtrmNm;
    private final String frmtrmAmount;
    private final String ord;
    private final String currency;

    public static FullFinancialStatementResponse from(FullFinancialStatement statement) {
        return new FullFinancialStatementResponse(
                statement.rceptNo(), statement.reprtCode(), statement.bsnsYear(), statement.corpCode(),
                statement.sjDiv(), statement.sjNm(), statement.accountId(), statement.accountNm(),
                statement.accountDetail(), statement.thstrmNm(), statement.thstrmAmount(),
                statement.frmtrmNm(), statement.frmtrmAmount(), statement.ord(), statement.currency()
        );
    }
}
```

### StockTotalQuantityResponse

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.StockTotalQuantity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockTotalQuantityResponse {

    private final String rceptNo;
    private final String corpCls;
    private final String corpCode;
    private final String corpName;
    private final String se;
    private final String isuStockTotqy;
    private final String nowToIsuStockTotqy;
    private final String nowToDcrsStockTotqy;
    private final String redc;
    private final String profitIncnr;
    private final String rdmstkRepy;
    private final String etc;
    private final String istcTotqy;
    private final String tesstkCo;
    private final String distbStockCo;
    private final String stlmDt;

    public static StockTotalQuantityResponse from(StockTotalQuantity stq) {
        return new StockTotalQuantityResponse(
                stq.rceptNo(), stq.corpCls(), stq.corpCode(), stq.corpName(),
                stq.se(), stq.isuStockTotqy(), stq.nowToIsuStockTotqy(),
                stq.nowToDcrsStockTotqy(), stq.redc(), stq.profitIncnr(),
                stq.rdmstkRepy(), stq.etc(), stq.istcTotqy(),
                stq.tesstkCo(), stq.distbStockCo(), stq.stlmDt()
        );
    }
}
```

### DividendInfoResponse

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.DividendInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DividendInfoResponse {

    private final String rceptNo;
    private final String corpCls;
    private final String corpCode;
    private final String corpName;
    private final String se;
    private final String stockKnd;
    private final String thstrm;
    private final String frmtrm;
    private final String lwfr;
    private final String stlmDt;

    public static DividendInfoResponse from(DividendInfo info) {
        return new DividendInfoResponse(
                info.rceptNo(), info.corpCls(), info.corpCode(), info.corpName(),
                info.se(), info.stockKnd(), info.thstrm(), info.frmtrm(),
                info.lwfr(), info.stlmDt()
        );
    }
}
```

### LawsuitInfoResponse

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.LawsuitInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LawsuitInfoResponse {

    private final String rceptNo;
    private final String corpCls;
    private final String corpCode;
    private final String corpName;
    private final String icnm;
    private final String acAp;
    private final String rqCn;
    private final String cpct;
    private final String ftCtp;
    private final String lgd;
    private final String cfd;

    public static LawsuitInfoResponse from(LawsuitInfo info) {
        return new LawsuitInfoResponse(
                info.rceptNo(), info.corpCls(), info.corpCode(), info.corpName(),
                info.icnm(), info.acAp(), info.rqCn(), info.cpct(),
                info.ftCtp(), info.lgd(), info.cfd()
        );
    }
}
```

### FundUsageResponse

```java
package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.FundUsage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FundUsageResponse {

    private final String rceptNo;
    private final String corpCls;
    private final String corpCode;
    private final String corpName;
    private final String seNm;
    private final String tm;
    private final String payDe;
    private final String payAmount;
    private final String capitalUsePlan;
    private final String actualCapitalUsage;
    private final String planPurpose;
    private final String planAmount;
    private final String actualDetailContent;
    private final String actualDetailAmount;
    private final String differenceReason;
    private final String stlmDt;

    public static FundUsageResponse from(FundUsage usage) {
        return new FundUsageResponse(
                usage.rceptNo(), usage.corpCls(), usage.corpCode(), usage.corpName(),
                usage.seNm(), usage.tm(), usage.payDe(),
                usage.payAmount(), usage.capitalUsePlan(), usage.actualCapitalUsage(),
                usage.planPurpose(), usage.planAmount(),
                usage.actualDetailContent(), usage.actualDetailAmount(),
                usage.differenceReason(), usage.stlmDt()
        );
    }
}
```