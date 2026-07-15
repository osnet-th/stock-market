package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.CompanyReportCommands;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.domain.exception.CompanyReportNotFoundException;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.CompanyAnalysisReport;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;
import com.thlee.stock.market.stockmarket.companyreport.domain.repository.CompanyAnalysisReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 리포트 쓰기 유스케이스.
 * 스냅샷 조립은 외부 API(DART/KIS) 호출이므로 트랜잭션 밖에서 수행한다 (save는 단건 연산).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyReportWriteService {

    private final CompanyAnalysisReportRepository repository;
    private final CompanyReportSnapshotService snapshotService;
    private final ReportSnapshotJsonMapper snapshotJsonMapper;

    /**
     * 생성. 스냅샷 조립 실패 시에도 리포트는 저장하고(수동 입력 보존) 이후 새로고침으로 보완한다.
     */
    public Long create(CompanyReportCommands.Create command) {
        validateParams(command.params());
        ReportSnapshot snapshot = assembleSafely(command.stockCode());
        CompanyAnalysisReport report = CompanyAnalysisReport.create(
                command.userId(), command.stockCode(), stockNameOf(snapshot, command.stockCode()),
                command.manual(), command.grades(), command.params(),
                command.draft(), command.draftStep(),
                toJsonOrNull(snapshot), snapshot != null ? LocalDateTime.now() : null);
        return repository.save(report).getId();
    }

    @Transactional
    public void update(CompanyReportCommands.Update command) {
        validateParams(command.params());
        CompanyAnalysisReport report = repository.findByIdAndUserId(command.id(), command.userId())
                .orElseThrow(() -> new CompanyReportNotFoundException(command.id()));
        report.updateManual(command.manual(), command.grades(), command.params(),
                command.draft(), command.draftStep());
        repository.save(report);
    }

    /**
     * 스냅샷 새로고침. 명시적 데이터 갱신 요청이므로 조립 실패는 그대로 전파한다.
     */
    public void refreshSnapshot(Long id, Long userId) {
        CompanyAnalysisReport report = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CompanyReportNotFoundException(id));
        ReportSnapshot snapshot = snapshotService.assemble(report.getStockCode());
        report.refreshSnapshot(snapshotJsonMapper.toJson(snapshot), LocalDateTime.now(), snapshot.stockName());
        repository.save(report);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        if (!repository.deleteByIdAndUserId(id, userId)) {
            throw new CompanyReportNotFoundException(id);
        }
    }

    // === 내부 헬퍼 ===

    private ReportSnapshot assembleSafely(String stockCode) {
        try {
            return snapshotService.assemble(stockCode);
        } catch (Exception e) {
            log.warn("리포트 생성 시 스냅샷 조립 실패 (리포트는 저장): stockCode={}, cause={}", stockCode, e.getMessage());
            return null;
        }
    }

    private String stockNameOf(ReportSnapshot snapshot, String stockCode) {
        return snapshot != null ? snapshot.stockName() : stockCode;
    }

    private String toJsonOrNull(ReportSnapshot snapshot) {
        return snapshot != null ? snapshotJsonMapper.toJson(snapshot) : null;
    }

    private void validateParams(ValuationParams params) {
        if (params == null || params.adjustmentRatios() == null) {
            throw new IllegalArgumentException("가치평가 파라미터는 필수입니다.");
        }
        requireRange(params.discountRate(), "0.01", "1", "할인율");
        requireRange(params.optimisticGrowthRate(), "0", "1", "낙관 성장률");
        requireYears(params.optimisticGrowthYears());
        requireRatios(params.adjustmentRatios());
    }

    private void requireRange(BigDecimal value, String min, String max, String label) {
        boolean valid = value != null
                && value.compareTo(new BigDecimal(min)) >= 0
                && value.compareTo(new BigDecimal(max)) <= 0;
        if (!valid) {
            throw new IllegalArgumentException(label + "은(는) " + min + "~" + max + " 사이여야 합니다: " + value);
        }
    }

    private void requireYears(int years) {
        if (years < 1 || years > 10) {
            throw new IllegalArgumentException("낙관 성장 연수는 1~10 사이여야 합니다: " + years);
        }
    }

    private void requireRatios(ValuationParams.AdjustmentRatios ratios) {
        requireRange(ratios.cash(), "0", "1", "현금 조정비율");
        requireRange(ratios.securities(), "0", "1", "유가증권 조정비율");
        requireRange(ratios.receivables(), "0", "1", "매출채권 조정비율");
        requireRange(ratios.inventory(), "0", "1", "재고자산 조정비율");
        requireRange(ratios.investments(), "0", "1", "투자자산 조정비율");
        requireRange(ratios.tangible(), "0", "1", "유형자산 조정비율");
        requireRange(ratios.intangible(), "0", "1", "무형자산 조정비율");
        requireRange(ratios.otherCurrent(), "0", "1", "기타유동자산 조정비율");
    }
}
