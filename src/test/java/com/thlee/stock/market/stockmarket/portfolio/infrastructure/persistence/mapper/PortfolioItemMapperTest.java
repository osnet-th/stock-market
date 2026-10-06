package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.BondDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.FundDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PensionDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.RealEstateDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.BondSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.DepositMode;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.FundSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PensionSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.RealEstateSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
import com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence.CashItemEntity;
import com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence.OtherItemEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PortfolioItemMapper — 금융기관·납입 처리 방식 저장·조회 변환")
class PortfolioItemMapperTest {

    private static final Long USER_ID = 1L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(1_000_000);

    @ParameterizedTest
    @EnumSource(AssetType.class)
    @DisplayName("M1 모든 자산군에서 도메인 → 엔티티 → 도메인 변환 후에도 금융기관이 유지된다")
    void keepsInstitutionThroughRoundTrip(AssetType assetType) {
        PortfolioItem item = itemOf(assetType);
        item.updateInstitution("키움증권");

        PortfolioItem restored = PortfolioItemMapper.toDomain(PortfolioItemMapper.toEntity(item));

        assertThat(restored.getAssetType()).isEqualTo(assetType);
        assertThat(restored.getInstitution()).isEqualTo("키움증권");
    }

    @Test
    @DisplayName("M2 금융기관이 없는 기존 행은 미지정(null)으로 읽는다")
    void readsLegacyRowAsUnassigned() {
        LocalDateTime now = LocalDateTime.now();
        OtherItemEntity legacy = new OtherItemEntity(
                10L, USER_ID, "기타", AMOUNT, false, "DOMESTIC", null, null,
                PortfolioItemStatus.ACTIVE, 0L, now, now);

        PortfolioItem item = PortfolioItemMapper.toDomain(legacy);

        assertThat(item.getInstitution()).isNull();
    }

    @Test
    @DisplayName("M3 납입 처리 방식(자동 반영)이 엔티티를 거쳐 그대로 돌아온다")
    void keepsDepositModeThroughRoundTrip() {
        PortfolioItem item = PortfolioItem.createWithCash(USER_ID, "적금", AMOUNT, Region.DOMESTIC,
                new CashDetail(CashSubType.SAVINGS, BigDecimal.valueOf(3), null, null, TaxType.GENERAL,
                        BigDecimal.valueOf(300_000), 25, DepositMode.AUTO));

        CashItemEntity entity = (CashItemEntity) PortfolioItemMapper.toEntity(item);
        PortfolioItem restored = PortfolioItemMapper.toDomain(entity);

        assertThat(entity.getDepositMode()).isEqualTo("AUTO");
        assertThat(restored.getCashDetail().getDepositMode()).isEqualTo(DepositMode.AUTO);
    }

    @Test
    @DisplayName("M4 처리 방식 컬럼이 비어 있는 기존 행은 알림 확인으로 읽는다")
    void readsLegacyCashRowAsNotify() {
        LocalDateTime now = LocalDateTime.now();
        CashItemEntity legacy = new CashItemEntity(
                11L, USER_ID, "적금", AMOUNT, false, "DOMESTIC", null, null,
                PortfolioItemStatus.ACTIVE, 0L, now, now,
                "SAVINGS", BigDecimal.valueOf(3), null, null, "GENERAL",
                BigDecimal.valueOf(300_000), 25, null);

        PortfolioItem item = PortfolioItemMapper.toDomain(legacy);

        assertThat(item.getCashDetail().getDepositMode()).isEqualTo(DepositMode.NOTIFY);
    }

    private static PortfolioItem itemOf(AssetType assetType) {
        return switch (assetType) {
            case STOCK -> PortfolioItem.createWithStock(USER_ID, "삼성전자", Region.DOMESTIC,
                    new StockDetail(StockSubType.INDIVIDUAL, "005930", "KOSPI", "KRX", "KR",
                            10, BigDecimal.valueOf(70_000), null, PriceCurrency.KRW, null));
            case BOND -> PortfolioItem.createWithBond(USER_ID, "국고채", AMOUNT, Region.DOMESTIC,
                    new BondDetail(BondSubType.GOVERNMENT, LocalDate.of(2030, 1, 1), BigDecimal.valueOf(3), "AAA"));
            case REAL_ESTATE -> PortfolioItem.createWithRealEstate(USER_ID, "아파트", AMOUNT, Region.DOMESTIC,
                    new RealEstateDetail(RealEstateSubType.APARTMENT, "서울", BigDecimal.valueOf(84)));
            case FUND -> PortfolioItem.createWithFund(USER_ID, "주식형 펀드", AMOUNT, Region.DOMESTIC,
                    new FundDetail(FundSubType.EQUITY_FUND, BigDecimal.ONE));
            case CASH -> PortfolioItem.createWithCash(USER_ID, "정기예금", AMOUNT, Region.DOMESTIC,
                    new CashDetail(CashSubType.DEPOSIT, BigDecimal.valueOf(3), LocalDate.of(2026, 1, 1),
                            LocalDate.of(2027, 1, 1), TaxType.GENERAL));
            case PENSION -> PortfolioItem.createWithPension(USER_ID, "IRP", AMOUNT, Region.DOMESTIC,
                    new PensionDetail(PensionSubType.IRP, "미래에셋", null, null, null));
            case CRYPTO, GOLD, COMMODITY, OTHER ->
                    PortfolioItem.create(USER_ID, assetType.name(), assetType, AMOUNT, Region.DOMESTIC);
        };
    }
}
