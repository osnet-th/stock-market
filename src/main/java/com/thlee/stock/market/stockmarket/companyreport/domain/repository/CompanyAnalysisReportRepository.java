package com.thlee.stock.market.stockmarket.companyreport.domain.repository;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.CompanyAnalysisReport;

import java.util.List;
import java.util.Optional;

/**
 * 기업분석리포트 저장소 포트. 모든 조회/삭제는 userId 스코프를 강제한다 (IDOR 방지).
 */
public interface CompanyAnalysisReportRepository {

    CompanyAnalysisReport save(CompanyAnalysisReport report);

    Optional<CompanyAnalysisReport> findByIdAndUserId(Long id, Long userId);

    /**
     * 내 리포트 목록 (updatedAt 내림차순). stockNameKeyword가 null이면 전체, 아니면 종목명 부분일치.
     */
    List<CompanyAnalysisReport> findPage(Long userId, String stockNameKeyword, int page, int size);

    long count(Long userId, String stockNameKeyword);

    boolean deleteByIdAndUserId(Long id, Long userId);
}
