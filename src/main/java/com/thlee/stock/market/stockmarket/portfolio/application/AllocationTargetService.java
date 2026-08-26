package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationTargetResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.AllocationTarget;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.AllocationTargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * 목표 자산 배분 설정 조회/저장 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllocationTargetService {

    private final AllocationTargetRepository allocationTargetRepository;

    /**
     * 목표 배분 설정 조회 (미설정 시 empty)
     */
    public Optional<AllocationTargetResponse> getTarget(Long userId) {
        return allocationTargetRepository.findByUserId(userId)
                .map(AllocationTargetResponse::from);
    }

    /**
     * 목표 배분 설정 업서트
     */
    @Transactional
    public AllocationTargetResponse saveTarget(Long userId,
                                               BigDecimal safeRatio,
                                               BigDecimal investRatio,
                                               BigDecimal bandPctPoint,
                                               Map<AssetType, BigDecimal> investAssetRatios) {
        AllocationTarget target = allocationTargetRepository.findByUserId(userId)
                .map(existing -> {
                    existing.update(safeRatio, investRatio, bandPctPoint, investAssetRatios);
                    return existing;
                })
                .orElseGet(() -> AllocationTarget.create(
                        userId, safeRatio, investRatio, bandPctPoint, investAssetRatios));

        AllocationTarget saved = allocationTargetRepository.save(target);
        return AllocationTargetResponse.from(saved);
    }
}
