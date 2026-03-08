package com.thlee.stock.market.stockmarket.portfolio.application;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.AllocationResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.PortfolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 포트폴리오 자산 비중 계산 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioAllocationService {

    private final PortfolioItemRepository portfolioItemRepository;

    /**
     * AssetType별 투자 비중(%) 계산
     */
    public List<AllocationResponse> getAllocation(Long userId) {
        List<PortfolioItem> items = portfolioItemRepository.findByUserId(userId);

        BigDecimal totalAmount = items.stream()
                .map(PortfolioItem::getInvestedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return List.of();
        }

        Map<AssetType, BigDecimal> amountByType = items.stream()
                .collect(Collectors.groupingBy(
                        PortfolioItem::getAssetType,
                        Collectors.reducing(BigDecimal.ZERO, PortfolioItem::getInvestedAmount, BigDecimal::add)
                ));

        BigDecimal hundred = new BigDecimal("100");

        return amountByType.entrySet().stream()
                .map(entry -> new AllocationResponse(
                        entry.getKey().name(),
                        entry.getKey().getDescription(),
                        entry.getValue(),
                        entry.getValue().multiply(hundred).divide(totalAmount, 2, RoundingMode.HALF_UP)
                ))
                .collect(Collectors.toList());
    }
}