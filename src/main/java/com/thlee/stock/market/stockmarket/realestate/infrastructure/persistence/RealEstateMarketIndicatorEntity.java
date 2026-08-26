package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 부동산 시장 지표 히스토리 JPA Entity.
 */
@Entity
@Table(
        name = "real_estate_market_indicator",
        indexes = {
                @Index(name = "idx_re_indicator_region_cat_snapshot",
                        columnList = "region_code, category, snapshot_date DESC"),
                @Index(name = "idx_re_indicator_source_snapshot",
                        columnList = "source, snapshot_date DESC")
        }
)
@Getter
public class RealEstateMarketIndicatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "region_code", nullable = false, length = 8)
    private String regionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private RealEstateMarketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 32)
    private RealEstateMarketSource source;

    @Column(name = "indicator_code", nullable = false, length = 128)
    private String indicatorCode;

    @Column(name = "reference_text", length = 64)
    private String referenceText;

    @Column(name = "value", precision = 18, scale = 4)
    private BigDecimal value;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private String payload;

    @Column(name = "source_url", length = 255)
    private String sourceUrl;

    @Column(name = "cycle", length = 16)
    private String cycle;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDateTime snapshotDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RealEstateMarketIndicatorEntity() {
    }

    public RealEstateMarketIndicatorEntity(Long id,
                                           String regionCode,
                                           RealEstateMarketCategory category,
                                           RealEstateMarketSource source,
                                           String indicatorCode,
                                           String referenceText,
                                           BigDecimal value,
                                           String payload,
                                           String sourceUrl,
                                           String cycle,
                                           LocalDateTime snapshotDate,
                                           LocalDateTime createdAt) {
        this.id = id;
        this.regionCode = regionCode;
        this.category = category;
        this.source = source;
        this.indicatorCode = indicatorCode;
        this.referenceText = referenceText;
        this.value = value;
        this.payload = payload;
        this.sourceUrl = sourceUrl;
        this.cycle = cycle;
        this.snapshotDate = snapshotDate;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
