package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import jakarta.persistence.*;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

/**
 * 부동산 시장 지표 메타데이터 JPA Entity.
 * PK: (category, source, indicatorCode)
 */
@Entity
@Table(name = "real_estate_market_metadata")
@IdClass(RealEstateMarketMetadataEntity.MetadataId.class)
@Getter
public class RealEstateMarketMetadataEntity {

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

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "default_visible", nullable = false)
    private boolean defaultVisible;

    protected RealEstateMarketMetadataEntity() {
    }

    public RealEstateMarketMetadataEntity(RealEstateMarketCategory category,
                                          RealEstateMarketSource source,
                                          String indicatorCode,
                                          String displayName,
                                          String description,
                                          String unit,
                                          boolean defaultVisible) {
        this.category = category;
        this.source = source;
        this.indicatorCode = indicatorCode;
        this.displayName = displayName;
        this.description = description;
        this.unit = unit;
        this.defaultVisible = defaultVisible;
    }

    public static class MetadataId implements Serializable {
        private RealEstateMarketCategory category;
        private RealEstateMarketSource source;
        private String indicatorCode;

        public MetadataId() {
        }

        public MetadataId(RealEstateMarketCategory category,
                          RealEstateMarketSource source,
                          String indicatorCode) {
            this.category = category;
            this.source = source;
            this.indicatorCode = indicatorCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MetadataId that = (MetadataId) o;
            return category == that.category
                    && source == that.source
                    && Objects.equals(indicatorCode, that.indicatorCode);
        }

        @Override
        public int hashCode() {
            return Objects.hash(category, source, indicatorCode);
        }
    }
}
