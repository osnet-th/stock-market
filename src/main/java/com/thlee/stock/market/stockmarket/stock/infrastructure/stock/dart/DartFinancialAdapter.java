package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart;

import com.thlee.stock.market.stockmarket.stock.domain.model.*;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockFinancialPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class DartFinancialAdapter implements StockFinancialPort {

    private final DartApiClient dartApiClient;
    private final DartCorpCodeCache corpCodeCache;

    @Override
    public List<FinancialAccount> getFinancialAccounts(String stockCode, String year, String reportCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartSinglAcntItem> response = dartApiClient.fetchSingleAccount(corpCode, year, reportCode);
        return toList(response, item -> toFinancialAccount(item, stockCode));
    }

    @Override
    public List<FinancialIndex> getFinancialIndices(String stockCode, String year, String reportCode, String indexClassCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartSinglIndxItem> response = dartApiClient.fetchSingleFinancialIndex(corpCode, year, reportCode, indexClassCode);
        return toList(response, this::toFinancialIndex);
    }

    @Override
    public List<FullFinancialStatement> getFullFinancialStatements(String stockCode, String year, String reportCode, String fsDiv) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartSinglAcntAllItem> response = dartApiClient.fetchSingleFullFinancial(corpCode, year, reportCode, fsDiv);
        return toList(response, this::toFullFinancialStatement);
    }

    @Override
    public List<StockQuantity> getStockQuantities(String stockCode, String year, String reportCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartStockTotqyItem> response = dartApiClient.fetchStockTotalQuantity(corpCode, year, reportCode);
        return toList(response, this::toStockQuantity);
    }

    @Override
    public List<DividendInfo> getDividendInfos(String stockCode, String year, String reportCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartAlotMatterItem> response = dartApiClient.fetchDividendInfo(corpCode, year, reportCode);
        return toList(response, this::toDividendInfo);
    }

    @Override
    public List<Lawsuit> getLawsuits(String stockCode, String startDate, String endDate) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartLawsuitItem> response = dartApiClient.fetchLawsuits(corpCode, startDate, endDate);
        return toList(response, this::toLawsuit);
    }

    @Override
    public List<FundUsage> getPrivateFundUsages(String stockCode, String year, String reportCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartPrivateFundItem> response = dartApiClient.fetchPrivateFundUsage(corpCode, year, reportCode);
        return toList(response, this::toPrivateFundUsage);
    }

    @Override
    public List<FundUsage> getPublicFundUsages(String stockCode, String year, String reportCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartPublicFundItem> response = dartApiClient.fetchPublicFundUsage(corpCode, year, reportCode);
        return toList(response, this::toPublicFundUsage);
    }

    @Override
    public List<FinancialAccount> getMultiFinancialAccounts(List<String> stockCodes, String year, String reportCode) {
        String corpCodes = corpCodeCache.getCorpCodes(stockCodes);
        DartApiResponse<DartMultiAcntItem> response = dartApiClient.fetchMultiAccount(corpCodes, year, reportCode);
        return toList(response, this::toMultiFinancialAccount);
    }

    @Override
    public List<FinancialIndex> getMultiFinancialIndices(List<String> stockCodes, String year, String reportCode, String indexClassCode) {
        String corpCodes = corpCodeCache.getCorpCodes(stockCodes);
        DartApiResponse<DartCmpnyIndxItem> response = dartApiClient.fetchMultiFinancialIndex(corpCodes, year, reportCode, indexClassCode);
        return toList(response, this::toMultiFinancialIndex);
    }

    // === 변환 헬퍼 ===

    private <T, R> List<R> toList(DartApiResponse<T> response, Function<T, R> mapper) {
        if (response == null || response.getList() == null) {
            return Collections.emptyList();
        }
        return response.getList().stream().map(mapper).toList();
    }

    private FinancialAccount toFinancialAccount(DartSinglAcntItem item, String stockCode) {
        return new FinancialAccount(
                stockCode,
                item.getAccountNm(),
                item.getFsDiv(),
                item.getFsNm(),
                item.getSjDiv(),
                item.getSjNm(),
                item.getThstrmNm(),
                item.getThstrmAmount(),
                item.getFrmtrmNm(),
                item.getFrmtrmAmount(),
                item.getBfefrmtrmNm(),
                item.getBfefrmtrmAmount(),
                item.getCurrency()
        );
    }

    private FinancialAccount toMultiFinancialAccount(DartMultiAcntItem item) {
        return new FinancialAccount(
                item.getStockCode(),
                item.getAccountNm(),
                item.getFsDiv(),
                item.getFsNm(),
                item.getSjDiv(),
                item.getSjNm(),
                item.getThstrmNm(),
                item.getThstrmAmount(),
                item.getFrmtrmNm(),
                item.getFrmtrmAmount(),
                item.getBfefrmtrmNm(),
                item.getBfefrmtrmAmount(),
                item.getCurrency()
        );
    }

    private FinancialIndex toFinancialIndex(DartSinglIndxItem item) {
        return new FinancialIndex(
                item.getStockCode(),
                item.getIdxClCode(),
                item.getIdxClNm(),
                item.getIdxCode(),
                item.getIdxNm(),
                item.getIdxVal()
        );
    }

    private FinancialIndex toMultiFinancialIndex(DartCmpnyIndxItem item) {
        return new FinancialIndex(
                item.getStockCode(),
                item.getIdxClCode(),
                item.getIdxClNm(),
                item.getIdxCode(),
                item.getIdxNm(),
                item.getIdxVal()
        );
    }

    private FullFinancialStatement toFullFinancialStatement(DartSinglAcntAllItem item) {
        return new FullFinancialStatement(
                item.getSjDiv(),
                item.getSjNm(),
                item.getAccountId(),
                item.getAccountNm(),
                item.getAccountDetail(),
                item.getThstrmNm(),
                item.getThstrmAmount(),
                item.getFrmtrmNm(),
                item.getFrmtrmAmount(),
                item.getCurrency()
        );
    }

    private StockQuantity toStockQuantity(DartStockTotqyItem item) {
        return new StockQuantity(
                item.getSe(),
                item.getIsuStockTotqy(),
                item.getNowToIsuStockTotqy(),
                item.getNowToDcrsStockTotqy(),
                item.getRedc(),
                item.getProfitIncnr(),
                item.getRdmstkRepy(),
                item.getEtc(),
                item.getIstcTotqy(),
                item.getTesstkCo(),
                item.getDistbStockCo()
        );
    }

    private DividendInfo toDividendInfo(DartAlotMatterItem item) {
        return new DividendInfo(
                item.getSe(),
                item.getStockKnd(),
                item.getThstrm(),
                item.getFrmtrm(),
                item.getLwfr()
        );
    }

    private Lawsuit toLawsuit(DartLawsuitItem item) {
        return new Lawsuit(
                item.getIcnm(),
                item.getAcAp(),
                item.getRqCn(),
                item.getCpct(),
                item.getFtCtp(),
                item.getLgd(),
                item.getCfd()
        );
    }

    private FundUsage toPrivateFundUsage(DartPrivateFundItem item) {
        return new FundUsage(
                item.getSeNm(),
                item.getTm(),
                item.getPayDe(),
                item.getMtrptCptalUsePlanUseprps(),
                item.getMtrptCptalUsePlanPrcureAmount(),
                item.getRealCptalUseDtlsCn(),
                item.getRealCptalUseDtlsAmount(),
                item.getDffrncOccrrncResn()
        );
    }

    private FundUsage toPublicFundUsage(DartPublicFundItem item) {
        return new FundUsage(
                item.getSeNm(),
                item.getTm(),
                item.getPayDe(),
                item.getRsCptalUsePlanUseprps(),
                item.getRsCptalUsePlanPrcureAmount(),
                item.getRealCptalUseDtlsCn(),
                item.getRealCptalUseDtlsAmount(),
                item.getDffrncOccrrncResn()
        );
    }
}