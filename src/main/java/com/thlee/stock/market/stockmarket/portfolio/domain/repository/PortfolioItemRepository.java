package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;

import java.util.List;
import java.util.Optional;

/**
 * 포트폴리오 항목 Repository 인터페이스
 * 도메인 계층에 정의하여 인프라 계층이 구현
 */
public interface PortfolioItemRepository {

    /**
     * 포트폴리오 항목 저장 (타입별 자식 포함)
     */
    PortfolioItem save(PortfolioItem item);

    /**
     * ID로 단건 조회 (상세 필드 포함)
     */
    Optional<PortfolioItem> findById(Long id);

    /**
     * 사용자별 전체 조회 (상세 필드 포함)
     */
    List<PortfolioItem> findByUserId(Long userId);

    /**
     * 뉴스 활성화 항목 전체 조회
     */
    List<PortfolioItem> findByNewsEnabled(boolean newsEnabled);

    /**
     * 사용자별 종목명 + 뉴스 활성화 상태로 조회
     */
    List<PortfolioItem> findByUserIdAndItemNameAndNewsEnabled(Long userId, String itemName, boolean newsEnabled);

    /**
     * 중복 확인
     */
    boolean existsByUserIdAndItemNameAndAssetType(Long userId, String itemName, AssetType assetType);

    /**
     * 여러 사용자의 포트폴리오 항목 일괄 조회
     */
    List<PortfolioItem> findByUserIdIn(List<Long> userIds);

    /**
     * 포트폴리오 항목 삭제
     */
    void delete(PortfolioItem item);
}
