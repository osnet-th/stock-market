package com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteDisplayMode;
import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicatorSourceType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자 관심 지표 JPA Entity
 */
@Entity
@Table(
        name = "user_favorite_indicator",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "source_type", "indicator_code"})
        },
        indexes = {
                @Index(name = "idx_user_favorite_user", columnList = "user_id")
        }
)
@Getter
public class UserFavoriteIndicatorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private FavoriteIndicatorSourceType sourceType;

    @Column(name = "indicator_code", nullable = false, length = 310)
    private String indicatorCode;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "display_mode",
            nullable = false,
            length = 10,
            columnDefinition = "VARCHAR(10) NOT NULL DEFAULT 'INDICATOR'"
    )
    private FavoriteDisplayMode displayMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserFavoriteIndicatorEntity() {
    }

    public UserFavoriteIndicatorEntity(Long id,
                                       Long userId,
                                       FavoriteIndicatorSourceType sourceType,
                                       String indicatorCode,
                                       FavoriteDisplayMode displayMode,
                                       LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.sourceType = sourceType;
        this.indicatorCode = indicatorCode;
        this.displayMode = displayMode;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (displayMode == null) {
            displayMode = FavoriteDisplayMode.INDICATOR;
        }
    }
}
