package com.thlee.stock.market.stockmarket.economics.derivedindicator.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 사용자 커스텀 파생지표 JPA Entity.
 * <p>
 * 수식은 jsonb String payload로 저장(RealEstateMarketLatestEntity 선례, Hibernate 6 네이티브).
 * DerivedFormula ↔ JSON 직렬화는 DerivedFormulaJsonConverter가 담당, 본 Entity는 String만 보유.
 * 연관관계 금지(ARCHITECTURE §5) — userId는 Long 컬럼.
 * category는 교차 카테고리(프리셋 복제)인 경우 null(혼합).
 */
@Entity
@Table(
        name = "user_derived_indicator",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "name"})
        },
        indexes = {
                @Index(name = "idx_user_derived_indicator_user", columnList = "user_id")
        }
)
@Getter
public class UserDerivedIndicatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "description", length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "formula", columnDefinition = "jsonb", nullable = false)
    private String formulaJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 40)
    private EcosIndicatorCategory category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserDerivedIndicatorEntity() {
    }

    public UserDerivedIndicatorEntity(Long id,
                                      Long userId,
                                      String name,
                                      String unit,
                                      String description,
                                      String formulaJson,
                                      EcosIndicatorCategory category,
                                      LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.unit = unit;
        this.description = description;
        this.formulaJson = formulaJson;
        this.category = category;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
