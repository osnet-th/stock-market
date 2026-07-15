package com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.CompanyAnalysisReport;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.InvestmentGrades;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportManual;
import com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence.CompanyAnalysisReportEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * CompanyAnalysisReport ↔ Entity 변환. 가치평가 파라미터는 JSON 문자열로 직렬화한다.
 */
@Component
@RequiredArgsConstructor
public class CompanyAnalysisReportMapper {

    private final ValuationParamsJsonConverter paramsConverter;
    private final ReportManualJsonConverter manualConverter;

    public CompanyAnalysisReportEntity toEntity(CompanyAnalysisReport d) {
        InvestmentGrades grades = d.getGrades();
        return new CompanyAnalysisReportEntity(
                d.getId(), d.getUserId(), d.getStockCode(), d.getStockName(),
                manualConverter.toJson(d.getManual()),
                grades.assetUndervalue(), grades.earningsUndervalue(), grades.financialHealth(),
                grades.profitability(), grades.growth(), grades.businessCompetence(), grades.shareholderPolicy(),
                d.isDraft(), d.getDraftStep(),
                paramsConverter.toJson(d.getValuationParams()), d.getSnapshotJson(), d.getSnapshotAt(),
                d.getCreatedAt(), d.getUpdatedAt()
        );
    }

    public CompanyAnalysisReport toDomain(CompanyAnalysisReportEntity e) {
        return new CompanyAnalysisReport(
                e.getId(), e.getUserId(), e.getStockCode(), e.getStockName(),
                manualConverter.fromJson(e.getManualJson()), toGrades(e),
                paramsConverter.fromJson(e.getValuationParamsJson()),
                e.isDraft(), e.getDraftStep(),
                e.getSnapshotJson(), e.getSnapshotAt(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    private InvestmentGrades toGrades(CompanyAnalysisReportEntity e) {
        return new InvestmentGrades(
                e.getGradeAssetUndervalue(), e.getGradeEarningsUndervalue(), e.getGradeFinancialHealth(),
                e.getGradeProfitability(), e.getGradeGrowth(), e.getGradeBusinessCompetence(),
                e.getGradeShareholderPolicy());
    }
}
