package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 리포트 스냅샷 조립 유스케이스 — 종목코드 형태로 국가별 Assembler에 위임하는 라우터.
 * 국내(6자리 숫자)는 {@link KrReportSnapshotAssembler}, 미국(영문 티커)은 {@link UsReportSnapshotAssembler}.
 */
@Service
@RequiredArgsConstructor
public class CompanyReportSnapshotService {

    private final List<ReportSnapshotAssembler> assemblers;

    public ReportSnapshot assemble(String stockCode) {
        return assemblers.stream()
                .filter(assembler -> assembler.supports(stockCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 종목코드 형식입니다: " + stockCode))
                .assemble(stockCode);
    }
}
