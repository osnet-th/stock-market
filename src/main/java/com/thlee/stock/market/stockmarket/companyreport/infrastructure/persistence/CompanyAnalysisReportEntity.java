package com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportGrade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_analysis_report",
        indexes = {
                @Index(name = "idx_company_report_user_stock", columnList = "user_id, stock_code"),
                @Index(name = "idx_company_report_user_updated", columnList = "user_id, updated_at")
        })
@Getter
public class CompanyAnalysisReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(name = "stock_name", length = 100)
    private String stockName;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_asset_undervalue", length = 1)
    private ReportGrade gradeAssetUndervalue;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_earnings_undervalue", length = 1)
    private ReportGrade gradeEarningsUndervalue;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_financial_health", length = 1)
    private ReportGrade gradeFinancialHealth;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_profitability", length = 1)
    private ReportGrade gradeProfitability;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_growth", length = 1)
    private ReportGrade gradeGrowth;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_business_competence", length = 1)
    private ReportGrade gradeBusinessCompetence;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_shareholder_policy", length = 1)
    private ReportGrade gradeShareholderPolicy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manual", columnDefinition = "jsonb")
    private String manualJson;

    @Column(name = "draft", nullable = false, columnDefinition = "boolean default false not null")
    private boolean draft;

    @Column(name = "draft_step")
    private Integer draftStep;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valuation_params", columnDefinition = "jsonb", nullable = false)
    private String valuationParamsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", columnDefinition = "jsonb")
    private String snapshotJson;

    @Column(name = "snapshot_at")
    private LocalDateTime snapshotAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CompanyAnalysisReportEntity() {
    }

    public CompanyAnalysisReportEntity(Long id, Long userId, String stockCode, String stockName,
                                       String manualJson,
                                       ReportGrade gradeAssetUndervalue, ReportGrade gradeEarningsUndervalue,
                                       ReportGrade gradeFinancialHealth, ReportGrade gradeProfitability,
                                       ReportGrade gradeGrowth, ReportGrade gradeBusinessCompetence,
                                       ReportGrade gradeShareholderPolicy,
                                       boolean draft, Integer draftStep,
                                       String valuationParamsJson, String snapshotJson, LocalDateTime snapshotAt,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.manualJson = manualJson;
        this.gradeAssetUndervalue = gradeAssetUndervalue;
        this.gradeEarningsUndervalue = gradeEarningsUndervalue;
        this.gradeFinancialHealth = gradeFinancialHealth;
        this.gradeProfitability = gradeProfitability;
        this.gradeGrowth = gradeGrowth;
        this.gradeBusinessCompetence = gradeBusinessCompetence;
        this.gradeShareholderPolicy = gradeShareholderPolicy;
        this.draft = draft;
        this.draftStep = draftStep;
        this.valuationParamsJson = valuationParamsJson;
        this.snapshotJson = snapshotJson;
        this.snapshotAt = snapshotAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
