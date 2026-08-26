package com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyAnalysisReportJpaRepository extends JpaRepository<CompanyAnalysisReportEntity, Long> {

    Optional<CompanyAnalysisReportEntity> findByIdAndUserId(Long id, Long userId);

    Page<CompanyAnalysisReportEntity> findByUserIdOrderByUpdatedAtDesc(Long userId, Pageable pageable);

    Page<CompanyAnalysisReportEntity> findByUserIdAndStockNameContainingIgnoreCaseOrderByUpdatedAtDesc(
            Long userId, String stockName, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndStockNameContainingIgnoreCase(Long userId, String stockName);

    long deleteByIdAndUserId(Long id, Long userId);
}
