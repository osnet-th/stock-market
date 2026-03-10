# DartFinancialMapper / DartFinancialAdapter 구현 예시

## DartFinancialMapper

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart;

import com.thlee.stock.market.stockmarket.stock.domain.model.*;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DartFinancialMapper {

    public static FinancialAccount toFinancialAccountFromSingle(DartSinglAcntItem item) {
        return new FinancialAccount(
                item.getRceptNo(), item.getBsnsYear(), item.getStockCode(), item.getReprtCode(),
                item.getAccountNm(), item.getFsDiv(), item.getFsNm(),
                item.getSjDiv(), item.getSjNm(),
                item.getThstrmNm(), null, item.getThstrmAmount(), null,
                item.getFrmtrmNm(), null, item.getFrmtrmAmount(), null,
                item.getBfefrmtrmNm(), null, item.getBfefrmtrmAmount(),
                item.getOrd(), item.getCurrency()
        );
    }

    public static FinancialAccount toFinancialAccountFromMulti(DartMultiAcntItem item) {
        return new FinancialAccount(
                item.getRceptNo(), item.getBsnsYear(), item.getStockCode(), item.getReprtCode(),
                item.getAccountNm(), item.getFsDiv(), item.getFsNm(),
                item.getSjDiv(), item.getSjNm(),
                item.getThstrmNm(), item.getThstrmDt(), item.getThstrmAmount(), item.getThstrmAddAmount(),
                item.getFrmtrmNm(), item.getFrmtrmDt(), item.getFrmtrmAmount(), item.getFrmtrmAddAmount(),
                item.getBfefrmtrmNm(), item.getBfefrmtrmDt(), item.getBfefrmtrmAmount(),
                item.getOrd(), item.getCurrency()
        );
    }

    public static FinancialIndex toFinancialIndex(DartSinglIndxItem item) {
        return new FinancialIndex(
                item.getReprtCode(), item.getBsnsYear(), item.getCorpCode(), item.getStockCode(),
                item.getStlmDt(), item.getIdxClCode(), item.getIdxClNm(),
                item.getIdxCode(), item.getIdxNm(), item.getIdxVal()
        );
    }

    public static FinancialIndex toFinancialIndexFromMulti(DartCmpnyIndxItem item) {
        return new FinancialIndex(
                item.getReprtCode(), item.getBsnsYear(), item.getCorpCode(), item.getStockCode(),
                item.getStlmDt(), item.getIdxClCode(), item.getIdxClNm(),
                item.getIdxCode(), item.getIdxNm(), item.getIdxVal()
        );
    }

    public static FullFinancialStatement toFullFinancialStatement(DartSinglAcntAllItem item) {
        return new FullFinancialStatement(
                item.getRceptNo(), item.getReprtCode(), item.getBsnsYear(), item.getCorpCode(),
                item.getSjDiv(), item.getSjNm(), item.getAccountId(), item.getAccountNm(),
                item.getAccountDetail(), item.getThstrmNm(), item.getThstrmAmount(),
                item.getFrmtrmNm(), item.getFrmtrmAmount(), item.getOrd(), item.getCurrency()
        );
    }

    public static StockTotalQuantity toStockTotalQuantity(DartStockTotqyItem item) {
        return new StockTotalQuantity(
                item.getRceptNo(), item.getCorpCls(), item.getCorpCode(), item.getCorpName(),
                item.getSe(), item.getIsuStockTotqy(), item.getNowToIsuStockTotqy(),
                item.getNowToDcrsStockTotqy(), item.getRedc(), item.getProfitIncnr(),
                item.getRdmstkRepy(), item.getEtc(), item.getIstcTotqy(),
                item.getTesstkCo(), item.getDistbStockCo(), item.getStlmDt()
        );
    }

    public static DividendInfo toDividendInfo(DartAlotMatterItem item) {
        return new DividendInfo(
                item.getRceptNo(), item.getCorpCls(), item.getCorpCode(), item.getCorpName(),
                item.getSe(), item.getStockKnd(), item.getThstrm(), item.getFrmtrm(),
                item.getLwfr(), item.getStlmDt()
        );
    }

    public static LawsuitInfo toLawsuitInfo(DartLawsuitItem item) {
        return new LawsuitInfo(
                item.getRceptNo(), item.getCorpCls(), item.getCorpCode(), item.getCorpName(),
                item.getIcnm(), item.getAcAp(), item.getRqCn(), item.getCpct(),
                item.getFtCtp(), item.getLgd(), item.getCfd()
        );
    }

    public static FundUsage toFundUsageFromPrivate(DartPrivateFundItem item) {
        return new FundUsage(
                item.getRceptNo(), item.getCorpCls(), item.getCorpCode(), item.getCorpName(),
                item.getSeNm(), item.getTm(), item.getPayDe(),
                item.getPayAmount(), item.getCptalUsePlan(), item.getRealCptalUseSttus(),
                item.getMtrptCptalUsePlanUseprps(), item.getMtrptCptalUsePlanPrcureAmount(),
                item.getRealCptalUseDtlsCn(), item.getRealCptalUseDtlsAmount(),
                item.getDffrncOccrrncResn(), item.getStlmDt()
        );
    }

    public static FundUsage toFundUsageFromPublic(DartPublicFundItem item) {
        return new FundUsage(
                item.getRceptNo(), item.getCorpCls(), item.getCorpCode(), item.getCorpName(),
                item.getSeNm(), item.getTm(), item.getPayDe(),
                item.getPayAmount(), item.getOnDclrtCptalUsePlan(), item.getRealCptalUseSttus(),
                item.getRsCptalUsePlanUseprps(), item.getRsCptalUsePlanPrcureAmount(),
                item.getRealCptalUseDtlsCn(), item.getRealCptalUseDtlsAmount(),
                item.getDffrncOccrrncResn(), item.getStlmDt()
        );
    }
}
```

## DartFinancialAdapter

```java
package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart;

import com.thlee.stock.market.stockmarket.stock.domain.model.*;
import com.thlee.stock.market.stockmarket.stock.domain.service.DartFinancialPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DartFinancialAdapter implements DartFinancialPort {

    private final DartApiClient dartApiClient;

    @Override
    public List<FinancialAccount> getSingleAccount(String corpCode, String bsnsYear, String reprtCode) {
        DartApiResponse<?> response = dartApiClient.fetchSingleAccount(corpCode, bsnsYear, reprtCode);
        return safeList(response).stream()
                .map(item -> DartFinancialMapper.toFinancialAccountFromSingle(
                        (com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartSinglAcntItem) item))
                .toList();
    }

    @Override
    public List<FinancialAccount> getMultiAccount(String corpCodes, String bsnsYear, String reprtCode) {
        var response = dartApiClient.fetchMultiAccount(corpCodes, bsnsYear, reprtCode);
        return safeList(response).stream()
                .map(DartFinancialMapper::toFinancialAccountFromMulti)
                .toList();
    }

    @Override
    public List<FinancialIndex> getSingleFinancialIndex(String corpCode, String bsnsYear, String reprtCode, String idxClCode) {
        var response = dartApiClient.fetchSingleFinancialIndex(corpCode, bsnsYear, reprtCode, idxClCode);
        return safeList(response).stream()
                .map(DartFinancialMapper::toFinancialIndex)
                .toList();
    }

    @Override
    public List<FinancialIndex> getMultiFinancialIndex(String corpCodes, String bsnsYear, String reprtCode, String idxClCode) {
        var response = dartApiClient.fetchMultiFinancialIndex(corpCodes, bsnsYear, reprtCode, idxClCode);
        return safeList(response).stream()
                .map(DartFinancialMapper::toFinancialIndexFromMulti)
                .toList();
    }

    @Override
    public List<FullFinancialStatement> getFullFinancialStatement(String corpCode, String bsnsYear, String reprtCode, String fsDiv) {
        var response = dartApiClient.fetchSingleFullFinancial(corpCode, bsnsYear, reprtCode, fsDiv);
        return safeList(response).stream()
                .map(DartFinancialMapper::toFullFinancialStatement)
                .toList();
    }

    @Override
    public List<StockTotalQuantity> getStockTotalQuantity(String corpCode, String bsnsYear, String reprtCode) {
        var response = dartApiClient.fetchStockTotalQuantity(corpCode, bsnsYear, reprtCode);
        return safeList(response).stream()
                .map(DartFinancialMapper::toStockTotalQuantity)
                .toList();
    }

    @Override
    public List<DividendInfo> getDividendInfo(String corpCode, String bsnsYear, String reprtCode) {
        var response = dartApiClient.fetchDividendInfo(corpCode, bsnsYear, reprtCode);
        return safeList(response).stream()
                .map(DartFinancialMapper::toDividendInfo)
                .toList();
    }

    @Override
    public List<LawsuitInfo> getLawsuits(String corpCode, String bgnDe, String endDe) {
        var response = dartApiClient.fetchLawsuits(corpCode, bgnDe, endDe);
        return safeList(response).stream()
                .map(DartFinancialMapper::toLawsuitInfo)
                .toList();
    }

    @Override
    public List<FundUsage> getPrivateFundUsage(String corpCode, String bsnsYear, String reprtCode) {
        var response = dartApiClient.fetchPrivateFundUsage(corpCode, bsnsYear, reprtCode);
        return safeList(response).stream()
                .map(DartFinancialMapper::toFundUsageFromPrivate)
                .toList();
    }

    @Override
    public List<FundUsage> getPublicFundUsage(String corpCode, String bsnsYear, String reprtCode) {
        var response = dartApiClient.fetchPublicFundUsage(corpCode, bsnsYear, reprtCode);
        return safeList(response).stream()
                .map(DartFinancialMapper::toFundUsageFromPublic)
                .toList();
    }

    private <T> List<T> safeList(DartApiResponse<T> response) {
        return response != null && response.getList() != null
                ? response.getList()
                : Collections.emptyList();
    }
}
```