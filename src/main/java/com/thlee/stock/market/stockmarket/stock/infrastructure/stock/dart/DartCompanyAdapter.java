package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart;

import com.thlee.stock.market.stockmarket.stock.domain.model.BulkHoldingReport;
import com.thlee.stock.market.stockmarket.stock.domain.model.CompanyProfile;
import com.thlee.stock.market.stockmarket.stock.domain.model.MajorShareholder;
import com.thlee.stock.market.stockmarket.stock.domain.service.CompanyDisclosurePort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartApiResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartCompanyResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartHyslrSttusItem;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.dto.DartMajorstockItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class DartCompanyAdapter implements CompanyDisclosurePort {

    private final DartApiClient dartApiClient;
    private final DartCorpCodeCache corpCodeCache;

    @Override
    public Optional<CompanyProfile> getCompanyProfile(String stockCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartCompanyResponse response = dartApiClient.fetchCompanyProfile(corpCode);
        return Optional.ofNullable(response).map(this::toCompanyProfile);
    }

    @Override
    public List<MajorShareholder> getMajorShareholders(String stockCode, String year, String reportCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartHyslrSttusItem> response =
                dartApiClient.fetchMajorShareholderStatus(corpCode, year, reportCode);
        return toList(response, item -> toMajorShareholder(item, year));
    }

    @Override
    public List<BulkHoldingReport> getBulkHoldingReports(String stockCode) {
        String corpCode = corpCodeCache.getCorpCode(stockCode);
        DartApiResponse<DartMajorstockItem> response = dartApiClient.fetchBulkHoldingReports(corpCode);
        return toList(response, this::toBulkHoldingReport);
    }

    // === 변환 헬퍼 ===

    private <T, R> List<R> toList(DartApiResponse<T> response, Function<T, R> mapper) {
        if (response == null || response.getList() == null) {
            return Collections.emptyList();
        }
        return response.getList().stream().map(mapper).toList();
    }

    private CompanyProfile toCompanyProfile(DartCompanyResponse res) {
        return new CompanyProfile(
                res.getCorpCode(),
                res.getCorpName(),
                res.getCorpNameEng(),
                res.getStockName(),
                res.getStockCode(),
                res.getCeoNm(),
                res.getCorpCls(),
                res.getJurirNo(),
                res.getBizrNo(),
                res.getAdres(),
                res.getHmUrl(),
                res.getIrUrl(),
                res.getPhnNo(),
                res.getFaxNo(),
                res.getIndutyCode(),
                res.getEstDt(),
                res.getAccMt()
        );
    }

    private MajorShareholder toMajorShareholder(DartHyslrSttusItem item, String year) {
        return new MajorShareholder(
                year,
                item.getStockKnd(),
                item.getNm(),
                item.getRelate(),
                item.getBsisPosesnStockCo(),
                item.getBsisPosesnStockQotaRt(),
                item.getTrmendPosesnStockCo(),
                item.getTrmendPosesnStockQotaRt(),
                item.getRm(),
                item.getStlmDt()
        );
    }

    private BulkHoldingReport toBulkHoldingReport(DartMajorstockItem item) {
        return new BulkHoldingReport(
                item.getRceptNo(),
                item.getRceptDt(),
                item.getReportTp(),
                item.getRepror(),
                item.getStkqy(),
                item.getStkqyIrds(),
                item.getStkrt(),
                item.getStkrtIrds(),
                item.getCtrStkqy(),
                item.getCtrStkrt(),
                item.getReportResn()
        );
    }
}
