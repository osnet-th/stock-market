package com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.CompanyAnalysisReport;
import com.thlee.stock.market.stockmarket.companyreport.domain.repository.CompanyAnalysisReportRepository;
import com.thlee.stock.market.stockmarket.companyreport.infrastructure.persistence.mapper.CompanyAnalysisReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CompanyAnalysisReportRepositoryImpl implements CompanyAnalysisReportRepository {

    private final CompanyAnalysisReportJpaRepository jpaRepository;
    private final CompanyAnalysisReportMapper mapper;

    @Override
    public CompanyAnalysisReport save(CompanyAnalysisReport report) {
        CompanyAnalysisReportEntity saved = jpaRepository.save(mapper.toEntity(report));
        if (report.getId() == null) {
            report.assignId(saved.getId());
        }
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<CompanyAnalysisReport> findByIdAndUserId(Long id, Long userId) {
        return jpaRepository.findByIdAndUserId(id, userId).map(mapper::toDomain);
    }

    @Override
    public List<CompanyAnalysisReport> findPage(Long userId, String stockNameKeyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CompanyAnalysisReportEntity> result = stockNameKeyword == null
                ? jpaRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                : jpaRepository.findByUserIdAndStockNameContainingIgnoreCaseOrderByUpdatedAtDesc(
                        userId, stockNameKeyword, pageable);
        return result.getContent().stream().map(mapper::toDomain).toList();
    }

    @Override
    public long count(Long userId, String stockNameKeyword) {
        return stockNameKeyword == null
                ? jpaRepository.countByUserId(userId)
                : jpaRepository.countByUserIdAndStockNameContainingIgnoreCase(userId, stockNameKeyword);
    }

    @Override
    public boolean deleteByIdAndUserId(Long id, Long userId) {
        return jpaRepository.deleteByIdAndUserId(id, userId) > 0;
    }
}
