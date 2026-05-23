package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 부동산 시장 지표 최신값 JPA Entity (변경 감지 비교용).
 * <p>
 * Unique key: (regionCode, category, source, indicatorCode) — source 포함 필수.
 * 같은 region×category에 복수 출처(예: T2 매매가격 = MOLIT 실거래 + REB 가격지수)가
 * 들어오는 경우 source가 빠지면 덮어써짐.
 */
@Entity
@Table(name = "real_estate_market_latest")
@IdClass(RealEstateMarketLatestEntity.LatestId.class)
@Getter
public class RealEstateMarketLatestEntity {

    @Id
    @Column(name = "region_code", nullable = false, length = 8)
    private String regionCode;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private RealEstateMarketCategory category;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private RealEstateMarketSource source;

    @Id
    @Column(name = "indicator_code", nullable = false, length = 128)
    private String indicatorCode;

    @Column(name = "reference_text", length = 64)
    private String referenceText;

    @Column(name = "previous_reference_text", length = 64)
    private String previousReferenceText;

    @Column(name = "value", precision = 18, scale = 4)
    private BigDecimal value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RealEstateMarketLatestEntity() {
    }

    public RealEstateMarketLatestEntity(String regionCode,
                                        RealEstateMarketCategory category,
                                        RealEstateMarketSource source,
                                        String indicatorCode,
                                        String referenceText,
                                        String previousReferenceText,
                                        BigDecimal value,
                                        String payload,
                                        LocalDateTime updatedAt) {
        this.regionCode = regionCode;
        this.category = category;
        this.source = source;
        this.indicatorCode = indicatorCode;
        this.referenceText = referenceText;
        this.previousReferenceText = previousReferenceText;
        this.value = value;
        this.payload = payload;
        this.updatedAt = updatedAt;
    }

    public static class LatestId implements Serializable {
        private String regionCode;
        private RealEstateMarketCategory category;
        private RealEstateMarketSource source;
        private String indicatorCode;

        public LatestId() {
        }

        public LatestId(String regionCode,
                        RealEstateMarketCategory category,
                        RealEstateMarketSource source,
                        String indicatorCode) {
            this.regionCode = regionCode;
            this.category = category;
            this.source = source;
            this.indicatorCode = indicatorCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LatestId that = (LatestId) o;
            return Objects.equals(regionCode, that.regionCode)
                    && category == that.category
                    && source == that.source
                    && Objects.equals(indicatorCode, that.indicatorCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(regionCode, category, source, indicatorCode);
        }
    }
}
