package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.domain.exception.CompanyReportNotFoundException;
import com.thlee.stock.market.stockmarket.companyreport.domain.repository.CompanyAnalysisReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

/** 외부 조회가 끝난 뒤 재무 컬럼만 짧은 트랜잭션으로 갱신한다. */
@Service
@RequiredArgsConstructor
public class CompanyReportSnapshotPersistenceService {
    private final CompanyAnalysisReportRepository repository;

    /** stockName은 호출자가 도메인 규칙으로 이미 확정한 값이다 (빈 값 대체는 여기서 하지 않는다). */
    @Transactional
    public void replace(Long id, Long userId, String snapshotJson, String stockName) {
        if (!repository.updateSnapshot(id, userId, snapshotJson, LocalDateTime.now(), stockName))
            throw new CompanyReportNotFoundException(id);
    }
}
