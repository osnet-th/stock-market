package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;

import java.util.List;
import java.util.Optional;

/**
 * 포트폴리오 항목 Repository 인터페이스
 * 도메인 계층에 정의하여 인프라 계층이 구현
 *
 * <p>기본 조회 메서드는 ACTIVE 상태(보유 중)인 항목만 반환한다.
 * 단건 {@link #findById(Long)}는 status 무관하게 반환한다(매도 이력 사후 수정 등 CLOSED 접근 필요).</p>
 */
public interface PortfolioItemRepository {

    /**
     * 포트폴리오 항목 저장 (타입별 자식 포함)
     */
    PortfolioItem save(PortfolioItem item);

    /**
     * ID로 단건 조회 (상세 필드 포함, status 무관)
     */
    Optional<PortfolioItem> findById(Long id);

    /**
     * 사용자별 ACTIVE 항목 조회
     */
    List<PortfolioItem> findByUserId(Long userId);

    /**
     * 뉴스 활성화된 ACTIVE 항목 조회
     */
    List<PortfolioItem> findByNewsEnabled(boolean newsEnabled);

    /**
     * 사용자별 종목명 + 뉴스 활성화 상태 + ACTIVE 조회
     */
    List<PortfolioItem> findByUserIdAndItemNameAndNewsEnabled(Long userId, String itemName, boolean newsEnabled);

    /**
     * ACTIVE 항목 중복 확인 (재매수는 새 항목으로 등록 가능하도록 CLOSED는 제외)
     */
    boolean existsByUserIdAndItemNameAndAssetType(Long userId, String itemName, AssetType assetType);

    /**
     * 여러 사용자의 ACTIVE 포트폴리오 항목 일괄 조회
     */
    List<PortfolioItem> findByUserIdIn(List<Long> userIds);

    /**
     * 포트폴리오 항목 삭제
     */
    void delete(PortfolioItem item);
}