package com.thlee.stock.market.stockmarket.companyreport.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 기업분석리포트 본체.
 *
 * <p>정량 데이터는 저장 시점 스냅샷 JSON(snapshotJson)으로 보존하고, 정성 분석({@link ReportManual},
 * 항목별 구조화 입력)과 투자판단({@link InvestmentGrades})은 사용자가 직접 입력한다.
 * 청산가치/DCF 결과는 저장하지 않고 {@link ValuationParams}와 스냅샷 원시 입력으로 조회 시 파생 계산한다.
 *
 * <p>다른 도메인은 stockCode(6자리)로만 참조한다. 동일 종목 복수 리포트를 허용한다(시점별 기록).
 */
@Getter
public class CompanyAnalysisReport {

    /** 위저드 작성 단계 범위 (1단계는 종목 선택이라 저장 대상 아님) */
    public static final int DRAFT_STEP_MIN = 2;
    public static final int DRAFT_STEP_MAX = 7;

    private Long id;
    private final Long userId;
    private final String stockCode;
    private String stockName;
    private ReportManual manual;
    private InvestmentGrades grades;
    private ValuationParams valuationParams;
    private boolean draft;
    private Integer draftStep;
    private String snapshotJson;
    private LocalDateTime snapshotAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository / Mapper 전용) */
    public CompanyAnalysisReport(Long id, Long userId, String stockCode, String stockName,
                                 ReportManual manual, InvestmentGrades grades, ValuationParams valuationParams,
                                 boolean draft, Integer draftStep,
                                 String snapshotJson, LocalDateTime snapshotAt,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.manual = manual;
        this.grades = grades;
        this.valuationParams = valuationParams;
        this.draft = draft;
        this.draftStep = draftStep;
        this.snapshotJson = snapshotJson;
        this.snapshotAt = snapshotAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CompanyAnalysisReport create(Long userId, String stockCode, String stockName,
                                               ReportManual manual, InvestmentGrades grades,
                                               ValuationParams valuationParams,
                                               boolean draft, Integer draftStep,
                                               String snapshotJson, LocalDateTime snapshotAt) {
        requireUserId(userId);
        requireStockCode(stockCode);
        ReportManual validManual = normalizeManual(manual);
        LocalDateTime now = LocalDateTime.now();
        return new CompanyAnalysisReport(null, userId, stockCode, stockName, validManual,
                orEmptyGrades(grades), requireParams(valuationParams),
                draft, normalizeDraftStep(draft, draftStep), snapshotJson, snapshotAt, now, now);
    }

    /**
     * 정성 입력/투자판단/파라미터/작성 상태 수정 (스냅샷 불변).
     * draft=false로 저장하면 작성 완료 상태가 된다.
     */
    public void updateManual(ReportManual manual, InvestmentGrades grades, ValuationParams valuationParams,
                             boolean draft, Integer draftStep) {
        this.manual = normalizeManual(manual);
        this.grades = orEmptyGrades(grades);
        this.valuationParams = requireParams(valuationParams);
        this.draft = draft;
        this.draftStep = normalizeDraftStep(draft, draftStep);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 스냅샷 새로고침 (자동 산출 데이터만 교체)
     */
    public void refreshSnapshot(String snapshotJson, LocalDateTime snapshotAt, String stockName) {
        this.snapshotJson = snapshotJson;
        this.snapshotAt = snapshotAt;
        if (stockName != null && !stockName.isBlank()) {
            this.stockName = stockName;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void assignId(Long id) {
        this.id = id;
    }

    // === 검증 ===

    private static void requireUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void requireStockCode(String stockCode) {
        if (stockCode == null || !stockCode.matches("\\d{6}")) {
            throw new IllegalArgumentException("종목코드는 6자리 숫자여야 합니다: " + stockCode);
        }
    }

    private static ValuationParams requireParams(ValuationParams params) {
        if (params == null) {
            throw new IllegalArgumentException("가치평가 파라미터는 필수입니다.");
        }
        return params;
    }

    private static InvestmentGrades orEmptyGrades(InvestmentGrades grades) {
        return grades != null ? grades : InvestmentGrades.empty();
    }

    /**
     * 완성 리포트는 단계 정보를 갖지 않는다. 작성중이면 2~7만 허용(없으면 null → 프론트가 기본 2단계).
     */
    private static Integer normalizeDraftStep(boolean draft, Integer draftStep) {
        if (!draft || draftStep == null) {
            return null;
        }
        if (draftStep < DRAFT_STEP_MIN || draftStep > DRAFT_STEP_MAX) {
            throw new IllegalArgumentException("작성 단계는 2~7 사이여야 합니다: " + draftStep);
        }
        return draftStep;
    }

    private static ReportManual normalizeManual(ReportManual manual) {
        ReportManual target = manual != null ? manual : ReportManual.empty();
        target.validate();
        return withCurrentSchema(target);
    }

    private static ReportManual withCurrentSchema(ReportManual m) {
        return new ReportManual(ReportManual.CURRENT_SCHEMA_VERSION,
                m.history(), m.philosophyNote(), m.customers(), m.suppliers(), m.partnerAssessment(),
                m.competitors(), m.performanceNote(), m.financialChanges(), m.shareholderEvents(),
                m.shareholderNote(), m.judgmentComment(),
                m.revenueForecasts(), m.amountUnit(), m.metricInputs());
    }

}
