package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioItemRepositoryImpl ACTIVE 필터")
class PortfolioItemRepositoryImplActiveFilterTest {

    @Mock
    PortfolioItemJpaRepository jpaRepository;

    @InjectMocks
    PortfolioItemRepositoryImpl repository;

    @Test
    @DisplayName("findByUserId는 ACTIVE 항목만 반환한다")
    void findByUserId_returnsOnlyActiveItems() {
        OtherItemEntity activeEntity = new OtherItemEntity(
                10L, 1L, "기타1", BigDecimal.valueOf(1000), false,
                "DOMESTIC", null, PortfolioItemStatus.ACTIVE, 0L,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(jpaRepository.findByUserIdAndStatus(1L, PortfolioItemStatus.ACTIVE))
                .willReturn(List.of(activeEntity));

        List<PortfolioItem> result = repository.findByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("existsByUserIdAndItemNameAndAssetType은 ACTIVE 항목만 검사한다")
    void exists_checksActiveOnly() {
        given(jpaRepository.existsByUserIdAndItemNameAndAssetTypeAndStatus(
                1L, "삼성전자", "STOCK", PortfolioItemStatus.ACTIVE))
                .willReturn(false);

        boolean exists = repository.existsByUserIdAndItemNameAndAssetType(
                1L, "삼성전자", AssetType.STOCK);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findById는 status 무관하게 단건 조회한다")
    void findById_isStatusAgnostic() {
        OtherItemEntity closed = new OtherItemEntity(
                12L, 1L, "X", BigDecimal.valueOf(0), false,
                "DOMESTIC", null, PortfolioItemStatus.CLOSED, 0L,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(jpaRepository.findById(12L)).willReturn(Optional.of(closed));

        Optional<PortfolioItem> result = repository.findById(12L);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(PortfolioItemStatus.CLOSED);
    }

    @Test
    @DisplayName("findByUserIdIn은 ACTIVE 항목만 반환한다")
    void findByUserIdIn_returnsOnlyActiveItems() {
        OtherItemEntity active = new OtherItemEntity(
                20L, 2L, "Y", BigDecimal.valueOf(500), false,
                "DOMESTIC", null, PortfolioItemStatus.ACTIVE, 0L,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(jpaRepository.findByUserIdInAndStatus(List.of(2L, 3L), PortfolioItemStatus.ACTIVE))
                .willReturn(List.of(active));

        List<PortfolioItem> result = repository.findByUserIdIn(List.of(2L, 3L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByNewsEnabled는 ACTIVE 항목만 반환한다")
    void findByNewsEnabled_returnsOnlyActiveItems() {
        OtherItemEntity active = new OtherItemEntity(
                30L, 3L, "뉴스ON", BigDecimal.valueOf(100), true,
                "DOMESTIC", null, PortfolioItemStatus.ACTIVE, 0L,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(jpaRepository.findByNewsEnabledAndStatus(true, PortfolioItemStatus.ACTIVE))
                .willReturn(List.of(active));

        List<PortfolioItem> result = repository.findByNewsEnabled(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isNewsEnabled()).isTrue();
        assertThat(result.get(0).getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
    }

    @Test
    @DisplayName("findByUserIdAndItemNameAndNewsEnabled는 ACTIVE 항목만 반환한다")
    void findByUserIdAndItemNameAndNewsEnabled_returnsOnlyActiveItems() {
        OtherItemEntity active = new OtherItemEntity(
                40L, 4L, "삼성전자", BigDecimal.valueOf(1000), true,
                "DOMESTIC", null, PortfolioItemStatus.ACTIVE, 0L,
                LocalDateTime.now(), LocalDateTime.now()
        );
        given(jpaRepository.findByUserIdAndItemNameAndNewsEnabledAndStatus(
                4L, "삼성전자", true, PortfolioItemStatus.ACTIVE))
                .willReturn(List.of(active));

        List<PortfolioItem> result = repository.findByUserIdAndItemNameAndNewsEnabled(4L, "삼성전자", true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItemName()).isEqualTo("삼성전자");
        assertThat(result.get(0).getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
    }

    /**
     * 가드: 의도치 않은 호출이 있을 때 InjectMocks 검증 보조
     */
    @SuppressWarnings("unused")
    private static void avoidUnusedMatchers() {
        any();
    }
}